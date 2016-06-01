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

from charms.reactive import when, when_not_all, is_state, set_state, remove_state
from charms.layer.bigtop_hbase import HBase
from charmhelpers.core import hookenv
from charms.reactive.helpers import data_changed
from charms.layer.apache_bigtop_base import get_layer_opts


@when('bigtop.available')
def report_status():
    hadoop_joined = is_state('hadoop.joined')
    hdfs_ready = is_state('hadoop.hdfs.ready')
    zk_joined = is_state('zookeeper.joined')
    zk_ready = is_state('zookeeper.ready')
    hbase_installed = is_state('hbase.installed')
    if not hadoop_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to hadoop plugin')
    elif not hdfs_ready:
        hookenv.status_set('waiting',
                           'waiting for hdfs to become ready')
    elif not zk_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to zookeeper')
    elif not zk_ready:
        hookenv.status_set('waiting',
                           'waiting for zookeeper to become ready')
    elif hbase_installed:
        hookenv.status_set('active',
                           'ready')


@when('bigtop.available', 'zookeeper.ready', 'hadoop.hdfs.ready')
def installing_hbase(zk, hdfs):
    zks = zk.zookeepers()
    if is_state('hbase.installed') and (not data_changed('zks', zks)):
        return

    msg = "configuring hbase" if is_state('hbase.installed') else "installing hbase"
    hookenv.status_set('maintenance', msg)

    hbase = HBase()
    hosts = {}
    nns = hdfs.namenodes()
    hosts['namenode'] = nns[0]
    hbase.configure(hosts, zks)
    hbase.open_ports()
    set_state('hbase.installed')
    hookenv.status_set('active', 'ready')


@when('hbase.installed')
@when_not_all('hadoop.hdfs.ready', 'zookeeper.ready')
def stop_hbase():
    hbase = HBase()
    hbase.close_ports()
    hbase.stop()
    remove_state('hbase.installed')
    report_status()


@when('hbase.installed', 'hbclient.joined')
def serve_client(client):
    config = get_layer_opts()
    master_port = config.port('hbase-master')
    regionserver_port = config.port('hbase-region')
    thrift_port = config.port('hbase-thrift')
    client.send_port(master_port, regionserver_port, thrift_port)
