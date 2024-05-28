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

set -e

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR                      path to doris dist.dir
     --source-dir=DIR                     path to package shared files dir
     --prefix=PREFIX                      path to install into

  Optional options:
     --doris-fe-lib-dir=DIR               path to install doris home [/usr/lib/doris-fe]
     --doris-be-lib-dir=DIR               path to install doris home [/usr/lib/doris-be]
     --bin-dir=DIR                        path to install bins [/usr/bin]
     --lib-hadoop=DIR                     path to hadoop home [/usr/lib/hadoop]
     --etc-doris=DIR                   path to install doris conf [/etc/doris]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'doris-fe-lib-dir:' \
  -l 'doris-be-lib-dir:' \
  -l 'bin-dir:' \
  -l 'lib-hadoop:' \
  -l 'etc-doris:' \
  -l 'source-dir:' \
  -l 'build-dir:' -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --source-dir)
        SOURCE_DIR=$2 ; shift 2
        ;;
        --doris-fe-lib-dir)
        DORIS_FE_LIB_DIR=$2 ; shift 2
        ;;
        --doris-be-lib-dir)
        DORIS_BE_LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-hadoop)
        LIB_HADOOP=$2 ; shift 2
        ;;
        --etc-doris)
        ETC_DORIS=$2 ; shift 2
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

for var in PREFIX BUILD_DIR SOURCE_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

# load bigtop component versions
if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi


BIN_DIR=${BIN_DIR:-/usr/bin}
LIB_HADOOP=${LIB_HADOOP:-/usr/lib/hadoop}
DORIS_FE_LIB_DIR=${DORIS_FE_LIB_DIR:-/usr/lib/doris-fe}
DORIS_BE_LIB_DIR=${DORIS_BE_LIB_DIR:-/usr/lib/doris-be}

ETC_DORIS=${ETC_DORIS:-/etc/doris}
ETC_DORIS_FE=$ETC_DORIS/conf.dist/doris-fe
ETC_DORIS_BE=$ETC_DORIS/conf.dist/doris-be

# No prefix
NP_ETC_DORIS=/etc/doris
NP_ETC_DORIS_FE=$NP_ETC_DORIS/conf.dist/doris-fe
NP_ETC_DORIS_BE=$NP_ETC_DORIS/conf.dist/doris-be
NP_VAR_LOG_DORIS_FE=/var/log/doris-fe
NP_VAR_LOG_DORIS_BE=/var/log/doris-be
NP_ETC_HADOOP=/etc/hadoop

install -d -m 0755 $PREFIX/$NP_ETC_DORIS
# Doris FE
install -d -m 0755 $PREFIX/$DORIS_FE_LIB_DIR
install -d -m 0755 $PREFIX/$NP_ETC_DORIS_FE
install -d -m 0755 $PREFIX/$ETC_DORIS_FE
install -d -m 0755 $PREFIX/$NP_VAR_LOG_DORIS_FE
install -d -m 0755 $PREFIX/var/run/doris-fe
# Doris BE
install -d -m 0755 $PREFIX/$DORIS_BE_LIB_DIR
install -d -m 0755 $PREFIX/$NP_ETC_DORIS_BE
install -d -m 0755 $PREFIX/$ETC_DORIS_BE
install -d -m 0755 $PREFIX/$NP_VAR_LOG_DORIS_BE
install -d -m 0755 $PREFIX/var/run/doris-be

cp -ra ${BUILD_DIR}/fe/* $PREFIX/${DORIS_FE_LIB_DIR}/
cp -ra ${BUILD_DIR}/be/* $PREFIX/${DORIS_BE_LIB_DIR}/

# remove conf directory
rm -rf $PREFIX/${LIB_DIR}/$DORIS_FE_LIB_DIR/conf
rm -rf $PREFIX/${LIB_DIR}/$DORIS_BE_LIB_DIR/conf
# remove log directory
rm -rf $PREFIX/${LIB_DIR}/$DORIS_FE_LIB_DIR/log
rm -rf $PREFIX/${LIB_DIR}/$DORIS_BE_LIB_DIR/log

# Copy the configuration files
cp -ra ${BUILD_DIR}/fe/conf/* $PREFIX/$ETC_DORIS_FE
cp -ra ${BUILD_DIR}/be/conf/* $PREFIX/$ETC_DORIS_BE

# link the conf directory
ln -s $NP_ETC_DORIS_FE $PREFIX/$DORIS_FE_LIB_DIR/conf
ln -s $NP_ETC_DORIS_BE $PREFIX/$DORIS_BE_LIB_DIR/conf
# link the log directory
ln -s $NP_VAR_LOG_DORIS_FE $PREFIX/$DORIS_FE_LIB_DIR/log
ln -s $NP_VAR_LOG_DORIS_FE $PREFIX/$DORIS_BE_LIB_DIR/log


