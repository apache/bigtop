
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

    If slaves or clients join before the resourcemanager is installed, we can
    still provide enough info to start their installation. This will help
    parallelize installation among our cluster.

    Note that slaves can safely install early, but should not start until the
    'resourcemanager.ready' state is set by the mapred-slave interface.
    """
    rm_host = get_fqdn()
    rm_ipc = get_layer_opts().port('resourcemanager')
    jh_ipc = get_layer_opts().port('jobhistory')
    jh_http = get_layer_opts().port('jh_webapp_http')

    remote.send_resourcemanagers([rm_host])
    remote.send_ports(rm_ipc, jh_http, jh_ipc)


###############################################################################
# Core methods
###############################################################################
@when_not('namenode.joined')
def blocked():
    hookenv.status_set('blocked', 'missing required namenode relation')


@when('bigtop.available', 'namenode.joined')
@when_not('apache-bigtop-resourcemanager.installed')
def install_resourcemanager(namenode):
    """Install if the namenode has sent its FQDN.

    We only need the namenode FQDN to perform the RM install, so poll for
    namenodes() data whenever we have a namenode relation. This allows us to
    install asap, even if 'namenode.ready' is not set yet.
    """
    if namenode.namenodes():
        hookenv.status_set('maintenance', 'installing resourcemanager')
        # Hosts
        nn_host = namenode.namenodes()[0]
        rm_host = get_fqdn()

        # Ports
        rm_ipc = get_layer_opts().port('resourcemanager')
        rm_http = get_layer_opts().port('rm_webapp_http')
        jh_ipc = get_layer_opts().port('jobhistory')
        jh_http = get_layer_opts().port('jh_webapp_http')
        hdfs_port = namenode.port()
        webhdfs_port = namenode.webhdfs_port()

        bigtop = Bigtop()
        bigtop.render_site_yaml(
            hosts={
                'namenode': nn_host,
                'resourcemanager': rm_host,
            },
            roles=[
                'resourcemanager',
            ],
            # NB: When we colocate the NN and RM, the RM will run puppet apply
            # last. To ensure we don't lose any hdfs-site.xml data set by the
            # NN, override common_hdfs properties again here.
            overrides={
                'hadoop::common_yarn::hadoop_rm_port': rm_ipc,
                'hadoop::common_yarn::hadoop_rm_webapp_port': rm_http,
                'hadoop::common_yarn::hadoop_rm_bind_host': '0.0.0.0',
                'hadoop::common_mapred_app::mapreduce_jobhistory_host': '0.0.0.0',
                'hadoop::common_mapred_app::mapreduce_jobhistory_port': jh_ipc,
                'hadoop::common_mapred_app::mapreduce_jobhistory_webapp_port': jh_http,
                'hadoop::common_hdfs::hadoop_namenode_port': hdfs_port,
                'hadoop::common_hdfs::hadoop_namenode_bind_host': '0.0.0.0',
                'hadoop::common_hdfs::hadoop_namenode_http_port': webhdfs_port,
                'hadoop::common_hdfs::hadoop_namenode_http_bind_host': '0.0.0.0',
                'hadoop::common_hdfs::hadoop_namenode_https_bind_host': '0.0.0.0',
            }
        )
        bigtop.trigger_puppet()

        # /etc/hosts entries from the KV are not currently used for bigtop,
        # but a hosts_map attribute is required by some interfaces (eg: mapred-slave)
        # to signify RM's readiness. Set our RM info in the KV to fulfill this
        # requirement.
        utils.initialize_kv_host()

        # We need to create the 'spark' user/group since we may not be
        # installing spark on this machine. This is needed so the history
        # server can access spark job history files in hdfs. Also add our
        # ubuntu user to the hadoop, mapred, and spark groups on this machine.
        get_layer_opts().add_users()

        set_state('apache-bigtop-resourcemanager.installed')
        hookenv.status_set('maintenance', 'resourcemanager installed')
    else:
        hookenv.status_set('waiting', 'waiting for namenode fqdn')


@when('apache-bigtop-resourcemanager.installed', 'namenode.joined')
@when_not('namenode.ready')
def send_nn_spec(namenode):
    """Send our resourcemanager spec so the namenode can become ready."""
    bigtop = Bigtop()
    namenode.set_local_spec(bigtop.spec())
    hookenv.status_set('waiting', 'waiting for namenode to become ready')


@when('apache-bigtop-resourcemanager.installed', 'namenode.ready')
@when_not('apache-bigtop-resourcemanager.started')
def start_resourcemanager(namenode):
    hookenv.status_set('maintenance', 'starting resourcemanager')
    # NB: service should be started by install, but we want to verify it is
    # running before we set the .started state and open ports. We always
    # restart here, which may seem heavy-handed. However, restart works
    # whether the service is currently started or stopped. It also ensures the
    # service is using the most current config.
    rm_started = host.service_restart('hadoop-yarn-resourcemanager')
    if rm_started:
        for port in get_layer_opts().exposed_ports('resourcemanager'):
            hookenv.open_port(port)
        set_state('apache-bigtop-resourcemanager.started')
        hookenv.status_set('maintenance', 'resourcemanager started')
        hookenv.application_version_set(get_hadoop_version())
    else:
        hookenv.log('YARN ResourceManager failed to start')
        hookenv.status_set('blocked', 'resourcemanager failed to start')
        remove_state('apache-bigtop-resourcemanager.started')
        for port in get_layer_opts().exposed_ports('resourcemanager'):
            hookenv.close_port(port)

    hs_started = host.service_restart('hadoop-mapreduce-historyserver')
    if not hs_started:
        hookenv.log('YARN HistoryServer failed to start')


###############################################################################
# Slave methods
###############################################################################
@when('nodemanager.joined')
@when_not('apache-bigtop-resourcemanager.installed')
def send_nm_install_info(nodemanager):
    """Send nodemanagers enough relation data to start their install."""
    send_early_install_info(nodemanager)


@when('apache-bigtop-resourcemanager.started')
@when('nodemanager.joined')
def send_nm_all_info(nodemanager):
    """Send nodemanagers all mapred-slave relation data.

    At this point, the resourcemanager is ready to serve nodemanagers. Send all
    mapred-slave relation data so that our 'resourcemanager.ready' state becomes set.
    """
    bigtop = Bigtop()
    rm_host = get_fqdn()
    rm_ipc = get_layer_opts().port('resourcemanager')
    jh_ipc = get_layer_opts().port('jobhistory')
    jh_http = get_layer_opts().port('jh_webapp_http')

    nodemanager.send_resourcemanagers([rm_host])
    nodemanager.send_spec(bigtop.spec())
    nodemanager.send_ports(rm_ipc, jh_http, jh_ipc)

    # hosts_map and ssh_key are required by the mapred-slave interface to signify
    # RM's readiness. Send them, even though they are not utilized by bigtop.
    # NB: update KV hosts with all nodemanagers prior to sending the hosts_map
    # because mapred-slave gates readiness on a NM's presence in the hosts_map.
    utils.update_kv_hosts(nodemanager.hosts_map())
    nodemanager.send_hosts_map(utils.get_kv_hosts())
    nodemanager.send_ssh_key('invalid')

    # update status with slave count and report ready for hdfs
    num_slaves = len(nodemanager.nodes())
    hookenv.status_set('active', 'ready ({count} nodemanager{s})'.format(
        count=num_slaves,
        s='s' if num_slaves > 1 else '',
    ))
    set_state('apache-bigtop-resourcemanager.ready')


@when('apache-bigtop-resourcemanager.started')
@when('nodemanager.departing')
def remove_nm(nodemanager):
    """Handle a departing nodemanager.

    This simply logs a message about a departing nodemanager and removes
    the entry from our KV hosts_map. The hosts_map is not used by bigtop, but
    it is required for the 'resourcemanager.ready' state, so we may as well
    keep it accurate.
    """
    slaves_leaving = nodemanager.nodes()  # only returns nodes in "departing" state
    hookenv.log('Nodemanagers leaving: {}'.format(slaves_leaving))
    utils.remove_kv_hosts(slaves_leaving)
    nodemanager.dismiss()


@when('apache-bigtop-resourcemanager.started')
@when_not('nodemanager.joined')
def wait_for_nm():
    remove_state('apache-bigtop-resourcemanager.ready')
    # NB: we're still active since a user may be interested in our web UI
    # without any NMs, but let them know yarn is caput without a NM relation.
    hookenv.status_set('active', 'yarn requires a nodemanager relation')


###############################################################################
# Client methods
###############################################################################
@when('resourcemanager.clients')
@when_not('apache-bigtop-resourcemanager.installed')
def send_client_install_info(client):
    """Send clients enough relation data to start their install."""
    send_early_install_info(client)


@when('apache-bigtop-resourcemanager.started')
@when('resourcemanager.clients')
def send_client_all_info(client):
    """Send clients (plugin, RM, non-DNs) all dfs relation data.

    At this point, the resourcemanager is ready to serve clients. Send all
    mapred relation data so that our 'resourcemanager.ready' state becomes set.
    """
    bigtop = Bigtop()
    rm_host = get_fqdn()
    rm_ipc = get_layer_opts().port('resourcemanager')
    jh_ipc = get_layer_opts().port('jobhistory')
    jh_http = get_layer_opts().port('jh_webapp_http')

    client.send_resourcemanagers([rm_host])
    client.send_spec(bigtop.spec())
    client.send_ports(rm_ipc, jh_http, jh_ipc)

    # resourcemanager.ready implies we have at least 1 nodemanager, which means
    # yarn is ready for use. Inform clients of that with send_ready().
    if is_state('apache-bigtop-resourcemanager.ready'):
        client.send_ready(True)
    else:
        client.send_ready(False)

    # hosts_map is required by the mapred interface to signify
    # RM's readiness. Send it, even though it is not utilized by bigtop.
    client.send_hosts_map(utils.get_kv_hosts())


###############################################################################
# Benchmark methods
###############################################################################
@when('benchmark.joined')
def register_benchmarks(benchmark):
    benchmark.register('mrbench', 'nnbench', 'terasort', 'testdfsio')
