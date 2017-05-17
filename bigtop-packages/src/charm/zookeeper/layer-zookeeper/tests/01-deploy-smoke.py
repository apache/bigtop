#!/usr/bin/python3

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
    Deployment test for Apache Zookeeper quorum
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('zookeeper', units=3)
        cls.d.setup(timeout=1800)
        cls.d.sentry.wait_for_messages({'zookeeper': re.compile('ready')},
                                       timeout=1800)
        cls.unit = cls.d.sentry['zookeeper'][0]

    def test_deploy(self):
        """Verify zk quorum is running"""
        output, retcode = self.unit.run("pgrep -a java")
        assert 'QuorumPeerMain' in output, "Zookeeper QuorumPeerMain daemon is not started"

    def test_quorum(self):
        """
        Verify that our peers are talking to each other, and taking on
        appropriate roles.
        """
        for unit in self.d.sentry['zookeeper']:
            output, _ = unit.run(
                "/usr/lib/zookeeper/bin/zkServer.sh status"
            )
            # Unit should be a leader or follower
            self.assertTrue("leader" in output or "follower" in output)

    def test_smoke(self):
        """Validates Zookeeper using the Bigtop 'zookeeper' smoke test."""
        smk_uuids = []

        for unit in self.d.sentry['zookeeper']:
            smk_uuids.append(unit.run_action('smoke-test'))

        for smk_uuid in smk_uuids:
            # 'zookeeper' smoke takes a while (bigtop tests are slow)
            result = self.d.action_fetch(smk_uuid, timeout=1800, full_output=True)
            # actions set status=completed on success
            if (result['status'] != "completed"):
                self.fail('Zookeeper smoke-test failed: %s' % result)


if __name__ == '__main__':
    unittest.main()
