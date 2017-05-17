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

from charmhelpers.core import hookenv
from charms.layer.bigtop_pig import Pig
from charms.reactive import is_state, set_state, when, when_not
from charms.reactive.helpers import data_changed


@when('bigtop.available')
@when_not('pig.installed')
def install_pig():
    hookenv.status_set('maintenance', 'installing pig')
    pig = Pig()
    pig.install_pig()
    pig.initial_pig_config()
    set_state('pig.installed')


@when('pig.installed')
def check_config():
    mode = 'mapreduce' if is_state('hadoop.ready') else 'local'
    if data_changed('pig.mode', mode):
        Pig().update_config(mode)
        hookenv.status_set('active', 'ready (%s)' % mode)
