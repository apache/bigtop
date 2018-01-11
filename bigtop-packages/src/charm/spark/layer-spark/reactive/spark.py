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
from charms.layer.apache_bigtop_base import Bigtop, get_fqdn, get_package_version
from charms.layer.bigtop_spark import Spark
from charmhelpers.core import hookenv, host, unitdata
from charms import leadership
from charms.reactive.helpers import data_changed
from jujubigdata import utils


###############################################################################
# Status methods
###############################################################################
def report_status():
    mode = hookenv.config()['spark_execution_mode']
    if (not is_state('spark.yarn.installed')) and mode.startswith('yarn'):
        # if hadoop isn't here at all, we're blocked; otherwise, we're waiting
        if is_state('hadoop.joined'):
            hookenv.status_set('waiting',
                               'waiting for yarn to become ready')
        else:
            hookenv.status_set('blocked',
                               'yarn execution mode not available')
        return

    if mode == 'standalone' and is_state('zookeeper.ready'):
        mode = mode + " - HA"
    elif mode == 'standalone' and is_state('leadership.is_leader'):
        mode = mode + " - master"

    if is_state('spark.cuda.configured'):
        mode = mode + " with CUDA"

    if is_state('spark.started'):
        # inform the user if we have a different repo pkg available
        repo_ver = unitdata.kv().get('spark.version.repo', False)
        if repo_ver:
            msg = "install version {} with the 'reinstall' action".format(repo_ver)
        else:
            msg = 'ready ({})'.format(mode)
        hookenv.status_set('active', msg)
    else:
        hookenv.status_set('blocked', 'unable to start spark ({})'.format(mode))


###############################################################################
# Utility methods
###############################################################################
def get_spark_peers():
    nodes = [(hookenv.local_unit(), hookenv.unit_private_ip())]
    sparkpeer = RelationBase.from_state('sparkpeers.joined')
    if sparkpeer:
        nodes.extend(sorted(sparkpeer.get_nodes()))
    return nodes


def install_spark_standalone(zks, peers):
    """
    Called in local/standalone mode after Juju has elected a leader.
    """
    hosts = {
        'spark-master': leadership.leader_get('master-fqdn'),
    }

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

    # Let spark know if we have cuda libs installed.
    # NB: spark packages prereq hadoop (boo), so even in standalone mode, we'll
    # have hadoop libs installed. May as well include them in our lib path.
    extra_libs = ["/usr/lib/hadoop/lib/native"]
    if is_state('cuda.installed'):
        extra_libs.append("/usr/local/cuda/lib64")

    spark = Spark()
    spark.configure(hosts, zk_units=zks, peers=peers, extra_libs=extra_libs)
    set_deployment_mode_state('spark.standalone.installed')


def install_spark_yarn():
    """
    Called in 'yarn-*' mode after Juju has elected a leader. The
    'hadoop.yarn.ready' state must be set.
    """
    hosts = {
        'spark-master': leadership.leader_get('master-fqdn'),
    }
    hadoop = (RelationBase.from_state('hadoop.yarn.ready') or
              RelationBase.from_state('hadoop.hdfs.ready'))
    rms = hadoop.resourcemanagers()
    hosts['resourcemanager'] = rms[0]

    # Probably don't need to check this since yarn.ready implies hdfs.ready
    # for us, but it doesn't hurt.
    if is_state('hadoop.hdfs.ready'):
        nns = hadoop.namenodes()
        hosts['namenode'] = nns[0]

    # Always include native hadoop libs in yarn mode; add cuda libs if present.
    extra_libs = ["/usr/lib/hadoop/lib/native"]
    if is_state('cuda.installed'):
        extra_libs.append("/usr/local/cuda/lib64")

    spark = Spark()
    spark.configure(hosts, zk_units=None, peers=None, extra_libs=extra_libs)
    set_deployment_mode_state('spark.yarn.installed')


def set_deployment_mode_state(state):
    if is_state('spark.yarn.installed'):
        remove_state('spark.standalone.installed')
    if is_state('spark.standalone.installed'):
        remove_state('spark.yarn.installed')
    set_state(state)
    # set app version string for juju status output
    spark_version = get_package_version('spark-core') or 'unknown'
    hookenv.application_version_set(spark_version)


###############################################################################
# Reactive methods
###############################################################################
@when_any('master.elected',
          'hadoop.hdfs.ready', 'hadoop.yarn.ready',
          'sparkpeers.joined', 'sparkpeers.departed',
          'zookeeper.ready')
