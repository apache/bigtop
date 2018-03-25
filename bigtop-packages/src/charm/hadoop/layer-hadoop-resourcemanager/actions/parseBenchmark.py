#!/usr/local/sbin/charm-env python3

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

"""
Simple script to parse benchmark transaction results
and reformat them as JSON for sending back to juju
"""
import sys
import json
from charmhelpers.core import hookenv
import re


def parse_benchmark_output():
    """
    Parse the output from the benchmark and set the action results:
    """
    results = {}

    # Find all of the interesting things
    regex = re.compile('\t+(.*)=(.*)')
    for line in sys.stdin.readlines():
        m = regex.match(line)
        if m:
            results[m.group(1)] = m.group(2)
    hookenv.action_set({"meta.raw": json.dumps(results)})


if __name__ == "__main__":
    parse_benchmark_output()
