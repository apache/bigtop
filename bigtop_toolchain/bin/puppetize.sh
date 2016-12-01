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
else
    if [ -f /etc/redhat-release ]; then
	if grep "CentOS release 6" /etc/redhat-release >/dev/null ; then
	    ID=centos
	    VERSION_ID=6
	fi
    else
	echo "Unknown Linux Distribution."
	exit 1
    fi
fi

case ${ID}-${VERSION_ID} in
    fedora-20*)
	# Work around issue in fedora:20 docker image
	yum -y install yum-utils;  yum-config-manager --enable updates-testing
        rpm -ivh https://yum.puppetlabs.com/puppetlabs-release-fedora-20.noarch.rpm
        yum update
	yum -y install hostname curl sudo unzip wget puppet
	;;
    fedora-25*)
        dnf -y install yum-utils
        dnf -y update 
        dnf -y install hostname findutils curl sudo unzip wget puppet
        ;;
    ubuntu-14.04)
	apt-get update
	apt-get -y install wget
	if [ $HOSTTYPE = "x86_64" ] ; then
	  # BIGTOP-2003. A workaround to install newer hiera to get rid of hiera 1.3.0 bug.
	  wget -O /tmp/puppetlabs-release-trusty.deb https://apt.puppetlabs.com/puppetlabs-release-trusty.deb && dpkg -i /tmp/puppetlabs-release-trusty.deb
	  rm -f /tmp/puppetlabs-release-trusty.deb
	  apt-get update
        fi
	apt-get -y install curl sudo unzip puppet software-properties-common
	;;
    ubuntu-1[56]*)
	apt-get update
	apt-get -y install curl sudo unzip wget puppet software-properties-common
	;;
    debian-8*)
	apt-get update
	apt-get -y install wget
	# BIGTOP-2523. in order to install puppet 3.8 we need to get it from puppet repo
	wget -O /tmp/puppetlabs-release-trusty.deb https://apt.puppetlabs.com/puppetlabs-release-trusty.deb && dpkg -i /tmp/puppetlabs-release-trusty.deb
	rm -f /tmp/puppetlabs-release-trusty.deb
	apt-get update
	apt-get -y install curl sudo unzip puppet
	;;
    opensuse-*)
	zypper --gpg-auto-import-keys install -y curl sudo unzip wget puppet suse-release ca-certificates-mozilla net-tools tar
	;;
    centos-6*)
        rpm -ivh http://yum.puppetlabs.com/puppetlabs-release-el-6.noarch.rpm
	yum -y install curl sudo unzip wget puppet tar
	;;
    centos-7*)
        rpm -ivh http://yum.puppetlabs.com/puppetlabs-release-el-7.noarch.rpm
	yum -y install hostname curl sudo unzip wget puppet
	;;
    *)
	echo "Unsupported OS ${ID}-${VERSION_ID}."
	exit 1
esac

puppet module install puppetlabs-stdlib

case ${ID} in
   debian|ubuntu)
      puppet module install puppetlabs-apt;;
esac
