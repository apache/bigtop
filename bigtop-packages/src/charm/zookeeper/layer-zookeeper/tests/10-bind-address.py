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
import time
import unittest

TIMEOUT = 1800


class TestBindClientPort(unittest.TestCase):
    """
    Test to verify that we can bind to listen for client connections
    on a specific interface.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('zk-test', charm='zookeeper')
        cls.d.setup(timeout=TIMEOUT)
        cls.d.sentry.wait_for_messages({'zk-test': re.compile('^ready')},
                                       timeout=TIMEOUT)
        cls.unit = cls.d.sentry['zk-test'][0]

    @classmethod
    def tearDownClass(cls):
        # NB: seems to be a remove_service issue with amulet. However, the
        # unit does still get removed. Pass OSError for now:
        #  OSError: juju command failed ['remove-application', 'zk-test']:
        #  ERROR allocation for service ...zk-test... owned by ... not found
        try:
            cls.d.remove_service('zk-test')
        except OSError as e:
            print("IGNORE: Amulet remove_service failed: {}".format(e))
            pass

    def test_bind_port(self):
        """
        Verify that we update client port bindings successfully.
        """
        network_interface = None
        # Regular expression should handle interfaces in the format
        # eth[n], and in the format en[foo] (the "predicatble
        # interface names" in v197+ of systemd).
        ethernet_interface = re.compile('^e[thn]+.*')
        interfaces, _ = self.unit.run(
            "ifconfig -a | sed 's/[ \t].*//;/^$/d'")
        interfaces = interfaces.split()  # Splits on newlines
        for interface in interfaces:
            if ethernet_interface.match(interface):
                network_interface = interface
                break

        if network_interface is None:
            raise Exception(
                "Could not find any interface on the unit that matched my "
                "criteria.")
        self.d.configure('zk-test', {'network_interface': network_interface})

        # NB: we used to watch for a maintenance status message, but every now
        # and then, we'd miss it. Wait 2m to let the config-changed hook settle.
        time.sleep(120)
        ret = self.unit.run(
            'grep clientPortAddress /etc/zookeeper/conf/zoo.cfg')[0]

        matcher = re.compile(
            "^clientPortAddress=\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}.*")
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run and the unit returns to 'ready'
        smk_uuid = self.unit.run_action('smoke-test')
        # 'zookeeper' smoke takes a while (bigtop tests are slow)
        result = self.d.action_fetch(smk_uuid, timeout=1800, full_output=True)
        # actions set status=completed on success
        if (result['status'] != "completed"):
            self.fail('Zookeeper smoke-test failed: %s' % result)
        self.d.sentry.wait_for_messages({'zk-test': re.compile('^ready')},
                                        timeout=TIMEOUT)

    def test_reset_bindings(self):
        """
        Verify that we can reset the client port bindings to 0.0.0.0
        """
        self.d.configure('zk-test', {'network_interface': '0.0.0.0'})

        # NB: we used to watch for a maintenance status message, but every now
        # and then, we'd miss it. Wait 2m to let the config-changed hook settle.
        time.sleep(120)
        ret = self.unit.run(
            'grep clientPortAddress /etc/zookeeper/conf/zoo.cfg')[0]

        matcher = re.compile("^clientPortAddress=0\.0\.0\.0.*")
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run and the unit returns to 'ready'
        smk_uuid = self.unit.run_action('smoke-test')
        # 'zookeeper' smoke takes a while (bigtop tests are slow)
        result = self.d.action_fetch(smk_uuid, timeout=1800, full_output=True)
        # actions set status=completed on success
        if (result['status'] != "completed"):
            self.fail('Zookeeper smoke-test failed: %s' % result)
        self.d.sentry.wait_for_messages({'zk-test': re.compile('^ready')},
                                        timeout=TIMEOUT)


if __name__ == '__main__':
    unittest.main()
