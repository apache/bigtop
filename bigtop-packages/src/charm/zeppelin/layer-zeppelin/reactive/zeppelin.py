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

from charms.reactive import is_state, remove_state, set_state, when, when_not
from charmhelpers.core import hookenv, unitdata
from charms.layer.apache_bigtop_base import Bigtop, get_package_version
from charms.layer.bigtop_zeppelin import Zeppelin
from charms.reactive.helpers import data_changed


@when('zeppelin.installed')
def update_status():
    hadoop_joined = is_state('hadoop.joined')
    hadoop_ready = is_state('hadoop.ready')
    hive_joined = is_state('hive.joined')
    hive_ready = is_state('hive.ready')
    spark_joined = is_state('spark.joined')
    spark_ready = is_state('spark.ready')
    spark_blocked = is_state('spark.master.unusable')

    # handle blockers first; then report what's ready/waiting
    if spark_blocked:
        hookenv.status_set('blocked',
                           'remote spark must be in standalone mode')
    else:
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

        # Set appropriate status
        repo_ver = unitdata.kv().get('zeppelin.version.repo', False)
        if repo_ver:
            # Pending upgrade takes precedent over other status messages
            msg = "install version {} with the 'reinstall' action".format(repo_ver)
            hookenv.status_set('active', msg)
        elif waiting_apps:
            # Waiting takes precedent over active status messages
            msg = "waiting for: {}".format(' & '.join(waiting_apps))
            hookenv.status_set('waiting', msg)
        elif ready_apps:
            msg = "ready with: {}".format(' & '.join(ready_apps))
            hookenv.status_set('active', msg)
        else:
            hookenv.status_set('active', 'ready')


@when('bigtop.available')
@when_not('zeppelin.installed')
def initial_setup():
    hookenv.status_set('maintenance', 'installing zeppelin')
    zeppelin = Zeppelin()
    zeppelin.install()
    zeppelin.open_ports()
    set_state('zeppelin.installed')
    update_status()
    # set app version string for juju status output
    zeppelin_version = get_package_version('zeppelin') or 'unknown'
    hookenv.application_version_set(zeppelin_version)


@when('zeppelin.installed', 'bigtop.version.changed')
def check_repo_version():
    """
    Configure a bigtop site.yaml if a new version of zeppelin is available.

    This method will set unitdata if a different version of zeppelin is
    available in the newly configured bigtop repo. This unitdata allows us to
    configure site.yaml while gating the actual puppet apply. The user must do
    the puppet apply by calling the 'reinstall' action.
    """
    repo_ver = Bigtop().check_bigtop_repo_package('zeppelin')
    if repo_ver:
        unitdata.kv().set('zeppelin.version.repo', repo_ver)
        unitdata.kv().flush(True)
        zeppelin = Zeppelin()
        zeppelin.trigger_bigtop()
    else:
        unitdata.kv().unset('zeppelin.version.repo')
    update_status()


@when('zeppelin.installed', 'hadoop.ready')
@when_not('zeppelin.hadoop.configured')
def configure_hadoop(hadoop):
    zeppelin = Zeppelin()
    zeppelin.configure_hadoop()
    zeppelin.register_hadoop_notebooks()
    set_state('zeppelin.hadoop.configured')


@when('zeppelin.installed', 'zeppelin.hadoop.configured')
@when_not('hadoop.ready')
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
    '''
    Configure Zeppelin to use remote Spark resources.
    '''
    # NB: Use the master_url string if it already starts with spark://.
    # Otherwise, it means the remote spark is in local or yarn mode -- that's
    # bad because using 'local' or 'yarn' here would cause zepp's spark-submit
    # to use the builtin spark, hence ignoring the remote spark. In this case,
    # set a state so we can inform the user that the remote spark is unusable.
    master_url = spark.get_master_url()

    if master_url.startswith('spark'):
        remove_state('spark.master.unusable')
        # Only (re)configure if our master url has changed.
        if data_changed('spark.master', master_url):
            hookenv.status_set('maintenance', 'configuring spark')
            zeppelin = Zeppelin()
            zeppelin.configure_spark(master_url)
            set_state('zeppelin.spark.configured')
    else:
        remove_state('zeppelin.spark.configured')
        set_state('spark.master.unusable')
    update_status()


@when('zeppelin.installed', 'zeppelin.spark.configured')
@when_not('spark.ready')
def unconfigure_spark():
    '''
    Remove remote Spark; reconfigure Zeppelin to use embedded Spark.
    '''
    hookenv.status_set('maintenance', 'removing spark relation')
    zeppelin = Zeppelin()

    # Zepp includes the spark-client role, so reconfigure our built-in spark
    # if our related spark has gone away.
    if is_state('zeppelin.hadoop.configured'):
        local_master = 'yarn-client'
    else:
        local_master = 'local[*]'
    zeppelin.configure_spark(local_master)
    data_changed('spark.master', local_master)  # ensure updated if re-added
    remove_state('zeppelin.spark.configured')
    update_status()


@when('zeppelin.installed', 'client.notebook.registered')
def register_notebook(client):
    zeppelin = Zeppelin()
    for notebook in client.unregistered_notebooks():
        notebook_md5 = hashlib.md5(notebook.encode('utf8')).hexdigest()
        if zeppelin.register_notebook(notebook_md5, notebook):
            client.accept_notebook(notebook)
        else:
            client.reject_notebook(notebook)


@when('zeppelin.installed', 'client.notebook.removed')
def remove_notebook(client):
    zeppelin = Zeppelin()
    for notebook in client.unremoved_notebooks():
        notebook_md5 = hashlib.md5(notebook.encode('utf8')).hexdigest()
        zeppelin.remove_notebook(notebook_md5)
        client.remove_notebook(notebook)
