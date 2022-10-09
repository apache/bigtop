#!/bin/sh

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
     --build-dir=DIR             path to tez dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --man-dir=DIR               path to install mans [/usr/share/man]
     --doc-dir=DIR               path to install docs into [/usr/share/doc/tez]
     --lib-dir=DIR               path to install tez home [/usr/lib/tez]
     --etc-tez=DIR               path to install tez conf [/etc/tez]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'man-dir:' \
  -l 'doc-dir:' \
  -l 'lib-dir:' \
  -l 'etc-tez:' \
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
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --etc-tez)
        ETC_TEZ=$2 ; shift 2
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

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/tez}
LIB_DIR=${LIB_DIR:-/usr/lib/tez}

ETC_TEZ=${ETC_TEZ:-/etc/tez}
# No prefix
NP_ETC_TEZ=/etc/tez

install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c tez.1 > $PREFIX/$MAN_DIR/tez.1.gz

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$NP_ETC_TEZ
install -d -m 0755 $PREFIX/$ETC_TEZ/conf.dist
install -d -m 0755 $PREFIX/$MAN_DIR

tar -C $PREFIX/$LIB_DIR -xzf $BUILD_DIR/tez-dist/target/tez*-minimal.tar.gz

cp tez-site.xml $PREFIX/$ETC_TEZ/conf.dist/
ln -s $NP_ETC_TEZ/conf $PREFIX/$LIB_DIR/conf

TEZ_TAR=$BUILD_DIR/tez-dist/target/tez-[[:digit:]]*[[:digit:]].tar.gz
cp $TEZ_TAR $PREFIX/$LIB_DIR/lib/tez.tar.gz

