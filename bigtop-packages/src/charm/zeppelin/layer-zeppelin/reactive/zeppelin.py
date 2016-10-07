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

import hashlib

from charms.reactive import when, when_not
from charms.reactive import is_state, set_state, remove_state
from charmhelpers.core import hookenv
from charms.layer.bigtop_zeppelin import Zeppelin
from charms.reactive.helpers import data_changed


@when('zeppelin.installed')
def update_status():
    hadoop_joined = is_state('hadoop.joined')
    hadoop_ready = is_state('hadoop.ready')
    hive_joined = is_state('hive.connected')
    hive_ready = is_state('hive.available')
    spark_joined = is_state('spark.joined')
    spark_ready = is_state('spark.ready')

    waiting_apps = []
    ready_apps = []
    # Check status of the hadoop plugin
    if hadoop_joined and not hadoop_ready:
        waiting_apps.append('hadoop')
    elif hadoop_ready:
        ready_apps.append('hadoop')

    # Check status of Hive
    if hive_joined and not hive_ready:
        waiting_apps.append('hive')
    elif hive_ready:
        ready_apps.append('hive')

    # Check status of Spark
    if spark_joined and not spark_ready:
        waiting_apps.append('spark')
    elif spark_ready:
        ready_apps.append('spark')

    # Set appropriate status based on the apps we checked above
    if waiting_apps:
        hookenv.status_set('waiting',
                           'waiting for: {}'.format(' & '.join(waiting_apps)))
    elif ready_apps:
        hookenv.status_set('active',
                           'ready with: {}'.format(' & '.join(ready_apps)))
    else:
        hookenv.status_set('active', 'ready')


@when('bigtop.available')
@when_not('zeppelin.installed')
def initial_setup():
    hookenv.status_set('maintenance', 'installing zeppelin')
    zeppelin = Zeppelin()
    zeppelin.install()
    zeppelin.setup_etc_env()
    zeppelin.open_ports()
    set_state('zeppelin.installed')
    update_status()


@when('zeppelin.installed')
@when('hadoop.ready')
@when_not('zeppelin.hadoop.configured')
def configure_hadoop(hadoop):
    zeppelin = Zeppelin()
    zeppelin.configure_hadoop()
    zeppelin.register_hadoop_notebooks()
    set_state('zeppelin.hadoop.configured')


@when('zeppelin.installed')
@when_not('hadoop.ready')
@when('zeppelin.hadoop.configured')
def unconfigure_hadoop():
    zeppelin = Zeppelin()
    zeppelin.remove_hadoop_notebooks()
    remove_state('zeppelin.hadoop.configured')


@when('zeppelin.installed', 'hive.ready')
def configure_hive(hive):
    hive_ip = hive.get_private_ip()
    hive_port = hive.get_port()
    hive_url = 'jdbc:hive2://%s:%s' % (hive_ip, hive_port)
    if data_changed('hive.connect', hive_url):
        hookenv.status_set('maintenance', 'configuring hive')
        zeppelin = Zeppelin()
        zeppelin.configure_hive(hive_url)
        set_state('zeppelin.hive.configured')
        update_status()


@when('zeppelin.installed', 'zeppelin.hive.configured')
@when_not('hive.ready')
def unconfigure_hive():
    hookenv.status_set('maintenance', 'removing hive relation')
    zeppelin = Zeppelin()
    zeppelin.configure_hive('jdbc:hive2://:')
    remove_state('zeppelin.hive.configured')
    update_status()


@when('zeppelin.installed', 'spark.ready')
def configure_spark(spark):
    master_url = spark.get_master_url()
    if data_changed('spark.master', master_url):
        hookenv.status_set('maintenance', 'configuring spark')
        zeppelin = Zeppelin()
        zeppelin.configure_spark(master_url)
        set_state('zeppelin.spark.configured')
        update_status()


@when('zeppelin.installed', 'zeppelin.spark.configured')
@when_not('spark.ready')
def unconfigure_spark():
    hookenv.status_set('maintenance', 'removing spark relation')
    zeppelin = Zeppelin()
    # Yarn / Hadoop may not actually be available, but that is the default
    # value and nothing else would reasonably work here either without Spark.
    zeppelin.configure_spark('yarn-client')
    data_changed('spark.master', 'yarn-client')  # ensure updated if re-added
    remove_state('zeppelin.spark.configured')
    update_status()


@when('zeppelin.started', 'client.notebook.registered')
def register_notebook(client):
    zeppelin = Zeppelin()
    for notebook in client.unregistered_notebooks():
        notebook_md5 = hashlib.md5(notebook.encode('utf8')).hexdigest()
        if zeppelin.register_notebook(notebook_md5, notebook):
            client.accept_notebook(notebook)
        else:
            client.reject_notebook(notebook)


@when('zeppelin.started', 'client.notebook.removed')
def remove_notebook(client):
    zeppelin = Zeppelin()
    for notebook in client.unremoved_notebooks():
        notebook_md5 = hashlib.md5(notebook.encode('utf8')).hexdigest()
        zeppelin.remove_notebook(notebook_md5)
        client.remove_notebook(notebook)
