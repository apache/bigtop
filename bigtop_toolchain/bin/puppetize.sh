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
    fedora-31)
        dnf -y install yum-utils
        dnf -y check-update
        dnf -y install hostname findutils curl sudo unzip wget puppet procps-ng libxcrypt-compat
        # On Fedora 31, the puppetlabs-stdlib package provided by the distro installs the module
        # into /usr/share/puppet/modules, but it's not recognized as the default module path.
        # So we install that module in the same way as CentOS 7.
        puppet module install puppetlabs-stdlib --version 4.12.0
        ;;
    ubuntu-16.04)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet software-properties-common puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib
        ;;
    ubuntu-18.04)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet software-properties-common puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib systemd-sysv
        ;;
    debian-9*)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib systemd-sysv
        ;;
    debian-10*)
        apt-get update
        apt-get -y install wget curl sudo unzip puppet puppet-module-puppetlabs-apt puppet-module-puppetlabs-stdlib systemd-sysv gnupg
        ;;
    centos-7*)
        rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
        yum updateinfo
        # BIGTOP-3088: pin puppetlabs-stdlib to 4.12.0 as the one provided by
        # distro (4.25.0) has conflict with puppet<4. Should be removed once
        # puppet in distro is updated.
        yum -y install hostname curl sudo unzip wget puppet
        puppet module install puppetlabs-stdlib --version 4.12.0
        ;;
    centos-8*)
        rpm -Uvh https://yum.puppet.com/puppet5-release-el-8.noarch.rpm
        dnf -y check-update
        dnf -y install hostname curl sudo unzip wget puppet-agent 'dnf-command(config-manager)'
        puppet module install puppetlabs-stdlib
        # Enabling the PowerTools and EPEL repositories via Puppet doesn't seem to work in some cases.
        # As a workaround for that, enable the former here in advance of running the Puppet manifests.
        dnf config-manager --set-enabled PowerTools
        ;;
    rhel-8*)
        rpm -Uvh https://yum.puppet.com/puppet5-release-el-8.noarch.rpm
        dnf -y check-update
        dnf -y install hostname curl sudo unzip wget puppet-agent 'dnf-command(config-manager)'
        puppet module install puppetlabs-stdlib
        # Enabling the CodeReady repositories via Puppet doesn't seem to work in some cases.
        # As a workaround for that, enable the former here in advance of running the Puppet manifests.
        dnf config-manager --set-enabled codeready-builder-for-rhel-8-rhui-rpms
        ;;
    *)
        echo "Unsupported OS ${ID}-${VERSION_ID}."
        exit 1
        ;;
esac
