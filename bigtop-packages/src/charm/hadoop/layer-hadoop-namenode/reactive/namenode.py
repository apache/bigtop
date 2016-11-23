
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

from charms.reactive import is_state, remove_state, set_state, when, when_not
from charms.layer.apache_bigtop_base import (
    Bigtop, get_hadoop_version, get_layer_opts, get_fqdn
)
from charmhelpers.core import hookenv, host
from jujubigdata import utils


###############################################################################
# Utility methods
###############################################################################
def send_early_install_info(remote):
    """Send clients/slaves enough relation data to start their install.

    If slaves or clients join before the namenode is installed, we can still provide enough
    info to start their installation. This will help parallelize installation among our
    cluster.

    Note that slaves can safely install early, but should not start until the
    'namenode.ready' state is set by the dfs-slave interface.
    """
    fqdn = get_fqdn()
    hdfs_port = get_layer_opts().port('namenode')
    webhdfs_port = get_layer_opts().port('nn_webapp_http')

    remote.send_namenodes([fqdn])
    remote.send_ports(hdfs_port, webhdfs_port)


###############################################################################
# Core methods
###############################################################################
@when('bigtop.available')
@when_not('apache-bigtop-namenode.installed')
def install_namenode():
    hookenv.status_set('maintenance', 'installing namenode')
    bigtop = Bigtop()
    hdfs_port = get_layer_opts().port('namenode')
    webhdfs_port = get_layer_opts().port('nn_webapp_http')
    bigtop.render_site_yaml(
        hosts={
            'namenode': get_fqdn(),
        },
        roles=[
            'namenode',
            'mapred-app',
        ],
        # NB: We want the NN to listen on all interfaces, so bind to 0.0.0.0.
        overrides={
            'hadoop::common_hdfs::hadoop_namenode_port': hdfs_port,
            'hadoop::common_hdfs::hadoop_namenode_bind_host': '0.0.0.0',
            'hadoop::common_hdfs::hadoop_namenode_http_port': webhdfs_port,
            'hadoop::common_hdfs::hadoop_namenode_http_bind_host': '0.0.0.0',
            'hadoop::common_hdfs::hadoop_namenode_https_bind_host': '0.0.0.0',
        }
    )
    bigtop.trigger_puppet()

    # /etc/hosts entries from the KV are not currently used for bigtop,
    # but a hosts_map attribute is required by some interfaces (eg: dfs-slave)
    # to signify NN's readiness. Set our NN info in the KV to fulfill this
    # requirement.
    utils.initialize_kv_host()

    # We need to create the 'mapred' and 'spark' user/group since we may not
    # be installing hadoop-mapreduce or spark on this machine. This is needed
    # so the namenode can access yarn and spark job history files in hdfs. Also
    # add our ubuntu user to the hadoop, mapred, and spark groups.
    get_layer_opts().add_users()

    set_state('apache-bigtop-namenode.installed')
    hookenv.status_set('maintenance', 'namenode installed')


@when('apache-bigtop-namenode.installed')
@when_not('apache-bigtop-namenode.started')
def start_namenode():
    hookenv.status_set('maintenance', 'starting namenode')
    # NB: service should be started by install, but we want to verify it is
    # running before we set the .started state and open ports. We always
    # restart here, which may seem heavy-handed. However, restart works
    # whether the service is currently started or stopped. It also ensures the
    # service is using the most current config.
    started = host.service_restart('hadoop-hdfs-namenode')
    if started:
        for port in get_layer_opts().exposed_ports('namenode'):
            hookenv.open_port(port)
        set_state('apache-bigtop-namenode.started')
        hookenv.status_set('maintenance', 'namenode started')
        hookenv.application_version_set(get_hadoop_version())
    else:
        hookenv.log('NameNode failed to start')
        hookenv.status_set('blocked', 'namenode failed to start')
        remove_state('apache-bigtop-namenode.started')
        for port in get_layer_opts().exposed_ports('namenode'):
            hookenv.close_port(port)


