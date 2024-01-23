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


usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to dinky dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install dinky home [/usr/lib/dinky]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --etc-dinky=DIR             path to install dinky conf [/etc/dinky]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'build-dir:' \
  -l 'bin-dir:' \
  -l 'lib-dir:' \
  -l 'etc-dinky:' -- "$@")

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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --etc-dinky)
        ETC_DINKY=$2 ; shift 2
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

LIB_DIR=${LIB_DIR:-/usr/lib/dinky}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DINKY=${ETC_DINKY:-/etc/dinky}
RUN_DIR=${RUN_DIR:-/var/run/dinky}
LOG_DIR=${LOG_DIR:-/var/log/dinky}

NP_ETC_DINKY=/etc/dinky
NP_VAR_LIB_DINKY_DATA=/var/lib/dinky/data


install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/extends
install -d -m 0755 $PREFIX/$LIB_DIR/jar
install -d -m 0755 $PREFIX/$LIB_DIR/sql
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/dink-loader
install -d -m 0755 $PREFIX/$LIB_DIR/config

install -d -m 0755 $PREFIX/$NP_ETC_DINKY
install -d -m 0755 $PREFIX/$ETC_DINKY/conf.dist
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR

TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/build/dinky-release*.tar.gz -C $TMP_DIR

cp -ra ${TMP_DIR}/dinky-*/extends/* ${PREFIX}/${LIB_DIR}/extends/
cp -ra ${TMP_DIR}/dinky-*/jar/* ${PREFIX}/${LIB_DIR}/jar/
cp -ra ${TMP_DIR}/dinky-*/lib/* ${PREFIX}/${LIB_DIR}/lib/
cp -ra ${TMP_DIR}/dinky-*/sql/* ${PREFIX}/${LIB_DIR}/sql/
cp -ra ${TMP_DIR}/dinky-*/dink-loader/* ${PREFIX}/${LIB_DIR}/dink-loader/
cp -ra ${TMP_DIR}/dinky-*/config/* $PREFIX/$ETC_DINKY/conf.dist/
cp -ra ${TMP_DIR}/dinky-*/auto.sh ${PREFIX}/${LIB_DIR}/auto.sh

ln -s $NP_ETC_DINKY/conf $PREFIX/$LIB_DIR/config
ln -s $LOG_DIR $PREFIX/$LIB_DIR/logs
ln -s $RUN_DIR $PREFIX/$LIB_DIR/run

rm -rf $TMP_DIR
