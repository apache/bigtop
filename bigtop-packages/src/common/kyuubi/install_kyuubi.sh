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
     --build-dir=DIR             path to kyuubi dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install kyuubi home [/usr/lib/kyuubi]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --etc-kyuubi=DIR             path to install kyuubi conf [/etc/kyuubi]
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
  -l 'etc-kyuubi:' -- "$@")

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
        --etc-kyuubi)
        ETC_KNOX=$2 ; shift 2
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

LIB_DIR=${LIB_DIR:-/usr/lib/kyuubi}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_KNOX=${ETC_KNOX:-/etc/kyuubi}
RUN_DIR=${RUN_DIR:-/var/run/kyuubi}
LOG_DIR=${LOG_DIR:-/var/log/kyuubi}

NP_ETC_KNOX=/etc/kyuubi

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/beeline-jars
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/charts
install -d -m 0755 $PREFIX/$LIB_DIR/db-scripts
install -d -m 0755 $PREFIX/$LIB_DIR/docker
install -d -m 0755 $PREFIX/$LIB_DIR/extension
install -d -m 0755 $PREFIX/$LIB_DIR/externals
install -d -m 0755 $PREFIX/$LIB_DIR/jars
install -d -m 0755 $PREFIX/$LIB_DIR/web-ui
install -d -m 0755 $PREFIX/$LIB_DIR/work


install -d -m 0755 $PREFIX/$NP_ETC_KNOX
install -d -m 0755 $PREFIX/$ETC_KNOX/conf.dist
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR


TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/apache-kyuubi-*-bin.tar.gz -C $TMP_DIR

cp -ra ${TMP_DIR}/apache-kyuubi-*-bin/beeline-jars/* ${PREFIX}/${LIB_DIR}/beeline-jars/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/bin/* ${PREFIX}/${LIB_DIR}/bin/
cp -ra ${TMP_DIR}/apache-kyuubi-*-bin/charts/* ${PREFIX}/${LIB_DIR}/charts/
cp -ra ${TMP_DIR}/apache-kyuubi-*-bin/db-scripts/* ${PREFIX}/${LIB_DIR}/db-scripts/
cp -ra ${TMP_DIR}/apache-kyuubi-*-bin/docker/* ${PREFIX}/${LIB_DIR}/docker/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/extension/* ${PREFIX}/${LIB_DIR}/extension/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/externals/* ${PREFIX}/${LIB_DIR}/externals/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/jars/* ${PREFIX}/${LIB_DIR}/jars/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/web-ui/* ${PREFIX}/${LIB_DIR}/web-ui/
cp -a ${TMP_DIR}/apache-kyuubi-*-bin/work/* ${PREFIX}/${LIB_DIR}/work/

cp -ra ${TMP_DIR}/apache-kyuubi-*-bin/conf/* ${PREFIX}/${ETC_KNOX}/conf.dist

ln -s $NP_ETC_KNOX/conf $PREFIX/$LIB_DIR/conf
ln -s $LOG_DIR $PREFIX/$LIB_DIR/logs
ln -s $RUN_DIR $PREFIX/$LIB_DIR/pids


rm -rf $TMP_DIR
