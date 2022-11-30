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
  echo "Creates bigtop/puppet image"
  echo
  echo "Usage: build.sh <PREIX-OS-VERSION>"
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
ARCH="-${ARCH}"
else
ARCH=""
fi

ENV_PATH=""
if [ ${OS} = "centos" -a ${VERSION} -ge 8 ]; then
  ENV_PATH='ENV PATH /opt/puppetlabs/bin:$PATH'
fi

cp ../../bigtop_toolchain/bin/puppetize.sh .
cat >Dockerfile <<EOF
FROM ${OS}:${VERSION}
MAINTAINER dev@bigtop.apache.org
COPY puppetize.sh /tmp/puppetize.sh
${ENV_PATH}
RUN bash /tmp/puppetize.sh
EOF

# modify the Dockerfile according to the OS/version
case "${OS}-${VERSION}" in
  fedora-36*)
    # use java 8 during build if the os's default java version is newer
    sed -i -e "s|RUN bash /tmp/puppetize.sh|ENV PATH /usr/lib/jvm/java-1.8.0/bin:\$PATH\nRUN bash /tmp/puppetize.sh|" Dockerfile
    # add initd
    sed -i -e "s|RUN bash /tmp/puppetize.sh|RUN bash /tmp/puppetize.sh\nRUN dnf install -y initscripts|" Dockerfile
    ;;
  *)
    ;;
esac

if [ $OS == "openeuler" ];then
  OPENEULER_OS="${OS}/${OS}"
  sed -i -e "s|${OS}:${VERSION}|$OPENEULER_OS:${VERSION}|" ./Dockerfile
fi

docker build -t bigtop/puppet:${PREFIX}-${OS}-${VERSION}${ARCH} .
rm -f Dockerfile puppetize.sh
