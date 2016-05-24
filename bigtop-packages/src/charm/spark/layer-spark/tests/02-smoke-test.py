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


class TestDeploy(unittest.TestCase):
    """
    Deployment and smoke-test of Apache Spark.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='trusty')
        cls.d.add('openjdk', 'openjdk')
        cls.d.add('spark', 'spark')
        cls.d.relate('spark:java', 'openjdk:java')
        cls.d.setup(timeout=900)
        cls.d.sentry.wait(timeout=1800)

    def test_deploy(self):
        self.d.sentry.wait_for_messages({
            "spark": "ready (standalone - master)",
        })
        spark = self.d.sentry['spark'][0]
        smk_uuid = spark.action_do("smoke-test")
        output = self.d.action_fetch(smk_uuid, full_output=True)
        assert "completed" in output['status']


if __name__ == '__main__':
    unittest.main()
