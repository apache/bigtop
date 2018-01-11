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
import unittest


class TestSmokeSpark(unittest.TestCase):
    """
    Smoke test for Apache Bigtop Zeppelin using remote Spark resources.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('zeppelin')
        cls.d.add('zeppelin-test-spark', charm='spark')

        cls.d.relate('zeppelin-test-spark:client', 'zeppelin:spark')

        cls.d.setup(timeout=1800)
        cls.d.sentry.wait_for_messages({'zeppelin': re.compile('ready with')}, timeout=1800)
        cls.zeppelin = cls.d.sentry['zeppelin'][0]

    @classmethod
    def tearDownClass(cls):
        # NB: seems to be a remove_service issue with amulet. However, the
        # unit does still get removed. Pass OSError for now:
        #  OSError: juju command failed ['remove-application', ...]:
        #  ERROR allocation for service ... owned by ... not found
        try:
            cls.d.remove_service('zeppelin-test-spark')
        except OSError as e:
            print("IGNORE: Amulet remove_service failed: {}".format(e))
            pass

    def test_zeppelin(self):
        """
        Validate Zeppelin by running the smoke-test action.
        """
        uuid = self.zeppelin.run_action('smoke-test')
        result = self.d.action_fetch(uuid, full_output=True)
        # action status=completed on success
        if (result['status'] != "completed"):
            self.fail('Zeppelin smoke-test failed: %s' % result)


if __name__ == '__main__':
    unittest.main()
