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

import os
import unittest

import yaml
import amulet


class TestBundle(unittest.TestCase):
    bundle_file = os.path.join(os.path.dirname(__file__), '..', 'bundle.yaml')

    @classmethod
    def setUpClass(cls):
        # classmethod inheritance doesn't work quite right with
        # setUpClass / tearDownClass, so subclasses have to manually call this
        cls.d = amulet.Deployment(series='trusty')
        with open(cls.bundle_file) as f:
            bun = f.read()
        bundle = yaml.safe_load(bun)
        cls.d.load(bundle)
        cls.d.setup(timeout=3600)
        cls.d.sentry.wait_for_messages({'client': 'Ready'}, timeout=3600)
        cls.hdfs = cls.d.sentry['namenode'][0]
        cls.yarn = cls.d.sentry['resourcemanager'][0]
        cls.slave = cls.d.sentry['slave'][0]
        cls.client = cls.d.sentry['client'][0]

    def test_components(self):
        """
        Confirm that all of the required components are up and running.
        """
        hdfs, retcode = self.hdfs.run("pgrep -a java")
        yarn, retcode = self.yarn.run("pgrep -a java")
        slave, retcode = self.slave.run("pgrep -a java")
        client, retcode = self.client.run("pgrep -a java")

        assert 'NameNode' in hdfs, "NameNode not started"
        assert 'NameNode' not in yarn, "NameNode should not be running on resourcemanager"
        assert 'NameNode' not in slave, "NameNode should not be running on slave"

        assert 'ResourceManager' in yarn, "ResourceManager not started"
        assert 'ResourceManager' not in hdfs, "ResourceManager should not be running on namenode"
        assert 'ResourceManager' not in slave, "ResourceManager should not be running on slave"

        assert 'JobHistoryServer' in yarn, "JobHistoryServer not started"
        assert 'JobHistoryServer' not in hdfs, "JobHistoryServer should not be running on namenode"
        assert 'JobHistoryServer' not in slave, "JobHistoryServer should not be running on slave"

        assert 'NodeManager' in slave, "NodeManager not started"
        assert 'NodeManager' not in yarn, "NodeManager should not be running on resourcemanager"
        assert 'NodeManager' not in hdfs, "NodeManager should not be running on namenode"

        assert 'DataNode' in slave, "DataServer not started"
        assert 'DataNode' not in yarn, "DataNode should not be running on resourcemanager"
        assert 'DataNode' not in hdfs, "DataNode should not be running on namenode"

    def test_hdfs(self):
        """Smoke test validates mkdir, ls, chmod, and rm on the hdfs cluster."""
        unit_name = self.hdfs.info['unit_name']
        uuid = self.d.action_do(unit_name, 'smoke-test')
        result = self.d.action_fetch(uuid)
        # hdfs smoke-test sets outcome=success on success
        if (result['outcome'] != "success"):
            error = "HDFS smoke-test failed"
            amulet.raise_status(amulet.FAIL, msg=error)

    def test_yarn(self):
        """Smoke test validates teragen/terasort."""
        unit_name = self.yarn.info['unit_name']
        uuid = self.d.action_do(unit_name, 'smoke-test')
        result = self.d.action_fetch(uuid)
        # yarn smoke-test only returns results on failure; if result is not
        # empty, the test has failed and has a 'log' key
        if result:
            error = "YARN smoke-test failed: %s" % result['log']
            amulet.raise_status(amulet.FAIL, msg=error)


if __name__ == '__main__':
    unittest.main()
