#!/bin/bash -x
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
     --build-dir=DIR             path to hive/build/dist
     --prefix=PREFIX             path to install into
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'version:' \
  -l 'build-dir:' \
  -l 'lib-dir:' \
  -l 'bin-dir:' \
  -l 'conf-dir:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --version)
        VERSION=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --conf-dir)
        CONF_DIR=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --)
        shift ; break
        ;;
        *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
done

for var in PREFIX BUILD_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

. `dirname ${0}`/bigtop.bom

BIN_DIR=${BIN_DIR:-/usr/lib/bigtop-groovy/bin}
CONF_DIR=${CONF_DIR:-/usr/lib/bigtop-groovy/conf}
LIB_DIR=${LIB_DIR:-/usr/lib/bigtop-groovy/lib}
echo ${BIGTOP_GROOVY_BASE_VERSION}

#bin
install -d -m 0755 ${PREFIX}/${BIN_DIR}
cp ${BUILD_DIR}/bin/startGroovy ${PREFIX}/${BIN_DIR}
cp ${BUILD_DIR}/bin/groovy ${PREFIX}/${BIN_DIR}
chmod 755 ${PREFIX}/${BIN_DIR}/*

# lib
install -d -m 0755 ${PREFIX}/${LIB_DIR}
cp ${BUILD_DIR}/lib/groovy-${BIGTOP_GROOVY_VERSION}.jar ${PREFIX}/${LIB_DIR}
cp ${BUILD_DIR}/lib/groovy-json-${BIGTOP_GROOVY_VERSION}.jar ${PREFIX}/${LIB_DIR}

#conf
install -d -m 0755 ${PREFIX}/${CONF_DIR}
cp ${BUILD_DIR}/conf/groovy-starter.conf  ${PREFIX}/${CONF_DIR}
