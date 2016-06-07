#!/usr/bin/env python3

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

import unittest
import amulet


class TestDeploy(unittest.TestCase):
    """
    Deployment and smoke test for Apache Mahout.
    """
    def setUp(self):
        self.d = amulet.Deployment(series='trusty')
        self.d.add('mahout', 'mahout')
        self.d.add('client', 'hadoop-client')
        self.d.add('resourcemgr', 'hadoop-resourcemanager')
        self.d.add('namenode', 'hadoop-namenode')
        self.d.add('slave', 'hadoop-slave')
        self.d.add('plugin', 'hadoop-plugin')
        self.d.add('openjdk', 'openjdk')

        self.d.configure('openjdk', {'java-type': 'jdk',
                                     'java-major': '8'})

        self.d.relate('plugin:hadoop-plugin', 'client:hadoop')
        self.d.relate('plugin:namenode', 'namenode:namenode')
        self.d.relate('plugin:resourcemanager', 'resourcemgr:resourcemanager')
        self.d.relate('slave:namenode', 'namenode:datanode')
        self.d.relate('slave:resourcemanager', 'resourcemgr:nodemanager')
        self.d.relate('namenode:namenode', 'resourcemgr:namenode')
        self.d.relate('mahout:mahout', 'client:mahout')

        self.d.relate('plugin:java', 'openjdk:java')
        self.d.relate('namenode:java', 'openjdk:java')
        self.d.relate('slave:java', 'openjdk:java')
        self.d.relate('resourcemgr:java', 'openjdk:java')
        self.d.relate('mahout:java', 'openjdk:java')
        self.d.relate('client:java', 'openjdk:java')

        self.d.setup(timeout=1800)
        self.d.sentry.wait_for_messages({"mahout": "ready"})
        self.mahout = self.d.sentry['mahout'][0]

    def test_mahout(self):
        smk_uuid = self.mahout.action_do("smoke-test")
        output = self.d.action_fetch(smk_uuid, full_output=True)
        assert "completed" in output['status']


if __name__ == '__main__':
    unittest.main()
