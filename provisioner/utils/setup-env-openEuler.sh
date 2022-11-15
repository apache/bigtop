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

# This may be crazy, but unless we change this - RHEL will actively
# revert back to localhost.localdomain
sed -ie 's#HOSTNAME=.*$#HOSTNAME='`hostname -f`'#' /etc/sysconfig/network

# Setup rng-tools to improve virtual machine entropy performance.
# The poor entropy performance will cause kerberos provisioning failed.
yum -y install rng-tools
if [ -x /usr/bin/systemctl ] ; then
    sed -i 's@ExecStart=/sbin/rngd -f@ExecStart=/sbin/rngd -f -r /dev/urandom@' /usr/lib/systemd/system/rngd.service
    systemctl daemon-reload
    systemctl start rngd
else
    sed -i.bak 's/EXTRAOPTIONS=\"\"/EXTRAOPTIONS=\"-r \/dev\/urandom\"/' /etc/sysconfig/rngd
    service rngd start
fi

if [ $enable_local_repo == "true" ]; then
    echo "Enabling local yum."

    if [ -f /etc/os-release ]; then
        . /etc/os-release
    fi

	sudo echo "gpgcheck=0" >> /etc/yum.conf

    mv /etc/yum.repos.d/openEuler.repo /etc/yum.repos.d/openEuler.repo-bak
    sudo echo "[bigtop-home_output]" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "name= bigtop-home_output" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "baseurl=file:///bigtop-home/output" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "gpgcheck=0" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "enabled=1" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "priority=9" >> /etc/yum.repos.d/bigtop-home_output.repo
else
    echo "local yum = $enable_local_repo ; NOT Enabling local yum.  Packages will be pulled from remote..."
fi
