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
        cls.d.sentry.wait_for_messages({'spark': 'ready (standalone - HA)',
                                        'zookeeper': re.compile('ready'),
                                        }, timeout=3600)
        cls.spark = cls.d.sentry['spark'][0]
        cls.zookeeper = cls.d.sentry['zookeeper'][0]

    def test_components(self):
        """
        Confirm that all of the required components are up and running.
        """
        spark, rc = self.spark.run("pgrep -a java")
        zk, rc = self.zookeeper.run("pgrep -a java")

        assert 'Master' in spark, "Spark Master should be running"
        assert 'QuorumPeerMain' in zk, "Zookeeper QuorumPeerMain should be running"

    def test_spark(self):
        """
        Validates Spark with a simple sparkpi test.
        """
        uuid = self.spark.run_action('smoke-test')
        result = self.d.action_fetch(uuid, timeout=600, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('Spark smoke-test did not complete: %s' % result)

    def test_zookeeper(self):
        """
        Validates Zookeeper using the Bigtop 'zookeeper' smoke test.
        """
        uuid = self.zookeeper.run_action('smoke-test')
        # 'zookeeper' smoke takes a while (bigtop tests download lots of stuff)
        result = self.d.action_fetch(uuid, timeout=1800, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('Zookeeper smoke-test did not complete: %s' % result)


if __name__ == '__main__':
    unittest.main()
