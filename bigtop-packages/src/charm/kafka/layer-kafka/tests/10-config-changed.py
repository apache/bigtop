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
import time
import unittest


class TestConfigChanged(unittest.TestCase):
    """
    Test to verify that we can bind to listen for client connections
    on a specific interface.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('kafka-test', charm='kafka')
        cls.d.add('kafka-test-zk', charm='zookeeper')

        cls.d.relate('kafka-test:zookeeper', 'kafka-test-zk:zookeeper')

        cls.d.setup(timeout=1800)
        cls.d.sentry.wait_for_messages({'kafka-test': 'ready'}, timeout=1800)
        cls.unit = cls.d.sentry['kafka-test'][0]

    @classmethod
    def tearDownClass(cls):
        # NB: seems to be a remove_service issue with amulet. However, the
        # unit does still get removed. Pass OSError for now:
        #  OSError: juju command failed ['remove-application', ...]:
        #  ERROR allocation for service ... owned by ... not found
        try:
            cls.d.remove_service('kafka-test', 'kafka-test-zk')
        except OSError as e:
            print("IGNORE: Amulet remove_service failed: {}".format(e))
            pass

    def test_bind_network_interface(self):
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
        self.d.configure('kafka-test', {'network_interface': network_interface})

        # NB: we used to watch for a maintenance status message, but every now
        # and then, we'd miss it. Wait 2m to let the config-changed hook settle.
        time.sleep(120)
        ret = self.unit.run(
            'grep host.name /etc/kafka/conf/server.properties')[0]

        # Correct line should start with host.name (no comment hash
        # mark), followed by an equals sign and something that looks
        # like an IP address (we aren't too strict about it being a
        # valid ip address.)
        matcher = re.compile("^host\.name=\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}.*")
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run
        smk_uuid = self.unit.run_action('smoke-test')
        result = self.d.action_fetch(smk_uuid, full_output=True)
        # actions set status=completed on success
        if (result['status'] != "completed"):
            self.fail('Kafka test failed after setting nic config: %s' % result)

    def test_reset_network_interface(self):
        """
        Verify that we can reset the client port bindings to 0.0.0.0
        """
        self.d.configure('kafka-test', {'network_interface': '0.0.0.0'})

        # NB: we used to watch for a maintenance status message, but every now
        # and then, we'd miss it. Wait 2m to let the config-changed hook settle.
        time.sleep(120)
        ret = self.unit.run(
            'grep host.name /etc/kafka/conf/server.properties')[0]

        matcher = re.compile("^host\.name=0\.0\.0\.0.*")
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run
        smk_uuid = self.unit.run_action('smoke-test')
        result = self.d.action_fetch(smk_uuid, full_output=True)
        # actions set status=completed on success
        if (result['status'] != "completed"):
            self.fail('Kafka test failed after resetting nic config: %s' % result)


if __name__ == '__main__':
    unittest.main()
