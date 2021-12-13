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
  -l 'build-dir:' \
  -l 'man-dir:' \
  -l 'bin-dir:' \
  -l 'doc-dir:' \
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
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
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

BIN_DIR=${BIN_DIR:-/usr/lib/bigtop-utils}
DOC_DIR=${DOC_DIR:-/usr/share/doc/bigtop-jsvc}

#libexec
install -d -m 0755 ${PREFIX}/${BIN_DIR}
cp ${BUILD_DIR}/jsvc ${PREFIX}/${BIN_DIR}

# docs
install -d -m 0755 ${PREFIX}/${DOC_DIR}
cp ${BUILD_DIR}/*.txt  ${PREFIX}/${DOC_DIR}/
