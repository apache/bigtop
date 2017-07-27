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
     --build-dir=DIR             path to dist.dir
     --source-dir=DIR            path to package shared files dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/zeppelin]
     --lib-dir=DIR               path to install Zeppelin home [/usr/lib/zeppelin]
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
  -l 'doc-dir:' \
  -l 'lib-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
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
        --doc-dir)
        DOC_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR SOURCE_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/zeppelin}
LIB_DIR=${LIB_DIR:-/usr/lib/zeppelin}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/zeppelin}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/zeppelin/conf.dist}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$DOC_DIR

install -d -m 0755 $PREFIX/var/lib/zeppelin/
install -d -m 0755 $PREFIX/var/lib/zeppelin/notebook/
install -d -m 0755 $PREFIX/var/log/zeppelin/
install -d -m 0755 $PREFIX/var/run/zeppelin/
install -d -m 0755 $PREFIX/var/run/zeppelin/webapps

cp -a ${BUILD_DIR}/build/dist/{bin,interpreter,lib,zeppelin-web-${ZEPPELIN_VERSION}.war} $PREFIX/$LIB_DIR/
cp -a ${BUILD_DIR}/build/dist/{LICENSE,NOTICE,README.md,licenses} $PREFIX/$DOC_DIR
cp -a ${BUILD_DIR}/build/dist/conf/* $PREFIX/$CONF_DIR
cp -a ${BUILD_DIR}/build/dist/notebook $PREFIX/var/lib/zeppelin/

rm -f $PREFIX/$LIB_DIR/bin/*.cmd
chmod 755 $PREFIX/$LIB_DIR/bin/*

rm -f $PREFIX/$CONF_DIR/*.cmd.*
cp -a ${SOURCE_DIR}/zeppelin-env.sh $PREFIX/$CONF_DIR
ln -s /etc/zeppelin/conf $PREFIX/$LIB_DIR/conf