###############################################################################
# Slave methods
###############################################################################
@when('datanode.joined')
@when_not('apache-bigtop-namenode.installed')
def send_dn_install_info(datanode):
    """Send datanodes enough relation data to start their install."""
    send_early_install_info(datanode)


@when('apache-bigtop-namenode.started', 'datanode.joined')
def send_dn_all_info(datanode):
    """Send datanodes all dfs-slave relation data.

    At this point, the namenode is ready to serve datanodes. Send all
    dfs-slave relation data so that our 'namenode.ready' state becomes set.
    """
    bigtop = Bigtop()
    fqdn = get_fqdn()
    hdfs_port = get_layer_opts().port('namenode')
    webhdfs_port = get_layer_opts().port('nn_webapp_http')

    datanode.send_spec(bigtop.spec())
    datanode.send_namenodes([fqdn])
    datanode.send_ports(hdfs_port, webhdfs_port)

    # hosts_map, ssh_key, and clustername are required by the dfs-slave
    # interface to signify NN's readiness. Send them, even though they are not
    # utilized by bigtop.
    # NB: update KV hosts with all datanodes prior to sending the hosts_map
    # because dfs-slave gates readiness on a DN's presence in the hosts_map.
    utils.update_kv_hosts(datanode.hosts_map())
    datanode.send_hosts_map(utils.get_kv_hosts())
    datanode.send_ssh_key('invalid')
    datanode.send_clustername(hookenv.service_name())

    # update status with slave count and report ready for hdfs
    num_slaves = len(datanode.nodes())
    hookenv.status_set('active', 'ready ({count} datanode{s})'.format(
        count=num_slaves,
        s='s' if num_slaves > 1 else '',
    ))
    set_state('apache-bigtop-namenode.ready')


@when('apache-bigtop-namenode.started', 'datanode.departing')
def remove_dn(datanode):
    """Handle a departing datanode.

    This simply logs a message about a departing datanode and removes
    the entry from our KV hosts_map. The hosts_map is not used by bigtop, but
    it is required for the 'namenode.ready' state, so we may as well keep it
    accurate.
    """
    slaves_leaving = datanode.nodes()  # only returns nodes in "departing" state
    hookenv.log('Datanodes leaving: {}'.format(slaves_leaving))
    utils.remove_kv_hosts(slaves_leaving)
    datanode.dismiss()


@when('apache-bigtop-namenode.started')
@when_not('datanode.joined')
def wait_for_dn():
    remove_state('apache-bigtop-namenode.ready')
    # NB: we're still active since a user may be interested in our web UI
    # without any DNs, but let them know hdfs is caput without a DN relation.
    hookenv.status_set('active', 'hdfs requires a datanode relation')


###############################################################################
# Client methods
###############################################################################
@when('namenode.clients')
@when_not('apache-bigtop-namenode.installed')
def send_client_install_info(client):
    """Send clients enough relation data to start their install."""
    send_early_install_info(client)


@when('apache-bigtop-namenode.started', 'namenode.clients')
def send_client_all_info(client):
    """Send clients (plugin, RM, non-DNs) all dfs relation data.

    At this point, the namenode is ready to serve clients. Send all
    dfs relation data so that our 'namenode.ready' state becomes set.
    """
    bigtop = Bigtop()
    fqdn = get_fqdn()
    hdfs_port = get_layer_opts().port('namenode')
    webhdfs_port = get_layer_opts().port('nn_webapp_http')

    client.send_spec(bigtop.spec())
    client.send_namenodes([fqdn])
    client.send_ports(hdfs_port, webhdfs_port)
    # namenode.ready implies we have at least 1 datanode, which means hdfs
    # is ready for use. Inform clients of that with send_ready().
    if is_state('apache-bigtop-namenode.ready'):
        client.send_ready(True)
    else:
        client.send_ready(False)

    # hosts_map and clustername are required by the dfs interface to signify
    # NN's readiness. Send it, even though they are not utilized by bigtop.
    client.send_hosts_map(utils.get_kv_hosts())
    client.send_clustername(hookenv.service_name())
