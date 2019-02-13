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
        rpm -Uvh https://yum.puppet.com/puppet5/puppet5-release-fedora-26.noarch.rpm
        dnf -y install yum-utils
        dnf -y check-update
        dnf -y install hostname findutils curl sudo unzip wget puppet procps-ng
        ln -s /opt/puppetlabs/puppet/bin/puppet /usr/bin/puppet
        ln -s /etc/puppetlabs/puppet /etc/puppet
        puppet module install puppetlabs-stdlib
        ;;
    ubuntu-16.04)
        apt-get update
        apt-get -y install wget curl sudo unzip software-properties-common
        wget https://apt.puppetlabs.com/puppet5-release-xenial.deb
        dpkg -i puppet5-release-xenial.deb
        rm -f puppet5-release-xenial.deb
        apt-get update
        apt-get -y install puppet
        ln -s /opt/puppetlabs/puppet/bin/puppet /usr/bin/puppet
        ln -s /etc/puppetlabs/puppet /etc/puppet
        puppet module install puppetlabs-stdlib
        # BIGTOP-3161: version 6.3.0 of puppetlats-apt can cause dependency cycle
        puppet module install puppetlabs-apt --version 6.2.1
        ;;
    debian-9*)
        apt-get update
        apt-get -y install wget curl sudo unzip systemd-sysv
        wget https://apt.puppetlabs.com/puppet5-release-stretch.deb
        dpkg -i puppet5-release-stretch.deb
        rm -f puppet5-release-stretch.deb
        apt-get update
        apt-get -y install puppet
        ln -s /opt/puppetlabs/puppet/bin/puppet /usr/bin/puppet
        ln -s /etc/puppetlabs/puppet /etc/puppet
        puppet module install puppetlabs-stdlib
        # BIGTOP-3161: version 6.3.0 of puppetlats-apt can cause dependency cycle
        puppet module install puppetlabs-apt --version 6.2.1
         ;;
    opensuse-42.3)
        zypper --gpg-auto-import-keys install -y curl sudo unzip wget suse-release ca-certificates-mozilla net-tools tar systemd-sysvinit
        rpm -Uvh https://yum.puppet.com/puppet5/puppet5-release-sles-12.noarch.rpm
        zypper --no-gpg-check install -y puppet
        ln -s /opt/puppetlabs/puppet/bin/puppet /usr/bin/puppet
        ln -s /etc/puppetlabs/puppet /etc/puppet
        puppet module install puppetlabs-stdlib
        ;;
    centos-7*)
        rpm -Uvh https://yum.puppet.com/puppet5/puppet5-release-el-7.noarch.rpm
        yum updateinfo
        yum -y install hostname curl sudo unzip wget puppet
        ln -s /opt/puppetlabs/puppet/bin/puppet /usr/bin/puppet
        ln -s /etc/puppetlabs/puppet /etc/puppet
        puppet module install puppetlabs-stdlib
        ;;
    *)
        echo "Unsupported OS ${ID}-${VERSION_ID}."
        exit 1
        ;;
esac
