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


class Hive(object):
    '''This class manages Hive.'''
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def install(self, hbase=None, zk_units=None):
        '''
        Trigger the Bigtop puppet recipe that handles the Hive service.
        '''
        # Dirs are handled by the bigtop deb. No need to call out to
        # dist_config to do that. We do want 'ubuntu' in the hive group though.
        self.dist_config.add_users()

        # Prep config
        roles = ['hive-client', 'hive-metastore', 'hive-server2']
        metastore = "thrift://{}:9083".format(hookenv.unit_private_ip())

        if hbase:
            roles.append('hive-hbase')
            hb_connect = "{}:{}".format(hbase['host'], hbase['master_port'])
            zk_hbase_connect = hbase['zk_connect']
        else:
            hb_connect = ""
            zk_hbase_connect = ""

        if zk_units:
            hive_support_concurrency = True
            zk_hive_connect = self.get_zk_connect(zk_units)
        else:
            hive_support_concurrency = False
            zk_hive_connect = ""

        override = {
            'hadoop_hive::common_config::hbase_master': hb_connect,
            'hadoop_hive::common_config::hbase_zookeeper_quorum':
                zk_hbase_connect,
            'hadoop_hive::common_config::hive_zookeeper_quorum':
                zk_hive_connect,
            'hadoop_hive::common_config::hive_support_concurrency':
                hive_support_concurrency,
            'hadoop_hive::common_config::metastore_uris': metastore,
            'hadoop_hive::common_config::server2_thrift_port':
                self.dist_config.port('hive-thrift'),
            'hadoop_hive::common_config::server2_thrift_http_port':
                self.dist_config.port('hive-thrift-web'),
        }

        bigtop = Bigtop()
        bigtop.render_site_yaml(roles=roles, overrides=override)
        bigtop.trigger_puppet()

        # Bigtop doesn't create a hive-env.sh, but we need it for heap config
        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        if not hive_env.exists():
            (self.dist_config.path('hive_conf') / 'hive-env.sh.template').copy(
                hive_env)

    def get_zk_connect(self, zk_units):
        zks = []
        for unit in zk_units:
            ip = utils.resolve_private_address(unit['host'])
            zks.append(ip)
        zks.sort()
        return ",".join(zks)

    def configure_hive(self):
        '''
        Called during config-changed events
        '''
        config = hookenv.config()
        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HADOOP_HEAPSIZE *=.*':
                'export HADOOP_HEAPSIZE=%s' % config['heap'],
        })

    def configure_remote_db(self, mysql):
        hive_site = self.dist_config.path('hive_conf') / 'hive-site.xml'
        jdbc_url = \
            "jdbc:mysql://{}:{}/{}?createDatabaseIfNotExist=true".format(
                mysql.host(), mysql.port(), mysql.database()
            )
        with utils.xmlpropmap_edit_in_place(hive_site) as props:
            props['javax.jdo.option.ConnectionURL'] = jdbc_url
            props['javax.jdo.option.ConnectionUserName'] = mysql.user()
            props['javax.jdo.option.ConnectionPassword'] = mysql.password()
            props['javax.jdo.option.ConnectionDriverName'] = \
                "com.mysql.jdbc.Driver"

        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HIVE_AUX_JARS_PATH *=.*':
            ('export HIVE_AUX_JARS_PATH='
             '/usr/share/java/mysql-connector-java.jar'),
        })

        # Now that we have db connection info, init our schema (only once)
        remote_db = hookenv.remote_service_name()
        if not unitdata.kv().get('hive.schema.initialized.%s' % remote_db):
            tool_path = "{}/bin/schematool".format(
                self.dist_config.path('hive'))
            utils.run_as(
                'ubuntu', tool_path, '-initSchema', '-dbType', 'mysql')
            unitdata.kv().set('hive.schema.initialized.%s' % remote_db, True)
            unitdata.kv().flush(True)

    def configure_local_db(self):
        local_url = \
            ('jdbc:derby:;databaseName='
             '/var/lib/hive/metastore/metastore_db;create=true')
        local_driver = 'org.apache.derby.jdbc.EmbeddedDriver'
        hive_site = self.dist_config.path('hive_conf') / 'hive-site.xml'
        with utils.xmlpropmap_edit_in_place(hive_site) as props:
            props['javax.jdo.option.ConnectionURL'] = local_url
            props['javax.jdo.option.ConnectionUserName'] = 'APP'
            props['javax.jdo.option.ConnectionPassword'] = 'mine'
            props['javax.jdo.option.ConnectionDriverName'] = local_driver

        hive_env = self.dist_config.path('hive_conf') / 'hive-env.sh'
        utils.re_edit_in_place(hive_env, {
            r'.*export HIVE_AUX_JARS_PATH *=.*':
            '# export HIVE_AUX_JARS_PATH=',
        })

    def restart(self):
        self.stop()
        self.start()

    def start(self):
        # order is important; metastore must start first.
        hookenv.log('Starting Hive services')
        host.service_start('hive-metastore')
        host.service_start('hive-server2')
        hookenv.log('Hive services have been started')

    def stop(self):
        # order is important; metastore must stop last.
        hookenv.log('Stopping Hive services')
        host.service_stop('hive-server2')
        host.service_stop('hive-metastore')
        hookenv.log('Hive services have been stopped')

    def open_ports(self):
        for port in self.dist_config.exposed_ports('hive'):
            hookenv.open_port(port)

    def close_ports(self):
        for port in self.dist_config.exposed_ports('hive'):
            hookenv.close_port(port)
