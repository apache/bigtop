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
import time

from charms.reactive import RelationBase, when, when_not, is_state, set_state, remove_state, when_any
from charms.layer.apache_bigtop_base import get_fqdn, get_package_version
from charms.layer.bigtop_spark import Spark
from charmhelpers.core import hookenv
from charms import leadership
from charms.reactive.helpers import data_changed
from jujubigdata import utils


###############################################################################
# Status methods
###############################################################################
def report_status():
    mode = hookenv.config()['spark_execution_mode']
    if (not is_state('spark.yarn.installed')) and mode.startswith('yarn'):
        hookenv.status_set('blocked',
                           'yarn execution mode not available')
        return

    if mode == 'standalone' and is_state('zookeeper.ready'):
        mode = mode + " - HA"
    elif mode == 'standalone' and is_state('leadership.is_leader'):
        mode = mode + " - master"

    hookenv.status_set('active', 'ready ({})'.format(mode))


###############################################################################
# Utility methods
###############################################################################
def get_spark_peers():
    nodes = [(hookenv.local_unit(), hookenv.unit_private_ip())]
    sparkpeer = RelationBase.from_state('sparkpeers.joined')
    if sparkpeer:
        nodes.extend(sorted(sparkpeer.get_nodes()))
    return nodes


def install_spark(hadoop, zks, peers):
    """
    Must be called after Juju has elected a leader.
    """
    hosts = {
        'spark-master': leadership.leader_get('master-fqdn'),
    }

    mode = hookenv.config()['spark_execution_mode']
    # Only include hadoop hosts if we're in yarn-* mode
    if mode.startswith('yarn'):
        if is_state('hadoop.yarn.ready'):
            rms = hadoop.resourcemanagers()
            hosts['resourcemanager'] = rms[0]

        if is_state('hadoop.hdfs.ready'):
            nns = hadoop.namenodes()
            hosts['namenode'] = nns[0]

    spark = Spark()
    hookenv.status_set('maintenance', 'configuring spark in mode: {}'.format(mode))
    spark.configure(hosts, zks, peers)
    # BUG: if a zk, spark master will go into recovery; workers will need to
    # be restarted after the master becomes alive again.


def set_deployment_mode_state(state):
    if is_state('spark.yarn.installed'):
        remove_state('spark.standalone.installed')
    if is_state('spark.standalone.installed'):
        remove_state('spark.yarn.installed')
    set_state('spark.started')
    set_state(state)
    # set app version string for juju status output
    spark_version = get_package_version('spark-core') or 'unknown'
    hookenv.application_version_set(spark_version)


###############################################################################
# Reactive methods
###############################################################################
@when_any('config.changed', 'master.elected',
          'hadoop.hdfs.ready', 'hadoop.yarn.ready',
          'sparkpeers.joined', 'sparkpeers.departed',
          'zookeeper.ready')
@when('bigtop.available', 'master.elected')
def reinstall_spark():
    """
    This is tricky. We want to fire on config or leadership changes, or when
    hadoop, sparkpeers, or zookeepers come and go. In the future this should
    fire when Cassandra or any other storage comes or goes. We always fire
    this method (or rather, when bigtop is ready and juju has elected a
    master). We then build a deployment-matrix and (re)install as things
    change.
    """
    spark_master_host = leadership.leader_get('master-fqdn')
    if not spark_master_host:
        hookenv.status_set('maintenance', 'juju leader not elected yet')
        return

    # If ZK is available, we are in HA. Do not consider the master_host
    # from juju leadership in our matrix. ZK takes care of this.
    zks = None
    if is_state('zookeeper.ready'):
        spark_master_host = ''
        zk = RelationBase.from_state('zookeeper.ready')
        zks = zk.zookeepers()

    peers = get_spark_peers()
    deployment_matrix = {
        'spark_master': spark_master_host,
        'yarn_ready': is_state('hadoop.yarn.ready'),
        'hdfs_ready': is_state('hadoop.hdfs.ready'),
        'zookeepers': zks,
        'peers': peers,
    }

    # If neither config nor our matrix is changing, there is nothing to do.
    if (not is_state('config.changed') and
            not data_changed('deployment_matrix', deployment_matrix)):
        return

    # If zks have changed and we are not handling a departed spark peer,
    # give the ensemble time to settle. Otherwise we might try to start
    # spark master with data from the wrong zk leader. Doing so will cause
    # spark-master to shutdown:
    #  https://issues.apache.org/jira/browse/SPARK-15544
    if (zks and data_changed('zks', zks) and not is_state('sparkpeers.departed')):
        hookenv.status_set('maintenance',
                           'waiting for zookeeper ensemble to settle')
        hookenv.log("Waiting 2m to ensure zk ensemble has settled: {}".format(zks))
        time.sleep(120)

    hadoop = (RelationBase.from_state('hadoop.yarn.ready') or
              RelationBase.from_state('hadoop.hdfs.ready'))
    install_spark(hadoop, zks, peers)
    if is_state('hadoop.yarn.ready'):
        set_deployment_mode_state('spark.yarn.installed')
    else:
        set_deployment_mode_state('spark.standalone.installed')

    report_status()


@when('bigtop.available', 'leadership.is_leader')
def send_fqdn():
    spark_master_host = get_fqdn()
    leadership.leader_set({'master-fqdn': spark_master_host})
    hookenv.log("Setting juju leader to {}".format(spark_master_host))


@when('leadership.changed.master-fqdn')
def leader_elected():
    set_state("master.elected")


@when('spark.started', 'client.joined')
def client_present(client):
    if is_state('leadership.is_leader'):
        client.set_spark_started()
        spark = Spark()
        master_ip = utils.resolve_private_address(hookenv.unit_private_ip())
        master_url = spark.get_master_url(master_ip)
        client.send_master_info(master_url, master_ip)


@when('client.joined')
@when_not('spark.started')
def client_should_stop(client):
    if is_state('leadership.is_leader'):
        client.clear_spark_started()
