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
     --build-dir=DIR             path to crunch build dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install crunch home [/usr/lib/crunch]
     --doc-dir=DIR               path to install crunch docs [/usr/share/doc/crunch]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'lib-dir:' \
  -l 'doc-dir:' \
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

for var in PREFIX BUILD_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

LIB_DIR=${LIB_DIR:-/usr/lib/crunch}
DOC_DIR=${DOC_DIR:-/usr/share/doc/crunch}

# First we'll move everything into lib
install -d -m 0755 $PREFIX/$LIB_DIR
cp $BUILD_DIR/*.jar $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
cp $BUILD_DIR/lib/*.jar $PREFIX/$LIB_DIR/lib

# Now take care of the docs
install -d -m 0755 $PREFIX/$DOC_DIR
cp $BUILD_DIR/LICENSE $PREFIX/$DOC_DIR
cp $BUILD_DIR/NOTICE $PREFIX/$DOC_DIR
cp $BUILD_DIR/README $PREFIX/$DOC_DIR
cp -r $BUILD_DIR/site/* $PREFIX/$DOC_DIR

# Finally move some file from lib to docs
mv $PREFIX/$LIB_DIR/crunch-examples-*.jar $PREFIX/$DOC_DIR
mv $PREFIX/$LIB_DIR/crunch-test-*.jar $PREFIX/$DOC_DIR

rm -f $PREFIX/$LIB_DIR/lib/zookeeper-*.jar
rm -f $PREFIX/$LIB_DIR/lib/hadoop-*.jar

