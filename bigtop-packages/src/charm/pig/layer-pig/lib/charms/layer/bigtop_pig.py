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

from subprocess import CalledProcessError, check_output

from charms.layer.apache_bigtop_base import Bigtop
from charms import layer
from charmhelpers.core import hookenv
from jujubigdata import utils


class Pig(object):
    """
    This class manages Pig.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('apache-bigtop-base'))

    def install_pig(self):
        '''
        Trigger the Bigtop puppet recipe that handles the Pig service.
        '''
        # Dirs are handled by the bigtop deb. No need to call out to
        # dist_config to do that work.
        roles = ['pig-client']

        bigtop = Bigtop()
        bigtop.render_site_yaml(roles=roles)
        bigtop.trigger_puppet()

        # Set app version for juju status output; pig --version looks like:
        #   Apache Pig version 0.15.0 (r: unknown)
        #   compiled Feb 06 2016, 23:00:40
        try:
            pig_out = check_output(['pig', '-x', 'local', '--version']).decode()
        except CalledProcessError as e:
            pig_out = e.output
        lines = pig_out.splitlines()
        parts = lines[0].split() if lines else []
        if len(parts) < 4:
            hookenv.log('Error getting Pig version: {}'.format(pig_out),
                        hookenv.ERROR)
            pig_ver = ''
        else:
            pig_ver = parts[3]
        hookenv.application_version_set(pig_ver)

    def initial_pig_config(self):
        '''
        Configure system-wide pig bits.
        '''
        pig_bin = self.dist_config.path('pig') / 'bin'
        with utils.environment_edit_in_place('/etc/environment') as env:
            if pig_bin not in env['PATH']:
                env['PATH'] = ':'.join([env['PATH'], pig_bin])
            env['PIG_CONF_DIR'] = self.dist_config.path('pig_conf')
            env['PIG_HOME'] = self.dist_config.path('pig')
            env['HADOOP_CONF_DIR'] = self.dist_config.path('hadoop_conf')

    def update_config(self, mode):
        """
        Configure Pig with the correct classpath.  If Hadoop is available, use
        HADOOP_CONF_DIR, otherwise use PIG_HOME.
        """
        with utils.environment_edit_in_place('/etc/environment') as env:
            key = 'HADOOP_CONF_DIR' if mode == 'mapreduce' else 'PIG_HOME'
            env['PIG_CLASSPATH'] = env[key]
