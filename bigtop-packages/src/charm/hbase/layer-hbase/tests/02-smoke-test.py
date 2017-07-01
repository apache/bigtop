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

import amulet
import re
import unittest


class TestDeploy(unittest.TestCase):
    """
    HDFS/HBase deployment and smoke test for the Apache Bigtop HBase service.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('hbase')
        cls.d.add('namenode', charm='hadoop-namenode')
        cls.d.add('plugin', charm='hadoop-plugin')
        cls.d.add('slave', charm='hadoop-slave')
        cls.d.add('zookeeper')

        cls.d.relate('hbase:hadoop', 'plugin:hadoop-plugin')
        cls.d.relate('hbase:zookeeper', 'zookeeper:zookeeper')
        cls.d.relate('plugin:namenode', 'namenode:namenode')
        cls.d.relate('slave:namenode', 'namenode:datanode')

        cls.d.setup(timeout=3600)
        cls.d.sentry.wait_for_messages({'hbase': re.compile('ready')}, timeout=3600)
        cls.hbase = cls.d.sentry['hbase'][0]

    def test_hbase(self):
        """
        Validate HBase by running the smoke-test action.
        """
        uuid = self.hbase.run_action('smoke-test')
        result = self.d.action_fetch(uuid, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('HBase smoke-test failed: %s' % result)


if __name__ == '__main__':
    unittest.main()
