#!/bin/bash

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

enable_local_repo=${1:-false}

bash /bigtop-home/bigtop_toolchain/bin/puppetize.sh

# Setup rng-tools to improve virtual machine entropy performance.
# The poor entropy performance will cause kerberos provisioning failed.
apt-get -y install rng-tools
sed -i.bak 's@#HRNGDEVICE=/dev/null@HRNGDEVICE=/dev/urandom@' /etc/default/rng-tools
service rng-tools start

if [ $enable_local_repo == "true" ]; then
    echo "deb file:///bigtop-home/output/apt bigtop contrib" > /etc/apt/sources.list.d/bigtop-home_output.list
    apt-get update
else
    echo "local apt = $enable_local_repo ; NOT Enabling local apt. Packages will be pulled from remote..."
fi

