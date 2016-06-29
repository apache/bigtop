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

import time
import os
import signal

from charmhelpers.core import hookenv
from charmhelpers.core import unitdata
from charms import layer
from jujubigdata import utils
from charms.layer.apache_bigtop_base import Bigtop


class Hive(object):
    """
    This class manages Hive.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def install(self):
        '''
        Trigger the Bigtop puppet recipe that handles the Hive service.
        '''
        # Dirs are handled by the bigtop deb. No need to call out to
        # dist_config to do that work.
        self.dist_config.add_users()
        roles = ['hive-client']

        bigtop = Bigtop()
        bigtop.render_site_yaml(roles=roles)
        bigtop.trigger_puppet()

    def initial_hive_config(self):
        '''
        Configure system-wide hive bits and get hive config files set up
        so future config changes can be added where needed.
        '''
        # Configure system-wide bits
        hive_bin = self.dist_config.path('hive') / 'bin'
        with utils.environment_edit_in_place('/etc/environment') as env:
            if hive_bin not in env['PATH']:
                env['PATH'] = ':'.join([env['PATH'], hive_bin])
            env['HIVE_CONF_DIR'] = self.dist_config.path('hive_conf')
            env['HIVE_HOME'] = self.dist_config.path('hive')

        # Copy template to config file so we can adjust config later
        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        if not hive_env.exists():
            (self.dist_config.path('hive_conf') / 'hive-env.sh.template').copy(hive_env)

        # Configure immutable things
        hive_log4j = self.dist_config.path('hive_conf') / 'hive-log4j.properties'
        hive_logs = self.dist_config.path('hive_logs')
        utils.re_edit_in_place(hive_log4j, {
            r'^hive.log.dir.*': 'hive.log.dir={}'.format(hive_logs),
        })
        hive_site = self.dist_config.path('hive_conf') / 'hive-site.xml'
        with utils.xmlpropmap_edit_in_place(hive_site) as props:
            # XXX (kwm): these 4 were needed in 0.12, but it seems ok without them in > 1.0
            # props['hive.exec.local.scratchdir'] = "/tmp/hive"
            # props['hive.downloaded.resources.dir'] = "/tmp/hive_resources"
            # props['hive.querylog.location'] = "/tmp/hive"
            # props['hive.server2.logging.operation.log.location'] = "/tmp/hive"
            props['hive.hwi.war.file'] = "lib/hive-hwi.jar"

    # called during config-changed events
    def configure_hive(self):
        config = hookenv.config()
        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HADOOP_HEAPSIZE *=.*': 'export HADOOP_HEAPSIZE=%s' % config['heap'],
        })

    def configure_remote_db(self, mysql):
        hive_site = self.dist_config.path('hive_conf') / 'hive-site.xml'
        jdbc_url = "jdbc:mysql://{}:{}/{}?createDatabaseIfNotExist=true".format(
            mysql.host(), mysql.port(), mysql.database()
        )
        with utils.xmlpropmap_edit_in_place(hive_site) as props:
            props['javax.jdo.option.ConnectionURL'] = jdbc_url
            props['javax.jdo.option.ConnectionUserName'] = mysql.user()
            props['javax.jdo.option.ConnectionPassword'] = mysql.password()
            props['javax.jdo.option.ConnectionDriverName'] = "com.mysql.jdbc.Driver"

        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HIVE_AUX_JARS_PATH *=.*': 'export HIVE_AUX_JARS_PATH=/usr/share/java/mysql-connector-java.jar',
        })

        # Now that we have db connection info, init our schema (only once)
        remote_db = hookenv.remote_service_name()
        if not unitdata.kv().get('hive.schema.initialized.%s' % remote_db):
            utils.run_as('ubuntu', 'schematool', '-initSchema', '-dbType', 'mysql')
            unitdata.kv().set('hive.schema.initialized.%s' % remote_db, True)

    def configure_local_db(self):
        local_url = 'jdbc:derby:;databaseName=/var/lib/hive/metastore/metastore_db;create=true'
        local_driver = 'org.apache.derby.jdbc.EmbeddedDriver'
        hive_site = self.dist_config.path('hive_conf') / 'hive-site.xml'
        with utils.xmlpropmap_edit_in_place(hive_site) as props:
            props['javax.jdo.option.ConnectionURL'] = local_url
            props['javax.jdo.option.ConnectionUserName'] = 'APP'
            props['javax.jdo.option.ConnectionPassword'] = 'mine'
            props['javax.jdo.option.ConnectionDriverName'] = local_driver

        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HIVE_AUX_JARS_PATH *=.*': '# export HIVE_AUX_JARS_PATH=',
        })

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        self.stop()
        hive_log = self.dist_config.path('hive_logs') / 'HiveServer2.out'
        utils.run_bg_as(
            'root', hive_log, 'hive',
            '--config', self.dist_config.path('hive_conf'),
            '--service', 'hiveserver2')
        time.sleep(5)

    def stop(self):
        hive_pids = utils.jps('HiveServer2')
        for pid in hive_pids:
            os.kill(int(pid), signal.SIGTERM)

    def open_ports(self):
        for port in self.dist_config.exposed_ports('hive'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('hive'):
            hookenv.close_port(port)
