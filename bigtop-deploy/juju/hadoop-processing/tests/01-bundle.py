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
import os
import re
import unittest
import yaml


class TestBundle(unittest.TestCase):
    bundle_file = os.path.join(os.path.dirname(__file__), '..', 'bundle.yaml')

    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        with open(cls.bundle_file) as f:
            bun = f.read()
        bundle = yaml.safe_load(bun)

        # NB: strip machine ('to') placement. We don't seem to be guaranteed
        # the same machine numbering after the initial bundletester deployment,
        # so we might fail when redeploying --to a specific machine to run
        # these bundle tests. This is ok because all charms in this bundle are
        # using 'reset: false', so we'll already have our deployment just the
        # way we want it by the time this test runs. This was originally
        # raised as:
        #  https://github.com/juju/amulet/issues/148
        for service, service_config in bundle['services'].items():
            if 'to' in service_config:
                del service_config['to']

        cls.d.load(bundle)
        cls.d.setup(timeout=3600)

        # we need units reporting ready before we attempt our smoke tests
        cls.d.sentry.wait_for_messages({'client': re.compile('ready'),
                                        'namenode': re.compile('ready'),
                                        'resourcemanager': re.compile('ready'),
                                        'slave': re.compile('ready'),
                                        }, timeout=3600)
        cls.hdfs = cls.d.sentry['namenode'][0]
        cls.yarn = cls.d.sentry['resourcemanager'][0]
        cls.slave = cls.d.sentry['slave'][0]

    def test_components(self):
        """
        Confirm that all of the required components are up and running.
        """
        hdfs, retcode = self.hdfs.run("pgrep -a java")
        yarn, retcode = self.yarn.run("pgrep -a java")
        slave, retcode = self.slave.run("pgrep -a java")

        assert 'NameNode' in hdfs, "NameNode not started"
        assert 'NameNode' not in slave, "NameNode should not be running on slave"

        assert 'ResourceManager' in yarn, "ResourceManager not started"
        assert 'ResourceManager' not in slave, "ResourceManager should not be running on slave"

        assert 'JobHistoryServer' in yarn, "JobHistoryServer not started"
        assert 'JobHistoryServer' not in slave, "JobHistoryServer should not be running on slave"

        assert 'NodeManager' in slave, "NodeManager not started"
        assert 'NodeManager' not in yarn, "NodeManager should not be running on resourcemanager"
        assert 'NodeManager' not in hdfs, "NodeManager should not be running on namenode"

        assert 'DataNode' in slave, "DataServer not started"
        assert 'DataNode' not in yarn, "DataNode should not be running on resourcemanager"
        assert 'DataNode' not in hdfs, "DataNode should not be running on namenode"

    def test_hdfs(self):
        """
        Validates mkdir, ls, chmod, and rm HDFS operations.
        """
        uuid = self.hdfs.run_action('smoke-test')
        result = self.d.action_fetch(uuid, timeout=600, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('HDFS smoke-test did not complete: %s' % result)

    def test_yarn(self):
        """
        Validates YARN using the Bigtop 'yarn' smoke test.
        """
        uuid = self.yarn.run_action('smoke-test')
        # 'yarn' smoke takes a while (bigtop tests download lots of stuff)
        result = self.d.action_fetch(uuid, timeout=1800, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('YARN smoke-test did not complete: %s' % result)

    @unittest.skip(
        'Skipping slave smoke tests; they are too inconsistent and long running for CWR.')
    def test_slave(self):
        """
        Validates slave using the Bigtop 'hdfs' and 'mapred' smoke test.
        """
        uuid = self.slave.run_action('smoke-test')
        # 'hdfs+mapred' smoke takes a long while (bigtop tests are slow)
        result = self.d.action_fetch(uuid, timeout=3600, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('Slave smoke-test did not complete: %s' % result)


if __name__ == '__main__':
    unittest.main()
