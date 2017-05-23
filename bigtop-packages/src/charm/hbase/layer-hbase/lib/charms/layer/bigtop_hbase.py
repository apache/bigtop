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

from charmhelpers.core import hookenv, host, unitdata
from charms import layer
from charms.layer.apache_bigtop_base import Bigtop
from jujubigdata import utils
from path import Path


class HBase(object):
    '''This class manages HBase.'''
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def configure(self, hosts, zk_units):
        zk_connect = self.get_zk_connect(zk_units)
        roles = ['hbase-server', 'hbase-master', 'hbase-client']
        override = {
            'bigtop::hbase_thrift_port': self.dist_config.port('hbase-thrift'),
            'hadoop_hbase::client::thrift': True,
            'hadoop_hbase::common_config::heap_size': hookenv.config()['heap'],
            'hadoop_hbase::common_config::zookeeper_quorum': zk_connect,
            'hadoop_hbase::deploy::auxiliary': False,
        }

        bigtop = Bigtop()
        bigtop.render_site_yaml(hosts, roles, override)
        bigtop.trigger_puppet()

    def get_zk_connect(self, zk_units):
        zks = []
        for unit in zk_units:
            ip = utils.resolve_private_address(unit['host'])
            zks.append(ip)
        zks.sort()
        return ",".join(zks)

    def update_regionservers(self, addrs, remove=False):
        '''
        Each HBase unit in the cluster runs a RegionServer process. Ensure
        all unit IP addresses are listed in the regionservers file.

        @param: addrs List of IP addresses
        @param: remove Bool to add (False) or remove (True) unit IPs
        '''
        unit_kv = unitdata.kv()
        kv_ips = unit_kv.get('regionservers', default=[])

        # add/remove IPs from our list
        if remove:
            kv_ips = [ip for ip in kv_ips if ip not in addrs]
        else:
            kv_ips.extend(addrs)

        # write regionservers file using a sorted, unique set of addrs
        new_kv = sorted(set(kv_ips))
        rs_file = Path('/etc/hbase/conf/regionservers')
        rs_file.write_lines(
            [
                '# DO NOT EDIT',
                '# This file is automatically managed by Juju',
            ] + [ip for ip in new_kv],
            append=False
        )

        # save the new kv IPs
        unit_kv.set('regionservers', new_kv)
        unit_kv.flush(True)

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        # order is important; master must start first.
        hookenv.log('Starting HBase services')
        host.service_start('hbase-master')
        host.service_start('hbase-regionserver')
        host.service_start('hbase-thrift')
        hookenv.log('HBase services have been started')

    def stop(self):
        # order is important; master must stop last.
        hookenv.log('Stopping HBase services')
        host.service_stop('hbase-thrift')
        host.service_stop('hbase-regionserver')
        host.service_stop('hbase-master')
        hookenv.log('HBase services have been stopped')

    def open_ports(self):
        for port in self.dist_config.exposed_ports('hbase'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('hbase'):
            hookenv.close_port(port)
