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

import json
import time
from charmhelpers.core import hookenv
from charms.layer.apache_bigtop_base import get_package_version
from charms.layer.bigtop_zookeeper import Zookeeper
from charms.leadership import leader_set, leader_get
from charms.reactive import (
    hook,
    is_state,
    remove_state,
    set_state,
    when,
    when_not
)
from charms.reactive.helpers import data_changed
import shutil
import os


@when('local-monitors.available')
def local_monitors_available(nagios):
    setup_nagios(nagios)


@when('nrpe-external-master.available')
def nrpe_external_master_available(nagios):
    setup_nagios(nagios)


def setup_nagios(nagios):
    config = hookenv.config()
    unit_name = hookenv.local_unit()
    checks = [
        {
            'name': 'zk_open_file_descriptor_count',
            'description': 'ZK_Open_File_Descriptors_Count',
            'warn': config['open_file_descriptor_count_warn'],
            'crit': config['open_file_descriptor_count_crit']
        },
        {
            'name': 'zk_ephemerals_count',
            'description': 'ZK_Ephemerals_Count',
            'warn': config['ephemerals_count_warn'],
            'crit': config['ephemerals_count_crit']
        },
        {
            'name': 'zk_avg_latency',
            'description': 'ZK_Avg_Latency',
            'warn': config['avg_latency_warn'],
            'crit': config['avg_latency_crit']
        },
        {
            'name': 'zk_max_latency',
            'description': 'ZK_Max_Latency',
            'warn': config['max_latency_warn'],
            'crit': config['max_latency_crit']
        },
        {
            'name': 'zk_min_latency',
            'description': 'ZK_Min_Latency',
            'warn': config['min_latency_warn'],
            'crit': config['min_latency_crit']
        },
        {
            'name': 'zk_outstanding_requests',
            'description': 'ZK_Outstanding_Requests',
            'warn': config['outstanding_requests_warn'],
            'crit': config['outstanding_requests_crit']
        },
        {
            'name': 'zk_watch_count',
            'description': 'ZK_Watch_Count',
            'warn': config['watch_count_warn'],
            'crit': config['watch_count_crit']
        },
    ]
    check_cmd = ['/usr/local/lib/nagios/plugins/check_zookeeper.py',
                 '-o', 'nagios', '-s', 'localhost:2181']
    for check in checks:
        nagios.add_check(check_cmd + ['--key', check['name'],
                                      '-w', str(check['warn']),
                                      '-c', str(check['crit'])],
                         name=check['name'],
                         description=check['description'],
                         context=config["nagios_context"],
                         servicegroups=config["nagios_servicegroups"],
                         unit=unit_name
                         )
    nagios.updated()


@hook('upgrade-charm')
def nrpe_helper_upgrade_charm():
    # Make sure the nrpe handler will get replaced at charm upgrade
    remove_state('zookeeper.nrpe_helper.installed')


@when('zookeeper.nrpe_helper.registered')
@when_not('zookeeper.nrpe_helper.installed')
def install_nrpe_helper():
    dst_dir = '/usr/local/lib/nagios/plugins/'
    if not os.path.exists(dst_dir):
        os.makedirs(dst_dir)
    src = '{}/files/check_zookeeper.py'.format(hookenv.charm_dir())
    dst = '{}/check_zookeeper.py'.format(dst_dir)
    shutil.copy(src, dst)
    os.chmod(dst, 0o755)
    set_state('zookeeper.nrpe_helper.installed')


