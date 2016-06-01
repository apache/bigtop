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

from charmhelpers.core import hookenv, host
from jujubigdata import utils
from charms.layer.apache_bigtop_base import Bigtop
from charms import layer


class HBase(object):
    """
    This class manages HBase.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def configure(self, hosts, zk_units):
        zks = []
        for unit in zk_units:
            ip = utils.resolve_private_address(unit['host'])
            zks.append(ip)
        zks.sort()
        zk_connect = ",".join(zks)

        roles = ['hbase-server', 'hbase-master', 'hbase-client']

        override = {
            'hadoop_hbase::common_config::zookeeper_quorum': zk_connect,
            'hadoop_hbase::deploy::auxiliary': False
        }

        bigtop = Bigtop()
        bigtop.render_site_yaml(hosts, roles, override)
        bigtop.trigger_puppet()

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        host.service_start('hbase-master')
        host.service_start('hbase-regionserver')
        host.service_start('hbase-thrift')

    def stop(self):
        host.service_stop('hbase-master')
        host.service_stop('hbase-regionserver')
        host.service_stop('hbase-thrift')

    def open_ports(self):
        for port in self.dist_config.exposed_ports('hbase'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('hbase'):
            hookenv.close_port(port)
