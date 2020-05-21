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
     --distro-dir=DIR            path to distro specific files (debian/RPM)
     --build-dir=DIR             path to dist dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/elasticsearch]
     --lib-dir=DIR               path to install bits [/usr/lib/elasticsearch]
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
  -l 'doc-dir:' \
  -l 'lib-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
  -l 'initd-dir:' \
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
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --)
        shift; break
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

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/kibana}
LIB_DIR=${LIB_DIR:-/usr/lib/kibana}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DIR=${ETC_DIR:-/etc/kibana}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf.dist}
INITD_DIR=${INITD_DIR:-/etc/init.d}

VAR_DIR=$PREFIX/var

install -d -m 0755 $PREFIX/$MAN_DIR
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$ETC_DIR
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$BIN_DIR

# Extract files and copy to LIB_DIR
tar zxf $BUILD_DIR/kibana-*-linux-*64.tar.gz -C $PREFIX/$LIB_DIR --strip-components 1
chmod 755 $PREFIX/$LIB_DIR/* -R

# Copy configuration files
cp -a $PREFIX/$LIB_DIR/config/* $PREFIX/$CONF_DIR
ln -s $ETC_DIR/conf $PREFIX/$CONF_DIR

install -d -m 0755 $VAR_DIR/log/kibana
install -d -m 0755 $VAR_DIR/run/kibana
install -d -m 0755 $VAR_DIR/lib/kibana
