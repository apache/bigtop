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
     --build-dir=DIR             path to livy assembly
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install livy home [/usr/lib/livy]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'lib-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
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
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --installed-lib-dir)
        INSTALLED_LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

LIB_DIR=${LIB_DIR:-/usr/lib/livy}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/livy}
CONF_DIR=${CONF_DIR:-/etc/livy/conf.dist}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/jars
install -d -m 0755 $PREFIX/$LIB_DIR/repl_2.12-jars
install -d -m 0755 $PREFIX/$LIB_DIR/rsc-jars
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/var/log/livy
install -d -m 0755 $PREFIX/var/run/livy
install -d -m 0755 $PREFIX/var/lib/livy

# Copy the jar files
cp -ra ${BUILD_DIR}/jars/* $PREFIX/${LIB_DIR}/jars/
cp -ra ${BUILD_DIR}/repl_2.12-jars/* $PREFIX/${LIB_DIR}/repl_2.12-jars/
cp -ra ${BUILD_DIR}/rsc-jars/* $PREFIX/${LIB_DIR}/rsc-jars/

# Copy the bin files
cp -a ${BUILD_DIR}/bin/* $PREFIX/${LIB_DIR}/bin

# Copy the configuration files
cp -a ${BUILD_DIR}/conf/* $PREFIX/$CONF_DIR
ln -s /etc/livy/conf $PREFIX/$LIB_DIR/conf
