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

import os
import unittest

import amulet
import yaml


class TestBundle(unittest.TestCase):
    bundle_file = os.path.join(os.path.dirname(__file__), '..', 'bundle.yaml')

    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='xenial')
        with open(cls.bundle_file) as f:
            bun = f.read()
        bundle = yaml.safe_load(bun)
        cls.d.load(bundle)
        cls.d.setup(timeout=1800)
        cls.d.sentry.wait_for_messages({'spark': 'ready (standalone - HA)'}, timeout=1800)
        cls.spark = cls.d.sentry['spark'][0]

    def test_components(self):
        """
        Confirm that all of the required components are up and running.
        """
        spark, retcode = self.spark.run("pgrep -a java")

        assert 'spark' in spark, 'Spark should be running on spark'

    def test_spark(self):
        output, retcode = self.spark.run("su ubuntu -c 'bash -lc /home/ubuntu/sparkpi.sh 2>&1'")
        assert 'Pi is roughly' in output, 'SparkPI test failed: %s' % output


if __name__ == '__main__':
    unittest.main()
