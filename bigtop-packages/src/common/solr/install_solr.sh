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
     --bin-dir=DIR               path to install bins [/usr/bin]
     --man-dir=DIR               path to install mans [/usr/share/man]
     --etc-default=DIR           path to bigtop default dir [/etc/default]
     --lib-dir=DIR               path to install solr home [/usr/lib/solr]
     --var-dir=DIR               path to install solr contents [/var/lib/solr]
     --etc-solr=DIR              path to install solr conf [/etc/solr]
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
  -l 'bin-dir:' \
  -l 'man-dir:' \
  -l 'etc-default:' \
  -l 'lib-dir:' \
  -l 'var-dir:' \
  -l 'etc-solr:' \
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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --etc-default)
        ETC_DEFAULT=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --var-dir)
        VAR_DIR=$2 ; shift 2
        ;;
        --etc-solr)
        ETC_SOLR=$2 ; shift 2
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

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/solr}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DEFAULT=${ETC_DEFAULT:-/etc/default}
LIB_DIR=${LIB_DIR:-/usr/lib/solr}
VAR_DIR=${VAR_DIR:-/var/lib/solr}

ETC_SOLR=${ETC_SOLR:-/etc/solr}
# No prefix
NP_ETC_SOLR=/etc/solr

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
chmod 755 $PREFIX/$LIB_DIR/server/scripts/cloud-scripts/zkcli.sh
chmod 755 $PREFIX/$LIB_DIR/server/scripts/cloud-scripts/zkcli.bat
chmod 755 $PREFIX/$LIB_DIR/server/scripts/cloud-scripts/snapshotscli.sh

install -d -m 0755 $PREFIX/$LIB_DIR/licenses
cp -a  ${BUILD_DIR}/licenses/* $PREFIX/$LIB_DIR/licenses

install -d -m 0755 $PREFIX/$DOC_DIR
cp -a  ${BUILD_DIR}/*.txt $PREFIX/$DOC_DIR
cp -ra ${BUILD_DIR}/docs/* $PREFIX/$DOC_DIR
cp -ra ${BUILD_DIR}/example/ $PREFIX/$DOC_DIR/

# Copy in the configuration files
install -d -m 0755 $PREFIX/$ETC_DEFAULT
cp $DISTRO_DIR/solr.default $PREFIX/$ETC_DEFAULT/solr
cp $DISTRO_DIR/solr.in.sh $PREFIX/$ETC_DEFAULT/solr.in.sh
install -d -m 0755 $PREFIX/$NP_ETC_SOLR
install -d -m 0755 $PREFIX/$ETC_SOLR/conf.dist
cp -a ${BUILD_DIR}/server/resources/* $PREFIX/$ETC_SOLR/conf.dist

# Copy in the wrapper
cp -a ${DISTRO_DIR}/solrd $PREFIX/$LIB_DIR/bin/solrd
chmod 755 $PREFIX/$LIB_DIR/bin/solrd

# installing the only script that goes into /usr/bin
install -D -m 0755 $DISTRO_DIR/solrctl.sh $PREFIX/$BIN_DIR/solrctl

# precreating /var layout
install -d -m 0755 $PREFIX/$VAR_DIR
install -d -m 0755 $PREFIX/var/log/solr
install -d -m 0755 $PREFIX/var/run/solr
