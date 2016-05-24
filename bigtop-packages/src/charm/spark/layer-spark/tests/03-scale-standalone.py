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
import time


class TestScaleStandalone(unittest.TestCase):
    """
    Test scaling of Apache Spark in standalone mode.
    """
    @classmethod
    def setUpClass(cls):
        cls.d = amulet.Deployment(series='trusty')
        cls.d.add('sparkscale', 'spark', units=3)
        cls.d.add('openjdk', 'openjdk')
        cls.d.relate('openjdk:java', 'sparkscale:java')
        cls.d.setup(timeout=1800)
        cls.d.sentry.wait(timeout=1800)

    @classmethod
    def tearDownClass(cls):
        cls.d.remove_service('sparkscale')

    def test_scaleup(self):
        """
        Wait for all three spark units to agree on a master.
        Remove the master.
        Check that all units agree on the same new master.
        """
        print("Waiting for units to become ready.")
        self.d.sentry.wait_for_messages({"sparkscale": ["ready (standalone - master)",
                                                        "ready (standalone)",
                                                        "ready (standalone)"]}, timeout=900)

        print("Waiting for units to agree on master.")
        time.sleep(60)

        spark0_unit = self.d.sentry['sparkscale'][0]
        spark1_unit = self.d.sentry['sparkscale'][1]
        spark2_unit = self.d.sentry['sparkscale'][2]
        (stdout0, errcode0) = spark0_unit.run('grep spark.master /etc/spark/conf/spark-defaults.conf')
        (stdout1, errcode1) = spark1_unit.run('grep spark.master /etc/spark/conf/spark-defaults.conf')
        (stdout2, errcode2) = spark2_unit.run('grep spark.master /etc/spark/conf/spark-defaults.conf')
        # ensure units agree on the master
        assert stdout0 == stdout2
        assert stdout1 == stdout2

        master_name = ''
        for unit in self.d.sentry['sparkscale']:
            (stdout, stderr) = unit.run("pgrep -f \"[M]aster\"")
            lines = len(stdout.split('\n'))
            if lines > 0:
                master_name = unit.info['unit_name']
                print("Killin master {}".format(master_name))
                self.d.remove_unit(master_name)
                break

        print("Waiting for the cluster to select a new master.")
        time.sleep(60)
        self.d.sentry.wait_for_messages({"sparkscale": ["ready (standalone - master)",
                                                        "ready (standalone)"]}, timeout=900)

        spark1_unit = self.d.sentry['sparkscale'][0]
        spark2_unit = self.d.sentry['sparkscale'][1]
        (stdout1, errcode1) = spark1_unit.run('grep spark.master /etc/spark/conf/spark-defaults.conf')
        (stdout2, errcode2) = spark2_unit.run('grep spark.master /etc/spark/conf/spark-defaults.conf')
        # ensure units agree on the master
        assert stdout1 == stdout2


if __name__ == '__main__':
    unittest.main()
