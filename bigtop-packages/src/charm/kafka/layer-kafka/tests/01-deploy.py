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
import unittest


class TestDeploy(unittest.TestCase):
    """
    Trivial deployment test for Apache Bigtop Kafka.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('kafka')
        cls.d.add('zookeeper')

        cls.d.relate('kafka:zookeeper', 'zookeeper:zookeeper')

        cls.d.setup(timeout=1800)
        cls.d.sentry.wait_for_messages({'kafka': 'ready'}, timeout=1800)
        cls.kafka = cls.d.sentry['kafka'][0]

    def test_deploy(self):
        """
        Simple test to make sure the Kafka java process is running.
        """
        output, retcode = self.kafka.run("pgrep -a java")
        assert 'Kafka' in output, "Kafka daemon is not started"


if __name__ == '__main__':
    unittest.main()
