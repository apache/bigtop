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


class TestAutopurge(unittest.TestCase):
    """
    Test to verify options for snapshots are set in place.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('zk-autopurge', charm='zookeeper')
        cls.d.setup(timeout=TIMEOUT)
        cls.d.sentry.wait_for_messages({'zk-autopurge': re.compile('^ready')},
                                       timeout=TIMEOUT)
        cls.unit = cls.d.sentry['zk-autopurge'][0]

    @classmethod
    def tearDownClass(cls):
        # NB: seems to be a remove_service issue with amulet. However, the
        # unit does still get removed. Pass OSError for now:
        #  OSError: juju command failed ['remove-application', 'zk-autopurge']:
        #  ERROR allocation for service ...zk-autopurge... owned by ... not found
        try:
            cls.d.remove_service('zk-autopurge')
        except OSError as e:
            print("IGNORE: Amulet remove_service failed: {}".format(e))
            pass

    def test_autopurge_config(self):
        """
        Verify that purgeInterval and snapRetainCount are set successfully.
        """
        autopurge_purge_interval = 30
        autopurge_snap_retain_count = 5
        self.d.configure('zk-autopurge', {
            'autopurge_purge_interval': autopurge_purge_interval,
            'autopurge_snap_retain_count': autopurge_snap_retain_count,
        })

        # NB: we used to watch for a maintenance status message, but every now
        # and then, we'd miss it. Wait 2m to let the config-changed hook settle.
        time.sleep(120)
        ret = self.unit.run(
            'grep autopurge\.purgeInterval /etc/zookeeper/conf/zoo.cfg')[0]
        matcher = re.compile(
            "^autopurge\.purgeInterval={}".format(autopurge_purge_interval))
        self.assertTrue(matcher.match(ret))

        ret = self.unit.run(
            'grep autopurge\.snapRetainCount /etc/zookeeper/conf/zoo.cfg')[0]
        matcher = re.compile(
            "^autopurge\.snapRetainCount={}".format(autopurge_snap_retain_count))
        self.assertTrue(matcher.match(ret))

        # Verify that smoke tests still run and the unit returns to 'ready'
        smk_uuid = self.unit.run_action('smoke-test')
        # 'zookeeper' smoke takes a while (bigtop tests are slow)
        result = self.d.action_fetch(smk_uuid, timeout=1800, full_output=True)
        # actions set status=completed on success
        if (result['status'] != "completed"):
            self.fail('Zookeeper smoke-test failed: %s' % result)
        self.d.sentry.wait_for_messages({'zk-autopurge': re.compile('^ready')},
                                        timeout=TIMEOUT)


if __name__ == '__main__':
    unittest.main()
