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
import requests
import time
import unittest


class TestDeployment(unittest.TestCase):
    """
    Test scaling of Apache Spark in HA mode.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('spark-test-ha', charm='spark',
                  units=3, constraints={'mem': '7G'})
        cls.d.add('spark-test-zk', charm='zookeeper')

        cls.d.relate('spark-test-zk:zookeeper', 'spark-test-ha:zookeeper')
        cls.d.expose('spark-test-ha')

        cls.d.setup(timeout=3600)
        cls.d.sentry.wait(timeout=3600)

    def test_master_selected(self):
        """
        Wait for all three spark-test-ha units to agree on a master leader.
        Remove the leader unit.
        Check that a new leader is elected.
        """
        self.d.sentry.wait_for_messages({"spark-test-ha": ["ready (standalone - HA)",
                                                           "ready (standalone - HA)",
                                                           "ready (standalone - HA)"]}, timeout=900)

        print("Waiting for units to agree on master.")
        time.sleep(120)

        master = ''
        masters_count = 0
        for unit in self.d.sentry['spark-test-ha']:
            ip = unit.info['public-address']
            url = 'http://{}:8080'.format(ip)
            homepage = requests.get(url)
            if 'ALIVE' in homepage.text:
                masters_count += 1
                master = unit.info['unit_name']
            else:
                assert 'STANDBY' in homepage.text

        assert masters_count == 1

        print("Removing master: {} ".format(master))
        self.d.remove_unit(master)
        time.sleep(120)
        self.d.sentry.wait_for_messages({"spark-test-ha": ["ready (standalone - HA)",
                                                           "ready (standalone - HA)"]}, timeout=900)

        print("Waiting for remaining units to agree on master.")
        time.sleep(120)

        masters_count = 0
        for unit in self.d.sentry['spark-test-ha']:
            ip = unit.info['public-address']
            url = 'http://{}:8080'.format(ip)
            homepage = requests.get(url)
            if 'ALIVE' in homepage.text:
                print("New master is {}".format(unit.info['unit_name']))
                masters_count += 1
            else:
                assert 'STANDBY' in homepage.text

        assert masters_count == 1


if __name__ == '__main__':
    unittest.main()
