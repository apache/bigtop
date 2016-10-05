# pylint: disable=unused-argument

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

from charms.reactive import when_any, is_state
from charmhelpers.core.hookenv import status_set


@when_any(
    'bigtop.available',
    'apache-bigtop-datanode.pending',
    'apache-bigtop-nodemanager.pending',
    'apache-bigtop-datanode.installed',
    'apache-bigtop-nodemanager.installed',
    'apache-bigtop-datanode.started',
    'apache-bigtop-nodemanager.started',
    'namenode.joined',
    'namenode.ready',
    'resourcemanager.joined',
    'resourcemanager.ready',
)
def update_status():
    hdfs_rel = is_state('namenode.joined')
    yarn_rel = is_state('resourcemanager.joined')
    hdfs_ready = is_state('namenode.ready')
    yarn_ready = is_state('resourcemanager.ready')

    if not (hdfs_rel or yarn_rel):
        status_set('blocked',
                   'missing required namenode and/or resourcemanager relation')
    elif hdfs_rel and not hdfs_ready:
        status_set('waiting', 'waiting for hdfs to become ready')
    elif yarn_rel and not yarn_ready:
        status_set('waiting', 'waiting for yarn to become ready')
    else:
        ready = []
        if hdfs_ready:
            ready.append('datanode')
        if yarn_ready:
            ready.append('nodemanager')
        status_set('active', 'ready ({})'.format(' & '.join(ready)))
