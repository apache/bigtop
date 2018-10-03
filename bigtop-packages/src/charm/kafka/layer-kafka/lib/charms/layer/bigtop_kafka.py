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

import os
import shutil
from subprocess import check_output

from charmhelpers.core import hookenv
from charmhelpers.core import host
from jujubigdata import utils
from charms.layer.apache_bigtop_base import Bigtop
from charms import layer


class Kafka(object):
    """
    This class manages Kafka.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def open_ports(self):
        for port in self.dist_config.exposed_ports('kafka'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('kafka'):
            hookenv.close_port(port)

    def configure_kafka(self, zk_units, network_interface=None, log_dir=None):
        # Get ip:port data from our connected zookeepers
        zks = []
        for unit in zk_units:
            ip = utils.resolve_private_address(unit['host'])
            zks.append("%s:%s" % (ip, unit['port']))
        zks.sort()
        zk_connect = ",".join(zks)
        service, unit_num = os.environ['JUJU_UNIT_NAME'].split('/', 1)
        kafka_port = self.dist_config.port('kafka')

        roles = ['kafka-server']
        override = {
            'kafka::server::broker_id': unit_num,
            'kafka::server::port': kafka_port,
            'kafka::server::zookeeper_connection_string': zk_connect,
            'kafka::server::log_dirs': log_dir,
        }
        if network_interface:
            ip = Bigtop().get_ip_for_interface(network_interface)
            override['kafka::server::bind_addr'] = ip

        bigtop = Bigtop()
        bigtop.render_site_yaml(roles=roles, overrides=override)
        bigtop.trigger_puppet()

        if log_dir:
            os.makedirs(log_dir, mode=0o700, exist_ok=True)
            shutil.chown(log_dir, user='kafka')

        self.set_advertise()
        self.restart()

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        host.service_start('kafka-server')

    def stop(self):
        host.service_stop('kafka-server')

    def set_advertise(self):
        short_host = check_output(['hostname', '-s']).decode('utf8').strip()

        # Configure server.properties
        # NB: We set the advertised.host.name below to our short hostname
        # to kafka (admin will still have to expose kafka and ensure the
        # external client can resolve the short hostname to our public ip).
        kafka_server_conf = '/etc/kafka/conf/server.properties'
        utils.re_edit_in_place(kafka_server_conf, {
            r'^#?advertised.host.name=.*': 'advertised.host.name=%s' % short_host,
        })
