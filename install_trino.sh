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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/trino]
     --lib-dir=DIR               path to install trino home [/usr/lib/trino]
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
  -l 'trino-version:' \
  -l 'source-dir:' \
  -l 'cli-dir:' \
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
        --cli-dir)
        CLI_BUILD_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR SOURCE_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/trino}
CLI_DIR=${CLI_DIR:-/usr/lib/trino-cli}
LIB_DIR=${TRINO_DIR:-/usr/lib/trino}
VAR_DIR=${VAR_DIR:-/var/lib/trino}
LOG_DIR=${LOG_DIR:-/var/log/trino}
RUN_DIR=${RUN_DIR:-/var/run/trino}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/trino}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/trino}
CONF_DIST_DIR=${CONF_DIST_DIR:-/etc/trino.dist}
DEFAULT_DIR=${DEFAULT_DIR:-/etc/default}

install -d -m 0755 $PREFIX/$CONF_DIST_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$CLI_DIR
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$VAR_DIR
install -d -m 0755 $PREFIX/$LOG_DIR
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$DEFAULT_DIR

cp -ra ${BUILD_DIR}/* $PREFIX/$LIB_DIR/
cp -ra ${CLI_BUILD_DIR}/* $PREFIX/$CLI_DIR/

chmod +x $PREFIX/$LIB_DIR/bin/launcher
