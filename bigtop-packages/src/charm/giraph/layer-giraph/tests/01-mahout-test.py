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
    Deployment and smoke test for Apache Bigtop Mahout.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('mahout', 'cs:xenial/mahout')
        cls.d.add('client', 'cs:xenial/hadoop-client')
        cls.d.add('resourcemanager', 'cs:xenial/hadoop-resourcemanager')
        cls.d.add('namenode', 'cs:xenial/hadoop-namenode')
        cls.d.add('slave', 'cs:xenial/hadoop-slave')
        cls.d.add('plugin', 'cs:xenial/hadoop-plugin')

        cls.d.relate('plugin:hadoop-plugin', 'client:hadoop')
        cls.d.relate('plugin:namenode', 'namenode:namenode')
        cls.d.relate('plugin:resourcemanager', 'resourcemanager:resourcemanager')
        cls.d.relate('slave:namenode', 'namenode:datanode')
        cls.d.relate('slave:resourcemanager', 'resourcemanager:nodemanager')
        cls.d.relate('namenode:namenode', 'resourcemanager:namenode')
        cls.d.relate('mahout:mahout', 'client:mahout')

        cls.d.setup(timeout=3600)
        cls.d.sentry.wait_for_messages({"mahout": "ready"}, timeout=3600)
        cls.mahout = cls.d.sentry['mahout'][0]

    def test_mahout(self):
        """
        Validate Mahout by running the smoke-test action.
        """
        uuid = self.mahout.run_action('smoke-test')
        result = self.d.action_fetch(uuid, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('Mahout smoke-test failed: %s' % result)


if __name__ == '__main__':
    unittest.main()
