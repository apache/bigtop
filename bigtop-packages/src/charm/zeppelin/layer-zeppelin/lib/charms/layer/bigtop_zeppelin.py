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
import json
import time
import requests
import socket

from path import Path
from urllib.parse import urljoin

from charmhelpers.core import hookenv, host, unitdata
from charms import layer
from charms.layer.apache_bigtop_base import Bigtop
from charms.reactive import is_state
from jujubigdata import utils


class Zeppelin(object):
    """
    This class manages Zeppelin.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def _add_override(self, name, value):
        unitdata.kv().update({
            name: value,
        }, prefix='zeppelin.bigtop.overrides.')

    def install(self):
        '''
        Perform initial one-time setup and trigger puppet.
        '''
        # Dirs are handled by the bigtop deb, so no need to call out to
        # dist_config to do that work.  However, we want to adjust the
        # groups for the `ubuntu` user for better interaction with Juju.
        self.dist_config.add_users()

        # Set ports based on layer.yaml options
        self._add_override('zeppelin::server::server_port',
                           self.dist_config.port('zeppelin'))
        self._add_override('zeppelin::server::web_socket_port',
                           self.dist_config.port('zeppelin_websocket'))

        # Default spark to local mode on initial install. This will be
        # reconfigured if/when hadoop or spark relations are made.
        local_master = 'local[*]'
        self._add_override('zeppelin::server::spark_master_url', local_master)

        # The spark-client role expects hdfs by default. Since we want to
        # keep Hadoop optional, ensure we remove hadoopy bits from our
        # local spark config. This has no effect if/when a remote spark joins,
        # and since there is no spark history server running, the event dirs
        # are not important -- they just need not be 'hdfs:///blah'.
        events_log_dir = 'file:///tmp'
        self._add_override('spark::common::master_url', local_master)
        self._add_override('spark::common::event_log_dir', events_log_dir)
        self._add_override('spark::common::history_log_dir', events_log_dir)

        self.trigger_bigtop()

    def trigger_bigtop(self):
        '''
        Trigger the Bigtop puppet recipe that handles the Zeppelin service.
        '''
        bigtop = Bigtop()
        overrides = unitdata.kv().getrange('zeppelin.bigtop.overrides.',
                                           strip=True)

        # The zep deb depends on spark-core which unfortunately brings in
        # most of hadoop. Include appropriate roles here to ensure these
        # packages are configured in the same way as our other Bigtop
        # software deployed with puppet.
        bigtop.render_site_yaml(
            roles=[
                'spark-client',
                'spark-yarn-slave',
                'zeppelin-server',
            ],
            overrides=overrides,
        )

        # NB: during an upgrade, we configure the site.yaml, but do not
        # trigger puppet. The user must do that with the 'reinstall' action.
        if unitdata.kv().get('zeppelin.version.repo', False):
            hookenv.log("An upgrade is available and the site.yaml has been "
                        "configured. Run the 'reinstall' action to continue.",
                        level=hookenv.INFO)
        else:
            ####################################################################
            # BUG: BIGTOP-2742
            # Default zeppelin init script looks for the literal '$(hostname)'
            # string. Symlink it so it exists before the apt install from puppet
            # tries to start the service.
            import subprocess
            host = subprocess.check_output(['hostname']).decode('utf8').strip()
            zepp_pid = '/var/run/zeppelin/zeppelin-zeppelin-{}.pid'.format(host)
            utils.run_as('root', 'mkdir', '-p', '/var/run/zeppelin')
            utils.run_as('root', 'ln', '-sf',
                         zepp_pid,
                         '/var/run/zeppelin/zeppelin-zeppelin-$(hostname).pid')
            ####################################################################

            bigtop.trigger_puppet()
            self.wait_for_api(30)

            ####################################################################
            # BUG: BIGTOP-2742
            # Puppet apply will call systemctl daemon-reload, which removes the
            # symlink we just created. Now that the bits are on disk, update the
            # init script $(hostname) that caused this mess to begin with.
            zepp_init_script = '/etc/init.d/zeppelin'
            utils.re_edit_in_place(zepp_init_script, {
                r'^# pidfile.*': '# pidfile: {}'.format(zepp_pid),
            })
            utils.run_as('root', 'systemctl', 'daemon-reload')
            self.restart()
            self.wait_for_api(30)
            ####################################################################

    def reconfigure_zeppelin(self):
        '''
        Configure zeppelin based on current environment
        '''
        raise NotImplementedError()
        # NB (kwm): this method is not currently called because Bigtop spark
        # doesn't expose these settings. Leaving this here just in case
        # we update the bigtop charms to provide these bits in the future.
        etc_env = utils.read_etc_env()
        hadoop_extra_classpath = etc_env.get('HADOOP_EXTRA_CLASSPATH', '')
        spark_driver_mem = etc_env.get('SPARK_DRIVER_MEMORY', '1g')
        spark_exe_mode = os.environ.get('MASTER', 'yarn-client')
        spark_executor_mem = etc_env.get('SPARK_EXECUTOR_MEMORY', '1g')
        zeppelin_env = self.dist_config.path('zeppelin_conf') / 'zeppelin-env.sh'
        with open(zeppelin_env, "a") as f:
            f.write('export ZEPPELIN_CLASSPATH_OVERRIDES={}\n'.format(hadoop_extra_classpath))
            f.write('export ZEPPELIN_JAVA_OPTS="-Dspark.driver.memory={} -Dspark.executor.memory={}"\n'.format(
                spark_driver_mem,
                spark_executor_mem))
            f.write('export SPARK_SUBMIT_OPTIONS="--driver-memory {} --executor-memory {}"\n'.format(
                spark_driver_mem,
                spark_executor_mem))
            f.write('export MASTER={}\n'.format(spark_exe_mode))

    def configure_hadoop(self):
        # create hdfs storage space
        utils.run_as('hdfs', 'hdfs', 'dfs', '-mkdir', '-p', '/user/zeppelin')
        utils.run_as('hdfs', 'hdfs', 'dfs', '-chown', 'zeppelin', '/user/zeppelin')

        # If spark is ready, let configure_spark() trigger bigtop. Otherwise,
        # put our spark in yarn-client mode since hadoop is here.
        if not is_state('spark.ready'):
            self._add_override('spark::common::master_url', 'yarn-client')
            self._add_override('zeppelin::server::spark_master_url', 'yarn-client')
            self.trigger_bigtop()

    def configure_spark(self, master_url):
        '''
        Configure the zeppelin spark interpreter
        '''
        # TODO: Add config for Spark driver and executor memory overrides
        self._add_override('spark::common::master_url', master_url)
        self._add_override('zeppelin::server::spark_master_url', master_url)
        self.trigger_bigtop()

    def configure_hive(self, hive_url):
        '''
        Configure the zeppelin hive interpreter
        '''
        self._add_override('zeppelin::server::hiveserver2_url', hive_url)
        self.trigger_bigtop()

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        host.service_start('zeppelin')

    def check_connect(self, addr, port):
        try:
            with socket.create_connection((addr, port), timeout=10):
                return True
        except OSError:
            return False

    def wait_for_api(self, timeout):
        start = time.time()
        while time.time() - start < timeout:
            if self.check_connect('localhost', self.dist_config.port('zeppelin')):
                return True
            time.sleep(2)
        raise utils.TimeoutError('Timed-out waiting for connection to Zeppelin')

    def stop(self):
        host.service_stop('zeppelin')

    def open_ports(self):
        for port in self.dist_config.exposed_ports('zeppelin'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('zeppelin'):
            hookenv.close_port(port)

    def register_notebook(self, local_id, contents):
        api = ZeppelinAPI()
        kv = unitdata.kv()
        notebook_ids = kv.get('zeppelin.notebooks.ids', {})
        if local_id in notebook_ids:
            hookenv.log('Replacing notebook {} registered as {}'.format(
                local_id, notebook_ids[local_id]))
            api.delete_notebook(notebook_ids[local_id])
        zeppelin_id = api.import_notebook(contents)
        if zeppelin_id:
            notebook_ids[local_id] = zeppelin_id
            hookenv.log('Registered notebook {} as {}'.format(local_id,
                                                              zeppelin_id))
            return True
        else:
            hookenv.log('Unable to register notebook: {}'.format(local_id),
                        hookenv.ERROR)
            return False
        kv.set('zeppelin.notebooks.ids', notebook_ids)

    def remove_notebook(self, local_id):
        api = ZeppelinAPI()
        kv = unitdata.kv()
        notebook_ids = kv.get('zeppelin.notebooks.ids', {})
        if local_id in notebook_ids:
            api.delete_notebook(notebook_ids[local_id])
            del notebook_ids[local_id]
        else:
            hookenv.log('Notebook not registered: {}'.format(local_id),
                        hookenv.ERROR)
        kv.set('zeppelin.notebooks.ids', notebook_ids)

    def register_hadoop_notebooks(self):
        for notebook in ('hdfs-tutorial', 'flume-tutorial'):
            contents = (Path('resources') / notebook / 'note.json').text()
            self.register_notebook(notebook, contents)

    def remove_hadoop_notebooks(self):
        for notebook in ('hdfs-tutorial', 'flume-tutorial'):
            self.remove_notebook(notebook)


class ZeppelinAPI(object):
    """
    Helper for interacting with the Appache Zeppelin REST API.
    """
    def _url(self, *parts):
        dc = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))
        url = 'http://localhost:{}/api/'.format(dc.port('zeppelin'))
        for part in parts:
            url = urljoin(url, part)
        return url

    def import_notebook(self, contents):
        response = requests.post(self._url('notebook'), data=contents)
        if response.status_code != 201:
            return None
        return response.json()['body']

    def delete_notebook(self, notebook_id):
        requests.delete(self._url('notebook/', notebook_id))

    def modify_interpreter(self, interpreter_name, properties):
        response = requests.get(self._url('interpreter/', 'setting'))
        try:
            body = response.json()['body']
        except json.JSONDecodeError:
            hookenv.log('Invalid response from API server: {} {}'.format(
                response, response.text), hookenv.ERROR)
            raise
        for interpreter_data in body:
            if interpreter_data['name'] == interpreter_name:
                break
        else:
            raise ValueError('Interpreter not found: {}'.format(
                interpreter_name))
        interpreter_data['properties'].update(properties)
        response = requests.put(self._url('interpreter/', 'setting/',
                                          interpreter_data['id']),
                                data=json.dumps(interpreter_data))
        if response.status_code != 200:
            raise ValueError('Unable to update interpreter: {}'.format(
                response.text))
