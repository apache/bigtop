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

from path import Path
from jujubigdata import utils
from charmhelpers.core import hookenv, host
from charms import layer
from charms.layer.apache_bigtop_base import Bigtop


class Zeppelin(object):
    """
    This class manages Zeppelin.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def install(self):
        '''
        Trigger the Bigtop puppet recipe that handles the Zepplin service.
        '''
        # Dirs are handled by the bigtop deb. No need to call out to
        # dist_config to do that work.
        self.dist_config.add_users()
        roles = ['zeppelin-server']

        bigtop = Bigtop()
        bigtop.render_site_yaml(roles=roles)
        bigtop.trigger_puppet()

    def initial_zeppelin_config(self):
        '''
        Configure system-wide hive bits and get zeppelin config files set up
        so future config changes can be added where needed.
        '''
        # Configure system-wide bits
        zeppelin_bin = self.dist_config.path('zeppelin') / 'bin'
        zeppelin_conf = self.dist_config.path('zeppelin_conf')
        with utils.environment_edit_in_place('/etc/environment') as env:
            if zeppelin_bin not in env['PATH']:
                env['PATH'] = ':'.join([env['PATH'], zeppelin_bin])
            env['ZEPPELIN_CONF_DIR'] = zeppelin_conf

        # Copy templates to config files if they don't exist
        zeppelin_env = zeppelin_conf / 'zeppelin-env.sh'
        if not zeppelin_env.exists():
            (zeppelin_conf / 'zeppelin-env.sh.template').copy(zeppelin_env)

        # java (esp 8) needs the following mem settings.
        mem_string = 'export ZEPPELIN_MEM="-Xms128m -Xmx1024m -XX:MaxPermSize=512m"'
        utils.re_edit_in_place(zeppelin_env, {
            r'.*export ZEPPELIN_MEM.*': mem_string,
        }, append_non_matches=True)

        # User needs write access to zepp's conf to write interpreter.json
        # on notebook binding. chown the whole conf dir, though we could probably
        # touch that file and chown it, leaving the rest owned as root:root.
        # TODO: weigh implications of have zepp's conf dir owned by non-root.
        host.chownr(path=zeppelin_conf, owner="zeppelin", group="zeppelin")

        # create hdfs storage space
        utils.run_as('hdfs', 'hdfs', 'dfs', '-mkdir', '-p', '/user/zeppelin')
        utils.run_as('hdfs', 'hdfs', 'dfs', '-chown', 'zeppelin', '/user/zeppelin')

    def copy_tutorial(self, tutorial_name):
        notebook_dir = self.dist_config.path('zeppelin_notebooks')
        tutorial_target = Path('%s/%s' % (notebook_dir, tutorial_name))
        tutorial_target.rmtree_p()

        tutorial_source = Path('resources/{}'.format(tutorial_name))
        tutorial_source.copytree('%s' % tutorial_target)

        # make sure the notebook dir contents are owned by our user
        host.chownr(path=notebook_dir, owner="zeppelin", group="zeppelin")

    def reconfigure_zeppelin(self):
        '''
        Configure zeppelin based on current environment
        '''
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

    def configure_hive(self, hive=None):
        '''
        Configure the zeppelin hive interpreter
        '''
        if hive:
            hive_ip = hive.get_private_ip()
            hive_port = hive.get_port()
            hive_connect = 'jdbc:hive2://%s:%s' % (hive_ip, hive_port)
        else:
            hive_connect = 'jdbc:hive2://:'

        interpreter_json = self.dist_config.path('zeppelin_conf') / 'interpreter.json'
        utils.re_edit_in_place(interpreter_json, {
            r'jdbc:hive2:.*"': '%s"' % hive_connect,
        })

    def configure_spark(self, spark_connection_url=None):
        '''
        Configure the zeppelin spark interpreter
        '''
        if spark_connection_url:
            spark_url = spark_connection_url
        else:
            spark_url = 'yarn-client'

        zeppelin_env = self.dist_config.path('zeppelin_conf') / 'zeppelin-env.sh'
        utils.re_edit_in_place(zeppelin_env, {
            r'.*export MASTER.*': 'export MASTER=%s' % spark_url,
        })

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        host.service_start('zeppelin')

    def stop(self):
        host.service_stop('zeppelin')

    def open_ports(self):
        for port in self.dist_config.exposed_ports('zeppelin'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('zeppelin'):
            hookenv.close_port(port)
