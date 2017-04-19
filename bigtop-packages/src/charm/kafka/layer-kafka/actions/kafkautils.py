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

import re
import sys

from charmhelpers.core import hookenv


def fail(msg):
    hookenv.action_set({'outcome': 'failure'})
    hookenv.action_fail(msg)
    sys.exit()


def get_zookeepers():
    cfg = '/etc/kafka/conf/server.properties'
    print(cfg)
    file = open(cfg, "r")

    for line in file:
        if re.search('^zookeeper.connect=.*', line):
            zks = line.split("=")[1].strip('\n')
            return zks

    return None
