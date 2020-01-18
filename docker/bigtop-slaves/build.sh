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

#!/bin/sh
set -ex

if [ $# != 1 ]; then
  echo "Creates bigtop/slaves image"
  echo
  echo "Usage: build.sh <PREFIX-OS-VERSION>"
  echo
  echo "Example: build.sh trunk-centos-7"
  echo "       : build.sh 1.0.0-centos-7"
  exit 1
fi

PREFIX=$(echo "$1" | cut -d '-' -f 1)
OS=$(echo "$1" | cut -d '-' -f 2)
VERSION=$(echo "$1" | cut -d '-' -f 3)
ARCH=$(uname -m)
if [ "${ARCH}" != "x86_64" ];then
  VERSION="${VERSION}-${ARCH}"
fi
## Workaround for docker defect on linaros cloud
if [ "${ARCH}" = "aarch64" ];then
  NETWORK="--network=host"
fi

# setup puppet/modules path and update cmds
case ${OS} in
    ubuntu)
        if [ "${VERSION}" > "16.04" ]; then
            PUPPET_MODULES="/usr/share/puppet/modules/bigtop_toolchain"
        else
            PUPPET_MODULES="/etc/puppet/modules/bigtop_toolchain"
        fi
        UPDATE_SOURCE="apt-get clean \&\& apt-get update"
        ;;
    debian)
        PUPPET_MODULES="/usr/share/puppet/modules/bigtop_toolchain"
        UPDATE_SOURCE="apt-get clean \&\& apt-get update"
        ;;
    fedora)
        PUPPET_MODULES="/etc/puppet/modules/bigtop_toolchain"
        UPDATE_SOURCE="dnf clean all \&\& dnf updateinfo"
        ;;
    centos)
        if [ "${VERSION}" -gt "7" ]; then
            PUPPET_MODULES="/etc/puppetlabs/code/environments/production/modules/bigtop_toolchain"
            UPDATE_SOURCE="dnf clean all \&\& dnf updateinfo"
        else
            PUPPET_MODULES="/etc/puppet/modules/bigtop_toolchain"
            UPDATE_SOURCE="yum clean all \&\& yum updateinfo"
        fi
        ;;
    opensuse)
        PUPPET_MODULES="/etc/puppet/modules/bigtop_toolchain"
        UPDATE_SOURCE="zypper clean \&\& zypper refresh"
        ;;
    *)
        echo "[ERROR] Specified distro [${OS}] is not supported!"
        exit 1
esac

# generate Dockerfile for build
sed -e "s|PREFIX|${PREFIX}|;s|OS|${OS}|;s|VERSION|${VERSION}|" Dockerfile.template | \
  sed -e "s|PUPPET_MODULES|${PUPPET_MODULES}|;s|UPDATE_SOURCE|${UPDATE_SOURCE}|" > Dockerfile

docker build ${NETWORK} --rm -t bigtop/slaves:${PREFIX}-${OS}-${VERSION} -f Dockerfile ../..
rm -f Dockerfile