@when('bigtop.available')
@when_not('zookeeper.installed')
def install_zookeeper():
    '''
    After Bigtop has done the initial setup, trigger a puppet install,
    via our Zooekeeper library.

    puppet will start the service, as a side effect.

    '''
    hookenv.status_set('maintenance', 'installing zookeeper')
    zookeeper = Zookeeper()
    # Prime data changed
    data_changed('zkpeer.nodes', zookeeper.read_peers())
    data_changed(
        'zk.network_interface',
        hookenv.config().get('network_interface'))
    data_changed(
        'zk.autopurge_purge_interval',
        hookenv.config().get('autopurge_purge_interval'))
    data_changed(
        'zk.autopurge_snap_retain_count',
        hookenv.config().get('autopurge_snap_retain_count'))
    zookeeper.install()
    zookeeper.open_ports()
    set_state('zookeeper.installed')
    set_state('zookeeper.started')
    hookenv.status_set('active', 'ready {}'.format(zookeeper.quorum_check()))
    # set app version string for juju status output
    zoo_version = get_package_version('zookeeper') or 'unknown'
    hookenv.application_version_set(zoo_version)


def _restart_zookeeper(msg):
    '''
    Restart Zookeeper by re-running the puppet scripts.

    '''
    hookenv.status_set('maintenance', msg)
    zookeeper = Zookeeper()
    zookeeper.install()
    hookenv.status_set('active', 'ready {}'.format(zookeeper.quorum_check()))


@when('zookeeper.started')
def update_network_interface():
    '''
    Possibly restart zookeeper, due to the network interface that it
    should listen on changing.

    '''
    network_interface = hookenv.config().get('network_interface')
    if data_changed('zk.network_interface', network_interface):
        _restart_zookeeper('updating network interface')


@when('zookeeper.started')
def update_autopurge_purge_interval():
    purge_interval = hookenv.config().get('autopurge_purge_interval')
    if data_changed('zk.autopurge_purge_interval', purge_interval):
        _restart_zookeeper('updating snapshot purge interval')


@when('zookeeper.started')
def update_autopurge_snap_retain_count():
    snap_retain = hookenv.config().get('autopurge_snap_retain_count')
    if data_changed('zk.autopurge_snap_retain_count', snap_retain):
        _restart_zookeeper('updating number of retained snapshots')


@when('zookeeper.started', 'zookeeper.joined')
def serve_client(client):
    config = Zookeeper().dist_config
    port = config.port('zookeeper')
    rest_port = config.port('zookeeper-rest')  # TODO: add zookeeper REST
    client.send_port(port, rest_port)


#
# Rolling restart -- helpers and handlers
#
# When we add or remove a Zookeeper peer, Zookeeper needs to perform a
# rolling restart of all of its peers, restarting the Zookeeper
# "leader" last.
#
# The following functions accomplish this. Here's how they all fit together:
#
# (As you read, keep in mind that one node functions as the "leader"
# in the context of Juju, and one node functions as the "leader" in
# the context of Zookeeper; these nodes may or may not be the same.)
#
# 0. Whenever the Zookeeper server starts, it attempts to determine
#    whether it is the Zookeeper leader. If so, it sets a flag on the
#    Juju peer relation data.
#
# 1. When a node is added or remove from the cluster, the Juju leader
#    runs `check_cluster`, and generates a "restart queue" comprising
#    nodes in the cluster, with the Zookeeper lead node sorted last in
#    the queue. It also sets a nonce, to identify this restart queue
#    uniquely, and thus handle the situation where another node is
#    added or restarted while we're still reacting to the first node's
#    addition or removal. The leader drops the queue and nonce into
#    the leadership data as "restart_queue" and "restart_nonce",
#    respectively.
#
# 2. When any node detects a leadership.changed.restart_queue event,
#    it runs `restart_for_quorum`, which is a noop unless the node's
#    private address is the first element of the restart queue. In
#    that case, if the node is the Juju leader, it will restart, then
#    remove itself from the restart queue, triggering another
#    leadership.changed.restart_queue event. If the node isn't the
#    Juju leader, it will restart itself, then run `inform_restart`.
#
# 3. `inform_restart` will create a relation data changed event, which
#    triggers `update_restart_queue` to run on the leader. This method
#    will update the restart_queue, clearing any nodes that have
#    restarted for the current nonce, and looping us back to step 2.
#
# 4. Once all the nodes have restarted, we should be in the following state:
#
#    * All nodes have an updated Zookeeper server running with the new
#    * peer data.
#
#    * The Zookeeper leader has restarted last, which should help
#      prevent orphaned jobs, per the Zookeeper docs.
#
#    * peers still have zkpeer.restarted.<nonce> set on their relation
#      data. This is okay, as we will generate a new nonce next time,
#      and the data is small.
#
# Edge cases and potential bugs:
#
# 1. Juju leader changes in the middle of a restart: this gets a
#    little bit dicey, but it should work. The new leader should run
#    `check_cluster_departed`, and start a new restart_queue.
#

