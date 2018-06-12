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

# Use /etc/os-release to determine Linux Distro

if [ -f /etc/os-release ]; then
    . /etc/os-release
fi

case ${ID}-${VERSION_ID} in
    fedora-26)
      dnf -y install yum-utils
      dnf -y check-update
      dnf -y install hostname findutils curl sudo unzip wget puppet puppetlabs-stdlib
        ;;
    ubuntu-16.04)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet software-properties-common puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib
        ;;
    debian-9*)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib systemd-sysv
         ;;
    opensuse-42.3)
        zypper --gpg-auto-import-keys install -y curl sudo unzip wget puppet suse-release ca-certificates-mozilla net-tools tar
        puppet module install puppetlabs-stdlib
        ;;
    centos-7*)
        rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
        yum -y install hostname curl sudo unzip wget puppet puppetlabs-stdlib
        ;;
    *)
        echo "Unsupported OS ${ID}-${VERSION_ID}."
        exit 1
        ;;
esac
