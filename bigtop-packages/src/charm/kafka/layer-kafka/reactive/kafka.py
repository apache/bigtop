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

from charmhelpers.core import hookenv, unitdata
from charms.layer.apache_bigtop_base import get_layer_opts, get_package_version
from charms.layer.bigtop_kafka import Kafka
from charms.reactive import set_state, remove_state, when, when_not, hook
from charms.reactive.helpers import data_changed


@when('bigtop.available')
@when_not('zookeeper.joined')
def waiting_for_zookeeper():
    hookenv.status_set('blocked', 'waiting for relation to zookeeper')


@when('bigtop.available', 'zookeeper.joined')
@when_not('kafka.started', 'zookeeper.ready')
def waiting_for_zookeeper_ready(zk):
    hookenv.status_set('waiting', 'waiting for zookeeper to become ready')


@when('bigtop.available', 'zookeeper.ready')
@when_not('kafka.started')
def configure_kafka(zk):
    hookenv.status_set('maintenance', 'setting up kafka')
    data_changed(  # Prime data changed for network interface
        'kafka.network_interface', hookenv.config().get('network_interface'))
    log_dir = unitdata.kv().get('kafka.storage.log_dir')
    data_changed('kafka.storage.log_dir', log_dir)
    kafka = Kafka()
    zks = zk.zookeepers()
    kafka.configure_kafka(zks, log_dir=log_dir)
    kafka.open_ports()
    set_state('kafka.started')
    hookenv.status_set('active', 'ready')
    # set app version string for juju status output
    kafka_version = get_package_version('kafka') or 'unknown'
    hookenv.application_version_set(kafka_version)


@when('kafka.started', 'zookeeper.ready')
def configure_kafka_zookeepers(zk):
    """Configure ready zookeepers and restart kafka if needed.

    As zks come and go, server.properties will be updated. When that file
    changes, restart Kafka and set appropriate status messages.

    This method also handles the restart if our network_interface
    config has changed.

    """
    zks = zk.zookeepers()
    network_interface = hookenv.config().get('network_interface')
    log_dir = unitdata.kv().get('kafka.storage.log_dir')
    if not(any((
            data_changed('zookeepers', zks),
            data_changed('kafka.network_interface', network_interface),
            data_changed('kafka.storage.log_dir', log_dir)))):
        return

    hookenv.log('Checking Zookeeper configuration')
    hookenv.status_set('maintenance', 'updating zookeeper instances')
    kafka = Kafka()
    kafka.configure_kafka(zks, network_interface=network_interface,
                          log_dir=log_dir)
    hookenv.status_set('active', 'ready')


@when('kafka.started')
@when_not('zookeeper.ready')
def stop_kafka_waiting_for_zookeeper_ready():
    hookenv.status_set('maintenance', 'zookeeper not ready, stopping kafka')
    kafka = Kafka()
    kafka.close_ports()
    kafka.stop()
    remove_state('kafka.started')
    hookenv.status_set('waiting', 'waiting for zookeeper to become ready')


@when('client.joined', 'zookeeper.ready')
def serve_client(client, zookeeper):
    kafka_port = get_layer_opts().port('kafka')
    client.send_port(kafka_port)
    client.send_zookeepers(zookeeper.zookeepers())
    hookenv.log('Sent Kafka configuration to client')


@hook('logs-storage-attached')
def storage_attach():
    storageids = hookenv.storage_list('logs')
    if not storageids:
        hookenv.status_set('blocked', 'cannot locate attached storage')
        return
    storageid = storageids[0]

    mount = hookenv.storage_get('location', storageid)
    if not mount:
        hookenv.status_set('blocked', 'cannot locate attached storage mount')
        return

    log_dir = os.path.join(mount, "logs")
    unitdata.kv().set('kafka.storage.log_dir', log_dir)
    hookenv.log('Kafka logs storage attached at {}'.format(log_dir))
    # Stop Kafka; removing the kafka.started state will trigger a reconfigure if/when it's ready
    kafka = Kafka()
    kafka.close_ports()
    kafka.stop()
    remove_state('kafka.started')
    hookenv.status_set('waiting', 'reconfiguring to use attached storage')
    set_state('kafka.storage.logs.attached')


@hook('logs-storage-detaching')
def storage_detaching():
    unitdata.kv().unset('kafka.storage.log_dir')
    kafka = Kafka()
    kafka.close_ports()
    kafka.stop()
    remove_state('kafka.started')
    hookenv.status_set('waiting', 'reconfiguring to use temporary storage')
    remove_state('kafka.storage.logs.attached')
