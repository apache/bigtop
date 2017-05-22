# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from charmhelpers.core import hookenv
from charms.layer.apache_bigtop_base import get_layer_opts, get_package_version
from charms.layer.bigtop_hbase import HBase
from charms.reactive import (
    RelationBase,
    is_state,
    remove_state,
    set_state,
    when,
    when_any,
    when_not,
    when_not_all
)
from charms.reactive.helpers import any_file_changed, data_changed


@when('bigtop.available')
def report_status():
    hadoop_joined = is_state('hadoop.joined')
    hdfs_ready = is_state('hadoop.hdfs.ready')
    zk_joined = is_state('zookeeper.joined')
    zk_ready = is_state('zookeeper.ready')
    hbase_installed = is_state('hbase.installed')
    if not hadoop_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to hadoop plugin')
    elif not hdfs_ready:
        hookenv.status_set('waiting',
                           'waiting for hdfs to become ready')
    elif not zk_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to zookeeper')
    elif not zk_ready:
        hookenv.status_set('waiting',
                           'waiting for zookeeper to become ready')
    elif not hbase_installed:
        hookenv.status_set('waiting',
                           'waiting to install hbase')
    else:
        hookenv.status_set('active',
                           'ready')


@when('bigtop.available', 'hadoop.hdfs.ready', 'zookeeper.ready')
def install_hbase(hdfs, zk):
    '''
    Anytime our dependencies are available, check to see if we have a valid
    reason to (re)install. These include:
    - initial install
    - config change
    - Zookeeper unit has joined/departed
    '''
    zks = zk.zookeepers()
    deployment_matrix = {
        'zookeepers': zks,
    }

    # Handle nuances when installing versus re-installing
    if not is_state('hbase.installed'):
        prefix = "installing"

        # On initial install, prime our kv with the current deployment matrix.
        # Subsequent calls will use this to determine if a reinstall is needed.
        data_changed('deployment_matrix', deployment_matrix)
    else:
        prefix = "configuring"

        # We do not need to reinstall when peers come and go; that is covered
        # by other handlers below.
        if is_state('hbpeer.departed') or is_state('hbpeer.joined'):
            return

        # Return if neither config nor our matrix has changed
        if not (is_state('config.changed') or
                data_changed('deployment_matrix', deployment_matrix)):
            return

    hookenv.status_set('maintenance', '{} hbase'.format(prefix))
    hookenv.log("{} hbase with: {}".format(prefix, deployment_matrix))
    hbase = HBase()
    hosts = {}
    hosts['namenode'] = hdfs.namenodes()[0]
    hbase.configure(hosts, zks)

    # Ensure our IP is in the regionservers list; restart if the rs conf
    # file has changed.
    hbase.update_regionservers([hookenv.unit_private_ip()])
    if any_file_changed(['/etc/hbase/conf/regionservers']):
        hbase.restart()

    # set app version string for juju status output
    hbase_version = get_package_version('hbase-master') or 'unknown'
    hookenv.application_version_set(hbase_version)

    hbase.open_ports()
    report_status()
    set_state('hbase.installed')


@when('hbase.installed')
@when_not_all('hadoop.hdfs.ready', 'zookeeper.ready')
def stop_hbase():
    '''
    HBase depends on HDFS and Zookeeper. If we are installed and either of
    these dependencies go away, shut down HBase services and remove our
    installed state.
    '''
    hbase = HBase()
    hbase.close_ports()
    hbase.stop()
    remove_state('hbase.installed')
    report_status()


@when('hbase.installed')
@when_any('hbpeer.departed', 'hbpeer.joined')
def handle_peers():
    '''
    We use HBase peers to keep track of the RegionServer IP addresses in a
    cluster. Use get_nodes() from the appropriate peer relation to retrieve
    a list of peer tuples, e.g.:
        [('hbase/0', '172.31.5.161'), ('hbase/2', '172.31.5.11')]

    Depending on the state, this handler will add or remove peer IP addresses
    from the regionservers config file.
    '''
    if is_state('hbpeer.departed'):
        hbpeer = RelationBase.from_state('hbpeer.departed')
        is_departing = True
        message = 'removing hbase peer(s)'
    else:
        hbpeer = RelationBase.from_state('hbpeer.joined')
        is_departing = False
        message = 'adding hbase peer(s)'

    # Make sure we have a valid relation object
    if hbpeer:
        nodes = hbpeer.get_nodes()
    else:
        hookenv.log('Ignoring unknown HBase peer state')
        return

    hookenv.status_set('maintenance', message)
    hbase = HBase()
    ip_addrs = [node[1] for node in nodes]
    hookenv.log('{}: {}'.format(message, ip_addrs))
    hbase.update_regionservers(ip_addrs, remove=is_departing)

    # NB: the rs conf file will always change when handling peer updates, but
    # we still include this condition to keep the files_changed kv current.
    if any_file_changed(['/etc/hbase/conf/regionservers']):
        hbase.restart()

    # Dismiss appropriate state now that we've handled the peer
    if is_departing:
        hbpeer.dismiss_departed()
    else:
        hbpeer.dismiss_joined()
    report_status()


@when('hbase.installed', 'leadership.is_leader')
@when('zookeeper.ready', 'hbclient.joined')
def serve_client(zk, client):
    '''
    We may have multiple HBase peers, but we only need to send 1 set of
    connection data. Leverage Juju leadership to only send the leader
    info (even if it's not the actual HBase master).

    Zookeeper will ensure that any HBase peer routes requests to the
    appropriate master.
    '''
    hbase = HBase()

    # Get hbase config and zk info
    config = get_layer_opts()
    host = hookenv.unit_private_ip()
    master_port = config.port('hbase-master')
    regionserver_port = config.port('hbase-region')
    thrift_port = config.port('hbase-thrift')
    zk_connect = hbase.get_zk_connect(zk.zookeepers())

    # Send data to our connected client
    client.send_connection(master_port=master_port,
                           regionserver_port=regionserver_port,
                           thrift_port=thrift_port,
                           host=host,
                           zk_connect=zk_connect)

    hookenv.log('Serving HBase client with master {}:{}, regionserver '
                'port {}, thrift port {}, and zk connect {}'.format(
                    host, master_port,
                    regionserver_port,
                    thrift_port,
                    zk_connect))


@when('leadership.is_leader', 'hbclient.joined')
@when_not('hbase.installed')
def stop_serving_client(client):
    '''
    If HDFS or ZK goes away, the 'installed' state will be removed. If we have
    connected clients, inform them that hbase is no longer ready.
    '''
    client.clear_hbase_started()
