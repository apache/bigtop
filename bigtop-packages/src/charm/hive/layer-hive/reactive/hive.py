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
from charms.layer.bigtop_hive import Hive
from charms.reactive import (
    RelationBase,
    is_state,
    remove_state,
    set_state,
    when,
    when_not,
)
from charms.reactive.helpers import data_changed


@when('bigtop.available')
def report_status():
    hadoop_joined = is_state('hadoop.joined')
    hadoop_ready = is_state('hadoop.ready')
    hbase_joined = is_state('hbase.joined')
    hbase_ready = is_state('hbase.ready')
    database_joined = is_state('database.connected')
    database_ready = is_state('database.available')
    hive_installed = is_state('hive.installed')

    if not hadoop_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to hadoop plugin')
    elif not hadoop_ready:
        hookenv.status_set('waiting',
                           'waiting for hadoop to become ready')
    elif database_joined and not database_ready:
        hookenv.status_set('waiting',
                           'waiting for database to become ready')
    elif hbase_joined and not hbase_ready:
        hookenv.status_set('waiting',
                           'waiting for hbase to become ready')
    elif hive_installed and not database_ready:
        hookenv.status_set('active',
                           'ready (local metastore)')
    elif hive_installed and database_ready:
        hookenv.status_set('active',
                           'ready (remote metastore)')


@when('bigtop.available', 'hadoop.ready')
def install_hive(hadoop):
    '''
    Anytime our dependencies are available, check to see if we have a valid
    reason to (re)install. These include:
    - initial install
    - HBase has joined/departed
    '''
    # Hive cannot handle - in the metastore db name and
    # mysql uses the service name to name the db
    if "-" in hookenv.service_name():
        hookenv.status_set('blocked', "application name may not contain '-'; "
                                      "redeploy with a different name")
        return

    # Get hbase connection dict if it's available
    if is_state('hbase.ready'):
        hbase = RelationBase.from_state('hbase.ready')
        hbserver = hbase.hbase_servers()[0]
    else:
        hbserver = None

    # Get zookeeper connection dict if it's available
    if is_state('zookeeper.ready'):
        zk = RelationBase.from_state('zookeeper.ready')
        zks = zk.zookeepers()
    else:
        zks = None

    # Use this to determine if we need to reinstall
    deployment_matrix = {
        'hbase': hbserver,
        'zookeepers': zks
    }

    # Handle nuances when installing versus re-installing
    if not is_state('hive.installed'):
        prefix = "installing"

        # On initial install, prime our kv with the current deployment matrix.
        # Subsequent calls will use this to determine if a reinstall is needed.
        data_changed('deployment_matrix', deployment_matrix)
    else:
        prefix = "configuring"

        # Return if our matrix has not changed
        if not data_changed('deployment_matrix', deployment_matrix):
            return

    hookenv.status_set('maintenance', '{} hive'.format(prefix))
    hookenv.log("{} hive with: {}".format(prefix, deployment_matrix))

    hive = Hive()
    hive.install(hbase=hbserver, zk_units=zks)
    hive.restart()
    hive.open_ports()
    set_state('hive.installed')
    report_status()

    # set app version string for juju status output
    hive_version = get_package_version('hive') or 'unknown'
    hookenv.application_version_set(hive_version)


@when('hive.installed', 'config.changed.heap')
def config_changed():
    hookenv.status_set('maintenance', 'configuring with new options')
    hive = Hive()
    hive.configure_hive()
    hive.restart()
    report_status()


@when('hive.installed', 'database.available')
@when_not('hive.db.configured')
def configure_with_remote_db(db):
    hookenv.status_set('maintenance', 'configuring external database')
    hive = Hive()
    hive.configure_remote_db(db)
    hive.restart()
    set_state('hive.db.configured')
    report_status()


@when('hive.installed', 'hive.db.configured')
@when_not('database.available')
def configure_with_local_db():
    '''
    Reconfigure Hive using a local metastore db.

    The initial installation will configure Hive with a local metastore_db.
    Once an external db becomes available, we reconfigure Hive to use it. If
    that external db goes away, we'll use this method to set Hive back into
    local mode.
    '''
    hookenv.status_set('maintenance', 'configuring local database')
    hive = Hive()
    hive.configure_local_db()
    hive.restart()
    remove_state('hive.db.configured')
    report_status()


@when('hive.installed')
@when_not('hadoop.ready')
def stop_hive():
    '''
    Hive depends on Hadoop. If we are installed and hadoop goes away, shut down
    services and remove our installed state.
    '''
    hive = Hive()
    hive.close_ports()
    hive.stop()
    remove_state('hive.installed')
    report_status()


@when('hive.installed', 'client.joined')
def serve_client(client):
    '''
    Inform clients when hive is ready to serve.
    '''
    port = get_layer_opts().port('hive-thrift')
    client.send_port(port)
    client.set_ready()


@when('client.joined')
@when_not('hive.installed')
def stop_serving_client(client):
    '''
    Inform connected clients that Hive is no longer ready. This can happen
    if Hadoop goes away (the 'installed' state will be removed).
    '''
    client.clear_ready()
