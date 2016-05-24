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
import time
import unittest
import requests


class TestDeployment(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='trusty')
        cls.d.add('sparkha', 'spark', units=3)
        cls.d.add('openjdk', 'openjdk')
        cls.d.add('zk', 'zookeeper')
        cls.d.expose('sparkha')
        cls.d.relate('openjdk:java', 'sparkha:java')
        cls.d.relate('zk:zookeeper', 'sparkha:zookeeper')
        try:
            cls.d.relate('zk:java', 'openjdk:java')
        except ValueError:
            # No need to related older versions of the zookeeper charm
            # to java.
            pass
        cls.d.setup(timeout=1800)
        cls.d.sentry.wait(timeout=1800)

    @classmethod
    def tearDownClass(cls):
        cls.d.remove_service('sparkha')

    def test_master_selected(self):
        '''
        Wait for all three spark units to agree on a master leader.
        Remove the leader unit.
        Check that a new leader is elected.
        '''
        self.d.sentry.wait_for_messages({"sparkha": ["ready (standalone - HA)",
                                                     "ready (standalone - HA)",
                                                     "ready (standalone - HA)"]}, timeout=900)
        # Give some slack for the spark units to elect a master
        time.sleep(60)
        master = ''
        masters_count = 0
        for unit in self.d.sentry['sparkha']:
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

        self.d.sentry.wait_for_messages({"sparkha": ["ready (standalone - HA)",
                                                     "ready (standalone - HA)"]}, timeout=900)

        masters_count = 0
        for unit in self.d.sentry['sparkha']:
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
