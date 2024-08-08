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

mkdir -p /data/sdv1
echo 'root:B767610qa4Z' | chpasswd

# This may be crazy, but unless we change this - RHEL will actively
# revert back to localhost.localdomain
sed -ie 's#HOSTNAME=.*$#HOSTNAME='`hostname -f`'#' /etc/sysconfig/network

if [ -f /etc/os-release ]; then
    . /etc/os-release
fi


cd /etc/yum.repos.d/ && mkdir backup && mv *repo backup/
repo_content='[baseos]
name=Rocky Linux $releasever - BaseOS
mirrorlist=https://mirrors.rockylinux.org/mirrorlist?arch=$basearch&repo=BaseOS-$releasever
gpgcheck=1
enabled=1
countme=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-rockyofficial

[devel]
name=Rocky Linux $releasever - Devel WARNING! FOR BUILDROOT AND KOJI USE
mirrorlist=https://mirrors.rockylinux.org/mirrorlist?arch=$basearch&repo=Devel-$releasever
gpgcheck=1
enabled=1
countme=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-rockyofficial
'

# Write the content to /etc/yum.repos.d/RockylinuxBase.repo
echo "$repo_content" > /etc/yum.repos.d/RockylinuxBase.repo

# Print a message indicating completion
echo "Repository configuration has been written to /etc/yum.repos.d/RockylinuxBase.repo"

#curl -o /etc/yum.repos.d/CentOS-Base.repo http://mirrors.aliyun.com/repo/Centos-8.repo

#sed -e 's|^mirrorlist=|#mirrorlist=|g' \
#    -e 's|^#baseurl=http://dl.rockylinux.org/$contentdir|baseurl=https://mirrors.aliyun.com/rockylinux|g' \
#    -i.bak \
#    /etc/yum.repos.d/Rocky*.repo

dnf makecache
yum install -y sshpass
yum -y install python3-devel
yum -y install createrepo
yum -y install openssh-clients openssh-server
systemctl start sshd.service

systemctl unmask systemd-logind.service
# Setup rng-tools to improve virtual machine entropy performance.
# The poor entropy performance will cause kerberos provisioning failed.

# BIGTOP-3883:
# yum-utils, yum-priorities and yum-config-manager are NOT available in openEuler 22.03
#if [ "${ID}" = "openEuler" ];then
#    dnf install rng-tools -y
#else
#    yum -y install yum-priorities
#fi

#if [ -x /usr/bin/systemctl ] ; then
#    sed -i 's@ExecStart=/sbin/rngd -f@ExecStart=/sbin/rngd -f -r /dev/urandom@' /usr/lib/systemd/system/rngd.service
#    systemctl daemon-reload
#    systemctl start rngd
#else
#    sed -i.bak 's/EXTRAOPTIONS=\"\"/EXTRAOPTIONS=\"-r \/dev\/urandom\"/' /etc/sysconfig/rngd
#    service rngd start
#fi

if [ $enable_local_repo == "true" ]; then
    echo "Enabling local yum."
    if [ "${ID}" != "openEuler" ];then
        yum -y install yum-utils
    fi

    case ${ID} in
        fedora | openEuler)
            sed -i 's/gpgcheck=1/gpgcheck=0/g' /etc/dnf/dnf.conf
            ;;
        centos)
            sudo echo "gpgcheck=0" >> /etc/yum.conf
            ;;
    esac

    if [ "${ID}" = "openEuler" ];then
        sudo dnf config-manager --add-repo file:///bigtop-home/output
    else
        sudo yum-config-manager --add-repo file:///bigtop-home/output
    fi

    sudo echo "gpgcheck=0" >> /etc/yum.repos.d/bigtop-home_output.repo
    sudo echo "priority=9" >> /etc/yum.repos.d/bigtop-home_output.repo
else
    echo "local yum = $enable_local_repo ; NOT Enabling local yum.  Packages will be pulled from remote..."
fi

