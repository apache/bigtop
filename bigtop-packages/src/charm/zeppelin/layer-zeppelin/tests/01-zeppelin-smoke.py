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
    Deployment and smoke test for the Apache Bigtop Zeppelin service.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='trusty')
        cls.d.add('zeppelin', 'zeppelin')
        cls.d.add('resourcemanager', 'hadoop-resourcemanager')
        cls.d.add('namenode', 'hadoop-namenode')
        cls.d.add('slave', 'hadoop-slave')
        cls.d.add('plugin', 'hadoop-plugin')

        cls.d.relate('plugin:hadoop-plugin', 'zeppelin:hadoop')
        cls.d.relate('plugin:namenode', 'namenode:namenode')
        cls.d.relate('plugin:resourcemanager', 'resourcemanager:resourcemanager')
        cls.d.relate('slave:namenode', 'namenode:datanode')
        cls.d.relate('slave:resourcemanager', 'resourcemanager:nodemanager')
        cls.d.relate('namenode:namenode', 'resourcemanager:namenode')

        cls.d.setup(timeout=3600)
        cls.d.sentry.wait_for_messages({'zeppelin': 'ready'}, timeout=3600)
        cls.zeppelin = cls.d.sentry['zeppelin'][0]

    def test_zeppelin(self):
        """
        Validate Zeppelin by running the smoke-test action.
        """
        uuid = self.zeppelin.action_do('smoke-test')
        output = self.d.action_fetch(uuid, full_output=True)
        assert "completed" in output['status']


if __name__ == '__main__':
    unittest.main()
