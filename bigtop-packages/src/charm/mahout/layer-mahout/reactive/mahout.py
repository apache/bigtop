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

from jujubigdata import utils
from charms.reactive import when, when_not, set_state
from charms.layer.apache_bigtop_base import Bigtop
from charmhelpers.core import hookenv


@when('bigtop.available')
@when_not('mahout.installed')
def install_mahout():
    hookenv.status_set('maintenance', 'installing mahout')
    bigtop = Bigtop()
    bigtop.render_site_yaml(
        roles=[
            'mahout-client',
        ],
    )
    bigtop.trigger_puppet()
    with utils.environment_edit_in_place('/etc/environment') as env:
        env['MAHOUT_HOME'] = '/usr/lib/mahout'

    hookenv.status_set('active', 'ready')
    set_state('mahout.installed')
