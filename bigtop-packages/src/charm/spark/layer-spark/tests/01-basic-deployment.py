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
    Simple deployment test for Apache Bigtop Spark.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        cls.d.add('spark')
        cls.d.setup(timeout=1800)
        cls.d.sentry.wait(timeout=1800)
        cls.unit = cls.d.sentry['spark'][0]

    def test_deployed(self):
        """
        Validate Spark deployed successfully.
        """
        self.assertTrue(self.d.deployed)


if __name__ == '__main__':
    unittest.main()