def _ip_list(nodes):
    '''
    Given a list of nodes, in the format that our peer relation or
    zookeeper lib will typically return node lists in, make a list of
    just the ips (stripping ports, if they have been added).

    We expect the list we passed in to look something like this:

        [('zookeeper/0', '10.0.0.4'), ('zookeeper/1', '10.0.0.5')]

    or this:

        [('0', '10.0.0.4:2888:4888'), ('1', '10.0.0.5:2888:4888')]

    We will return a list in the form:

        ['10.0.0.4', '10.0.0.5']

    '''
    return [node[1].split(':')[0] for node in nodes]


@when('zookeeper.started', 'leadership.is_leader', 'zkpeer.joined')
@when_not('zkpeer.departed')
def check_cluster(zkpeer):
    '''
    Checkup on the state of the cluster. Start a rolling restart if
    the peers have changed.

    '''
    zk = Zookeeper()
    if data_changed('zkpeer.nodes', zk.read_peers()):
        peers = _ip_list(zk.sort_peers(zkpeer))
        nonce = time.time()
        hookenv.log('Quorum changed. Restart queue: {}'.format(peers))
        leader_set(
            restart_queue=json.dumps(peers),
            restart_nonce=json.dumps(nonce)
        )


@when('zookeeper.started', 'leadership.is_leader', 'zkpeer.joined',
      'zkpeer.departed')
def check_cluster_departed(zkpeer, zkpeer_departed):
    '''
    Wrapper around check_cluster.

    Together with check_cluster, implements the following logic:

    "Run this when zkpeer.joined and zkpeer departed, or zkpeer.joined
    and not zkpeer.departed"

    '''
    check_cluster(zkpeer)


@when('zookeeper.started', 'leadership.is_leader', 'zkpeer.changed')
def check_cluster_changed(zkpeer):
    check_cluster(zkpeer)
    zkpeer.dismiss_changed()


@when('leadership.changed.restart_queue', 'zkpeer.joined')
def restart_for_quorum(zkpeer):
    '''
    If we're the next node in the restart queue, restart, and then
    inform the leader that we've restarted. (If we are the leader,
    remove ourselves from the queue, and update the leadership data.)

    '''
    private_address = hookenv.unit_get('private-address')
    queue = json.loads(leader_get('restart_queue') or '[]')

    if not queue:
        # Everything has restarted.
        return

    if private_address == queue[0]:
        # It's our turn to restart.
        _restart_zookeeper('rolling restart for quorum update')
        if is_state('leadership.is_leader'):
            queue = queue[1:]
            hookenv.log('Leader updating restart queue: {}'.format(queue))
            leader_set(restart_queue=json.dumps(queue))
        else:
            zkpeer.inform_restart()


@when('leadership.is_leader', 'zkpeer.joined')
def update_restart_queue(zkpeer):
    '''
    If a Zookeeper node has restarted as part of a rolling restart,
    pop it off of the queue.

    '''
    queue = json.loads(leader_get('restart_queue') or '[]')
    if not queue:
        return

    restarted_nodes = _ip_list(zkpeer.restarted_nodes())
    new_queue = [node for node in queue if node not in restarted_nodes]

    if new_queue != queue:
        hookenv.log('Leader updating restart queue: {}'.format(queue))
        leader_set(restart_queue=json.dumps(new_queue))
