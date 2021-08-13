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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/solr]
     --lib-dir=DIR               path to install bits [/usr/lib/solr]
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

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/solr}
LIB_DIR=${LIB_DIR:-/usr/lib/solr}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/solr}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/solr/conf}
DEFAULT_DIR=${ETC_DIR:-/etc/default}

VAR_DIR=$PREFIX/var

install -d -m 0755 $PREFIX/$LIB_DIR
cp -ra ${BUILD_DIR}/dist $PREFIX/$LIB_DIR/lib
# create symlink as configsets and post script reference 'dist' dir
ln -s $LIB_DIR/lib $PREFIX/$LIB_DIR/dist

install -d -m 0755 $PREFIX/$LIB_DIR/contrib
cp -ra ${BUILD_DIR}/contrib/* $PREFIX/$LIB_DIR/contrib

install -d -m 0755 $PREFIX/$LIB_DIR/server
cp -ra ${BUILD_DIR}/server/* $PREFIX/$LIB_DIR/server

install -d -m 0755 $PREFIX/$LIB_DIR/bin
cp -a ${BUILD_DIR}/bin/solr $PREFIX/$LIB_DIR/bin
cp -a ${BUILD_DIR}/bin/post $PREFIX/$LIB_DIR/bin
cp -a ${BUILD_DIR}/bin/oom_solr.sh $PREFIX/$LIB_DIR/bin
#cp -a ${BUILD_DIR}/server/scripts/cloud-scripts/*.sh $PREFIX/$LIB_DIR/bin
#cp -a $DISTRO_DIR/zkcli.sh $PREFIX/$LIB_DIR/bin
chmod 755 $PREFIX/$LIB_DIR/bin/*

install -d -m 0755 $PREFIX/$LIB_DIR/licenses
cp -a  ${BUILD_DIR}/licenses/* $PREFIX/$LIB_DIR/licenses

install -d -m 0755 $PREFIX/$DOC_DIR
cp -a  ${BUILD_DIR}/*.txt $PREFIX/$DOC_DIR
cp -ra ${BUILD_DIR}/docs/* $PREFIX/$DOC_DIR
cp -ra ${BUILD_DIR}/example/ $PREFIX/$DOC_DIR/

# Copy in the configuration files
install -d -m 0755 $PREFIX/$DEFAULT_DIR
cp $DISTRO_DIR/solr.default $PREFIX/$DEFAULT_DIR/solr
cp $DISTRO_DIR/solr.in.sh $PREFIX/$DEFAULT_DIR/solr.in.sh
install -d -m 0755 $PREFIX/${CONF_DIR}.dist
cp -a ${BUILD_DIR}/server/resources/* $PREFIX/${CONF_DIR}.dist

# Copy in the wrapper
cp -a ${DISTRO_DIR}/solrd $PREFIX/$LIB_DIR/bin/solrd
chmod 755 $PREFIX/$LIB_DIR/bin/solrd

# installing the only script that goes into /usr/bin
install -D -m 0755 $DISTRO_DIR/solrctl.sh $PREFIX/usr/bin/solrctl

# precreating /var layout
install -d -m 0755 $VAR_DIR/log/solr
install -d -m 0755 $VAR_DIR/run/solr
install -d -m 0755 $VAR_DIR/lib/solr
