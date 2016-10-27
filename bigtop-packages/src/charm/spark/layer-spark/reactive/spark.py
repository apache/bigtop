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
from charms.reactive import RelationBase, when, when_not, is_state, set_state, remove_state, when_any
from charms.layer.apache_bigtop_base import get_fqdn, get_package_version
from charms.layer.bigtop_spark import Spark
from charmhelpers.core import hookenv
from charms import leadership
from charms.reactive.helpers import data_changed
from jujubigdata import utils


def set_deployment_mode_state(state):
    if is_state('spark.yarn.installed'):
        remove_state('spark.yarn.installed')
    if is_state('spark.standalone.installed'):
        remove_state('spark.standalone.installed')
    set_state('spark.started')
    set_state(state)
    # set app version string for juju status output
    spark_version = get_package_version('spark-core') or 'unknown'
    hookenv.application_version_set(spark_version)


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


def install_spark(hadoop=None, zks=None):
    spark_master_host = leadership.leader_get('master-fqdn')
    if not spark_master_host:
        hookenv.status_set('waiting', 'master not elected yet')
        return False

    hosts = {
        'spark-master': spark_master_host,
    }

    if is_state('hadoop.yarn.ready'):
        rms = hadoop.resourcemanagers()
        hosts['resourcemanager'] = rms[0]

    if is_state('hadoop.hdfs.ready'):
        nns = hadoop.namenodes()
        hosts['namenode'] = nns[0]

    spark = Spark()
    spark.configure(hosts, zks, get_spark_peers())
    return True


@when('config.changed', 'spark.started')
def reconfigure_spark():
    config = hookenv.config()
    mode = config['spark_execution_mode']
    hookenv.status_set('maintenance',
                       'changing default execution mode to {}'.format(mode))

    hadoop = (RelationBase.from_state('hadoop.yarn.ready') or
              RelationBase.from_state('hadoop.hdfs.ready'))

    zks = None
    if is_state('zookeeper.ready'):
        zk = RelationBase.from_state('zookeeper.ready')
        zks = zk.zookeepers()

    if install_spark(hadoop, zks):
        report_status()


# This is a triky call. We want to fire when the leader changes, yarn and hdfs become ready or
# depart. In the future this should fire when Cassandra or any other storage
# becomes ready or departs. Since hdfs and yarn do not have a departed state we make sure
# we fire this method always ('spark.started'). We then build a deployment-matrix
# and if anything has changed we re-install.
# 'hadoop.yarn.ready', 'hadoop.hdfs.ready' can be ommited but I like them here for clarity
@when_any('hadoop.yarn.ready',
          'hadoop.hdfs.ready', 'master.elected', 'sparkpeers.joined', 'zookeeper.ready')
@when('bigtop.available', 'master.elected')
def reinstall_spark():
    spark_master_host = leadership.leader_get('master-fqdn')
    peers = []
    zks = []
    if is_state('zookeeper.ready'):
        # if ZK is availuable we are in HA. We do not want reconfigurations if a leader fails
        # HA takes care of this
        spark_master_host = ''
        zk = RelationBase.from_state('zookeeper.ready')
        zks = zk.zookeepers()
        # We need reconfigure Spark when in HA and peers change ignore otherwise
        peers = get_spark_peers()

    deployment_matrix = {
        'spark_master': spark_master_host,
        'yarn_ready': is_state('hadoop.yarn.ready'),
        'hdfs_ready': is_state('hadoop.hdfs.ready'),
        'zookeepers': zks,
        'peers': peers,
    }

    if not data_changed('deployment_matrix', deployment_matrix):
        return

    hookenv.status_set('maintenance', 'configuring spark')
    hadoop = (RelationBase.from_state('hadoop.yarn.ready') or
              RelationBase.from_state('hadoop.hdfs.ready'))
    if install_spark(hadoop, zks):
        if is_state('hadoop.yarn.ready'):
            set_deployment_mode_state('spark.yarn.installed')
        else:
            set_deployment_mode_state('spark.standalone.installed')

        report_status()


def get_spark_peers():
    nodes = [(hookenv.local_unit(), hookenv.unit_private_ip())]
    sparkpeer = RelationBase.from_state('sparkpeers.joined')
    if sparkpeer:
        nodes.extend(sorted(sparkpeer.get_nodes()))
    return nodes


@when('leadership.is_leader', 'bigtop.available')
def send_fqdn():
    spark_master_host = get_fqdn()
    leadership.leader_set({'master-fqdn': spark_master_host})
    hookenv.log("Setting leader to {}".format(spark_master_host))


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
