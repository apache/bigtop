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
     --distro-dir=DIR            path to distro specific files (debian/RPM)
     --build-dir=DIR             path to dist dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install bits [/usr/lib/knox]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
     --examples-dir=DIR          path to install examples [doc-dir/examples]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'distro-dir:' \
  -l 'lib-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
  -l 'examples-dir:' \
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
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --installed-lib-dir)
        INSTALLED_LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --examples-dir)
        EXAMPLES_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR DISTRO_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done
HOME_DIR=${HOME_DIR:-/usr/lib/knox}
LIB_DIR=${LIB_DIR:-/usr/lib/knox/lib}
DEP_DIR=${DEP_DIR:-/usr/lib/knox/dep}
BIN_DIR=${BIN_DIR:-/usr/lib/knox/bin}
CONF_DIR=${CONF_DIR:-/etc/knox}
SAMPLES_DIR=${SAMPLES_DIR:-/usr/lib/knox/samples}
TEMPLATES_DIR=${TEMPLATES_DIR:-/usr/lib/knox/templates}
DATA_DIR=${DATA_DIR:-/var/lib/knox/data}
RUN_DIR=${RUN_DIR:-/var/run/knox}
LOG_DIR=${LOG_DIR:-/var/log/knox}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$DEP_DIR
install -d -m 0755 $PREFIX/$BIN_DIR
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$SAMPLES_DIR
install -d -m 0755 $PREFIX/$TEMPLATES_DIR
install -d -m 0755 $PREFIX/$DATA_DIR
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR

TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/target/*.*.*/knox-*.tar.gz -C $TMP_DIR

cp -ra $TMP_DIR/knox-*/lib/* ${PREFIX}/${LIB_DIR}
cp -ra $TMP_DIR/knox-*/dep/* ${PREFIX}/${DEP_DIR}
cp -ra $TMP_DIR/knox-*/bin/* ${PREFIX}/${BIN_DIR}
cp -ra $TMP_DIR/knox-*/conf/* ${PREFIX}/${CONF_DIR}
cp -ra $TMP_DIR/knox-*/samples ${PREFIX}/${SAMPLES_DIR}
cp -ra $TMP_DIR/knox-*/templates/* ${PREFIX}/${TEMPLATES_DIR}
cp -ra $TMP_DIR/knox-*/data/* ${PREFIX}/${DATA_DIR}

ln -s $CONF_DIR ${PREFIX}/$HOME_DIR/conf
ln -s $LOG_DIR ${PREFIX}/$HOME_DIR/logs
ln -s $DATA_DIR ${PREFIX}/$HOME_DIR/data
ln -s $RUN_DIR ${PREFIX}/$HOME_DIR/pids

rm -rf ${PREFIX}/README
rm -rf ${PREFIX}/native
rm -rf $TMP_DIR