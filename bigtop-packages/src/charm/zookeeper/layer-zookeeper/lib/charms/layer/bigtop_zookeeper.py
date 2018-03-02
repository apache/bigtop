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

import subprocess

from charmhelpers.core import host
from charmhelpers.core.hookenv import (open_port, close_port, log,
                                       unit_private_ip, local_unit, config)
from charms import layer
from charms.layer.apache_bigtop_base import Bigtop
from charms.reactive.relations import RelationBase
from jujubigdata.utils import DistConfig


def format_node(unit, node_ip):
    '''
    Given a juju unit name and an ip address, return a tuple
    containing an id and formatted ip string suitable for passing to
    puppet, which will write it out to zoo.cfg.

    '''
    return (unit.split("/")[1], "{ip}:2888:3888".format(ip=node_ip))


class Zookeeper(object):
    '''
    Utility class for managing Zookeeper tasks like configuration, start,
    stop, and adding and removing nodes.

    '''
    def __init__(self, dist_config=None):
        self._dist_config = dist_config or DistConfig(
            data=layer.options('apache-bigtop-base'))

        self._roles = ['zookeeper-server', 'zookeeper-client']
        self._hosts = {}

    def is_zk_leader(self):
        '''
        Attempt to determine whether this node is the Zookeeper leader.

        Note that Zookeeper tracks leadership independently of juju,
        and that this command can fail, depending on the state that
        the Zookeeper node is in when we attempt to run it.

        '''
        try:
            status = subprocess.check_output(
                ["/usr/lib/zookeeper/bin/zkServer.sh", "status"])
            return "leader" in status.decode('utf-8')
        except Exception:
            log(
                "Unable to determine whether this node is the Zookeeper leader.",
                level="WARN"
            )
            return False

    def read_peers(self):
        '''
        Fetch the list of peers available.

        The first item in this list should always be the node that
        this code is executing on.

        '''
        # A Zookeeper node likes to be first on the list.
        nodes = [(local_unit(), unit_private_ip())]
        # Get the list of peers
        zkpeer = RelationBase.from_state('zkpeer.joined')
        if zkpeer:
            nodes.extend(sorted(zkpeer.get_nodes()))
        nodes = [format_node(*node) for node in nodes]
        return nodes

    def sort_peers(self, zkpeer):
        '''
        Return peers, sorted in an order suitable for performing a rolling
        restart.

        '''
        peers = self.read_peers()
        leader = zkpeer.find_zk_leader()
        peers.sort(key=lambda x: x[1] == leader)

        return peers

    @property
    def dist_config(self):
        '''
        Charm level config.

        '''
        return self._dist_config

    @property
    def _override(self):
        '''
        Return a dict of keys and values that will override puppet's
        defaults.

        '''
        override = {
            "hadoop_zookeeper::server::myid": local_unit().split("/")[1],
            "hadoop_zookeeper::server::ensemble": self.read_peers()
        }
        conf = config()
        network_interface = conf.get('network_interface')
        autopurge_purge_interval = conf.get('autopurge_purge_interval')
        autopurge_snap_retain_count = conf.get('autopurge_snap_retain_count')
        if network_interface:
            key = "hadoop_zookeeper::server::client_bind_addr"
            override[key] = Bigtop().get_ip_for_interface(network_interface)
        if autopurge_purge_interval:
            key = "hadoop_zookeeper::server::autopurge_purge_interval"
            override[key] = autopurge_purge_interval
        if autopurge_snap_retain_count:
            key = "hadoop_zookeeper::server::autopurge_snap_retain_count"
            override[key] = autopurge_snap_retain_count

        return override

    def install(self, nodes=None):
        '''
        Write out the config, then run puppet.

        After this runs, we should have a configured and running service.

        '''
        bigtop = Bigtop()
        log("Rendering site yaml ''with overrides: {}".format(self._override))
        bigtop.render_site_yaml(self._hosts, self._roles, self._override)
        bigtop.trigger_puppet()
        if self.is_zk_leader():
            zkpeer = RelationBase.from_state('zkpeer.joined')
            zkpeer.set_zk_leader()

    def start(self):
        '''
        Request that our service start. Normally, puppet will handle this
        for us.

        '''
        host.service_start('zookeeper-server')

    def stop(self):
        '''
        Stop Zookeeper.

        '''
        host.service_stop('zookeeper-server')

    def open_ports(self):
        '''
        Expose the ports in the configuration to the outside world.

        '''
        for port in self.dist_config.exposed_ports('zookeeper'):
            open_port(port)

    def close_ports(self):
        '''
        Close off communication from the outside world.

        '''
        for port in self.dist_config.exposed_ports('zookeeper'):
            close_port(port)

    def quorum_check(self):
        '''
        Returns a string reporting the node count. Append a message
        informing the user if the node count is too low for good quorum,
        or is even (meaning that one of the nodes is redundant for
        quorum).

        '''
        node_count = len(self.read_peers())
        if node_count == 1:
            count_str = "{} unit".format(node_count)
        else:
            count_str = "{} units".format(node_count)
        if node_count < 3:
            return " ({}; less than 3 is suboptimal)".format(count_str)
        if node_count % 2 == 0:
            return " ({}; an even number is suboptimal)".format(count_str)
        return "({})".format(count_str)
