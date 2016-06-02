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
import re


class TestConfigChanged(unittest.TestCase):
    """
    Test to verify that we update network interface bindings successfully.

    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='trusty')
        cls.d.log.debug("foo!")
        cls.d.add('kafka', 'kafka')
        cls.d.add('openjdk', 'openjdk')
        cls.d.add('zk', 'zookeeper')

        cls.d.configure('openjdk', {'java-type': 'jdk',
                                    'java-major': '8'})

        cls.d.relate('kafka:zookeeper', 'zk:zookeeper')
        cls.d.relate('kafka:java', 'openjdk:java')
        try:
            cls.d.relate('zk:java', 'openjdk:java')
        except ValueError:
            # No need to related older versions of the zookeeper charm
            # to java.
            pass

        cls.d.setup(timeout=900)
        cls.d.sentry.wait_for_messages({'kafka': 'ready'}, timeout=1800)
        cls.kafka = cls.d.sentry['kafka'][0]

    def test_bind_network_interface(self):
        """
        Test to verify that we update network interface bindings successfully.

        """
        self.d.configure('kafka', {'network_interface': 'eth0'})
        self.d.sentry.wait_for_messages({'kafka': 'updating zookeeper instances'}, timeout=600)

        self.d.sentry.wait_for_messages({'kafka': 'ready'}, timeout=600)
        ret = self.kafka.run(
            'grep host.name /etc/kafka/conf/server.properties')[0]
        # Correct line should start with host.name (no comment hash
        # mark), followed by an equals sign and something that looks
        # like an IP address (we aren't too strict about it being a
        # valid ip address.)
        matcher = re.compile("^host\.name=\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}.*")

        self.assertTrue('host.name' in ret)
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run
        smk_uuid = self.kafka.action_do("smoke-test")
        output = self.d.action_fetch(smk_uuid, full_output=True)
        assert "completed" in output['status']

    def test_reset_network_interface(self):
        """
        Verify that we can reset the network interface to 0.

        """
        self.d.configure('kafka', {'network_interface': '0.0.0.0'})
        self.d.sentry.wait_for_messages({'kafka': 'updating zookeeper instances'}, timeout=600)
        self.d.sentry.wait_for_messages({'kafka': 'ready'}, timeout=600)
        ret = self.kafka.run(
            'grep host.name /etc/kafka/conf/server.properties')[0]

        matcher = re.compile("^host\.name=0\.0\.0\.0.*")
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run
        smk_uuid = self.kafka.action_do("smoke-test")
        output = self.d.action_fetch(smk_uuid, full_output=True)
        assert "completed" in output['status']


if __name__ == '__main__':
    unittest.main()
