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

set -ex

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to ambari dist.dir
     --prefix=PREFIX             path to install into
     --source-dir=DIR            path to the source code
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'source-dir:' \
  -l 'distro-dir:' \
  -l 'build-dir:' -- "$@")

if [ $? != 0 ]; then
  usage
fi

eval set -- "$OPTS"
while true; do
  case "$1" in
  --prefix)
    PREFIX=$2
    shift 2
    ;;
  --build-dir)
    BUILD_DIR=$2
    shift 2
    ;;
  --source-dir)
    SOURCE_DIR=$2
    shift 2
    ;;
  --distro-dir)
    DISTRO_DIR=$2
    shift 2
    ;;
  --)
    shift
    break
    ;;
  *)
    echo "Unknown option: $1"
    usage
    exit 1
    ;;
  esac
done

for var in PREFIX BUILD_DIR SOURCE_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

install -d -m 0755 ${PREFIX}

#Ambari Server
LIB_DIR=${LIB_DIR:-/usr/lib/ambari-server}
ETC_DIR=${ETC_DIR:-/etc/ambari-server}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf}

VAR_LIB_DIR=/var/lib/ambari-server
SBIN_DIR=/usr/sbin
SERVER_DIR=$BUILD_DIR/ambari-server/target/ambari-server-*-dist

cp -ra $SERVER_DIR/* ${PREFIX}/

# End of Ambari Server

LIB_DIR=/usr/lib/ambari-agent
ETC_DIR=/etc/ambari-agent
VAR_LIB_DIR=/var/lib/ambari-agent

CONF_DIR=${ETC_DIR}/conf
AGENT_BUILD_DIR=$(find ${BUILD_DIR}/ambari-agent/target -type d -name 'ambari-agent-*')

cp -ra $AGENT_BUILD_DIR/* ${PREFIX}/
