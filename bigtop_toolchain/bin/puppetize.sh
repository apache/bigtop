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

RPM_DOWNLOAD_URL="https://yum.puppet.com/puppet7"
APT_DOWNLOAD_URL="https://apt.puppet.com/puppet7"
NIGHTLY=""

if [ "${ARCH}" = "ppc64le" ];then
RPM_DOWNLOAD_URL="https://nightlies.puppetlabs.com/yum/puppet7-nightly"
APT_DOWNLOAD_URL="https://nightlies.puppetlabs.com/apt/puppet7-nightly"
NIGHTLY="-nightly"
fi

case ${ID}-${VERSION_ID} in
    fedora-35)
        rpm -Uvh ${RPM_DOWNLOAD_URL}-release-fedora-34.noarch.rpm
        dnf -y install yum-utils
        dnf -y check-update
        dnf -y install hostname diffutils findutils curl sudo unzip wget puppet-agent procps-ng libxcrypt-compat systemd
        # On Fedora 31, the puppetlabs-stdlib package provided by the distro installs the module
        # into /usr/share/puppet/modules, but it's not recognized as the default module path.
        # So we install that module in the same way as CentOS 7.
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib 
        ;;
    ubuntu-18.04|ubuntu-20.04)
        apt-get update
        apt-get -y install wget curl sudo unzip software-properties-common systemd-sysv
        wget -P /tmp ${APT_DOWNLOAD_URL}-release-bionic.deb
        dpkg -i /tmp/puppet7${NIGHTLY}-release-bionic.deb
        apt-get update
        apt-get -y install puppet-agent
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib
        /opt/puppetlabs/bin/puppet module install puppetlabs-apt 
        ;;
    debian-10*|debian-11*)
        apt-get update
        apt-get -y install wget curl sudo unzip systemd-sysv gnupg procps
        wget -P /tmp ${APT_DOWNLOAD_URL}-release-buster.deb
        dpkg -i /tmp/puppet7${NIGHTLY}-release-buster.deb
        apt-get update
        apt-get -y install puppet-agent
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib
        /opt/puppetlabs/bin/puppet module install puppetlabs-apt
        ;;
    centos-7*)
        rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
        rpm -Uvh ${RPM_DOWNLOAD_URL}-release-el-7.noarch.rpm
        yum update -y
        yum -y install hostname curl sudo unzip wget puppet-agent
        # bump puppet to puppet7
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib
        ;;
    centos-8*)
        sed -i -e 's/^\(mirrorlist\)/#\1/' -e 's,^#baseurl=http://mirror.centos.org,baseurl=https://vault.centos.org,' /etc/yum.repos.d/CentOS-Linux-*
        ;&
    rocky-8*)
        rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
        rpm -Uvh ${RPM_DOWNLOAD_URL}-release-el-8.noarch.rpm
        dnf -y check-update
        dnf -y install glibc-langpack-en hostname diffutils curl sudo unzip wget puppet-agent 'dnf-command(config-manager)'
        # Install the module in the same way as Fedora 31 and CentOS 7 for compatibility issues.
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib
        # Enabling the PowerTools and EPEL repositories via Puppet doesn't seem to work in some cases.
        # As a workaround for that, enable the former here in advance of running the Puppet manifests.
        dnf config-manager --set-enabled powertools
        ;;
    rhel-8*)
        rpm -Uvh ${RPM_DOWNLOAD_URL}-release-el-8.noarch.rpm
        dnf -y check-update
        dnf -y install hostname diffutils curl sudo unzip wget puppet-agent 'dnf-command(config-manager)'
        /opt/puppetlabs/bin/puppet module install puppetlabs-stdlib
        # Enabling the CodeReady repositories via Puppet doesn't seem to work in some cases.
        # As a workaround for that, enable the former here in advance of running the Puppet manifests.
        dnf config-manager --set-enabled codeready-builder-for-rhel-8-rhui-rpms
        ;;
    *)
        echo "Unsupported OS ${ID}-${VERSION_ID}."
        exit 1
        ;;
esac