@when('bigtop.available')
@when_not('config.changed')
def reinstall_spark(force=False):
    """
    Gather the state of our deployment and (re)install when leaders, hadoop,
    sparkpeers, or zookeepers change. In the future this should also
    fire when Cassandra or any other storage comes or goes. Config changed
    events will also call this method, but that is invoked with a separate
    handler below.

    Use a deployment-matrix dict to track changes and (re)install as needed.
    """
    spark_master_host = leadership.leader_get('master-fqdn')
    if not spark_master_host:
        hookenv.status_set('maintenance', 'juju leader not elected yet')
        return

    mode = hookenv.config()['spark_execution_mode']
    peers = None
    zks = None

    # If mode is standalone and ZK is ready, we are in HA. Do not consider
    # the master_host from juju leadership in our matrix. ZK handles this.
    if (mode == 'standalone' and is_state('zookeeper.ready')):
        spark_master_host = ''
        zk = RelationBase.from_state('zookeeper.ready')
        zks = zk.zookeepers()
        # peers are only used to set our MASTER_URL in standalone HA mode
        peers = get_spark_peers()

    # Construct a deployment matrix
    sample_data = hookenv.resource_get('sample-data')
    deployment_matrix = {
        'hdfs_ready': is_state('hadoop.hdfs.ready'),
        'peers': peers,
        'sample_data': host.file_hash(sample_data) if sample_data else None,
        'spark_master': spark_master_host,
        'yarn_ready': is_state('hadoop.yarn.ready'),
        'zookeepers': zks,
    }

    # No-op if we are not forcing a reinstall or our matrix is unchanged.
    if not (force or data_changed('deployment_matrix', deployment_matrix)):
        report_status()
        return

    # (Re)install based on our execution mode
    hookenv.status_set('maintenance', 'configuring spark in {} mode'.format(mode))
    hookenv.log("Configuring spark with deployment matrix: {}".format(deployment_matrix))

    if mode.startswith('yarn') and is_state('hadoop.yarn.ready'):
        install_spark_yarn()
    elif mode.startswith('local') or mode == 'standalone':
        install_spark_standalone(zks, peers)
    else:
        # Something's wrong (probably requested yarn without yarn.ready).
        remove_state('spark.started')
        report_status()
        return

    # restart services to pick up possible config changes
    spark = Spark()
    spark.stop()
    spark.start()

    set_state('spark.started')
    report_status()


@when('bigtop.available', 'leadership.is_leader')
def send_fqdn():
    spark_master_host = get_fqdn()
    leadership.leader_set({'master-fqdn': spark_master_host})
    hookenv.log("Setting juju leader to {}".format(spark_master_host))


@when('leadership.changed.master-fqdn')
def leader_elected():
    set_state("master.elected")


@when('spark.started', 'config.changed')
def reconfigure_spark():
    """
    Reconfigure spark when user config changes.
    """
    # Almost all config changes should trigger a reinstall... except when
    # changing the bigtop repo version. Repo version changes require the user
    # to run an action, so we skip the reinstall in that case.
    if not is_state('config.changed.bigtop_version'):
        # Config changes should reinstall even if the deployment topology has
        # not changed. Hence, pass force=True.
        reinstall_spark(force=True)


@when('spark.started', 'bigtop.version.changed')
def check_repo_version():
    """
    Configure a bigtop site.yaml if a new version of spark is available.

    This method will set unitdata if a different version of spark-core is
    available in the newly configured bigtop repo. This unitdata allows us to
    configure site.yaml while gating the actual puppet apply. The user must do
    the puppet apply by calling the 'reinstall' action.
    """
    repo_ver = Bigtop().check_bigtop_repo_package('spark-core')
    if repo_ver:
        unitdata.kv().set('spark.version.repo', repo_ver)
        unitdata.kv().flush(True)
        reinstall_spark(force=True)
    else:
        unitdata.kv().unset('spark.version.repo')
    report_status()


@when('spark.started', 'cuda.installed')
@when_not('spark.cuda.configured')
def configure_cuda():
    """
    Ensure cuda bits are configured.

    We can't be sure that the config.changed handler in the nvidia-cuda
    layer will fire before the handler in this layer. We might call
    reinstall_spark on config-changed before the cuda.installed state is set,
    thereby missing the cuda lib path configuration. Deal with this by
    excplicitly calling reinstall_spark after we *know* cuda.installed is set.
    This may result in 2 calls to reinstall_spark when cuda-related config
    changes, but it ensures our spark lib config is accurate.
    """
    hookenv.log("Configuring spark with CUDA library paths")
    reinstall_spark()
    set_state('spark.cuda.configured')
    report_status()


@when('spark.started', 'spark.cuda.configured')
@when_not('cuda.installed')
def unconfigure_cuda():
    """
    Ensure cuda bits are unconfigured.

    Similar to the configure_cuda method, we can't be sure that the
    config.changed handler in the nvidia-cuda layer will fire before the
    handler in this layer. We might call reinstall_spark on config-changed
    before the cuda.installed state is removed, thereby configuring spark with
    a cuda lib path when the user wanted cuda config removed. Deal with this by
    excplicitly calling reinstall_spark after we *know* cuda.installed is gone.
    This may result in 2 calls to reinstall_spark when cuda-related config
    changes, but it ensures our spark lib config is accurate.
    """
    hookenv.log("Removing CUDA library paths from spark configuration")
    reinstall_spark()
    remove_state('spark.cuda.configured')
    report_status()


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
