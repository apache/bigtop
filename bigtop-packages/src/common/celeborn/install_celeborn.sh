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
     --build-dir=DIR             path to celeborn dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install celeborn home [/usr/lib/celeborn]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --etc-celeborn=DIR          path to install celeborn conf [/etc/celeborn]
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
  -l 'etc-celeborn:' -- "$@")

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
        --etc-celeborn)
        ETC_CELEBORN=$2 ; shift 2
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

LIB_DIR=${LIB_DIR:-/usr/lib/celeborn}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_CELEBORN=${ETC_CELEBORN:-/etc/celeborn}
RUN_DIR=${RUN_DIR:-/var/run/celeborn}
LOG_DIR=${LOG_DIR:-/var/log/celeborn}

NP_ETC_CELEBORN=/etc/celeborn



install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/charts
install -d -m 0755 $PREFIX/$LIB_DIR/docker
install -d -m 0755 $PREFIX/$LIB_DIR/flink
install -d -m 0755 $PREFIX/$LIB_DIR/jars
install -d -m 0755 $PREFIX/$LIB_DIR/master-jars
install -d -m 0755 $PREFIX/$LIB_DIR/mr
install -d -m 0755 $PREFIX/$LIB_DIR/sbin
install -d -m 0755 $PREFIX/$LIB_DIR/spark
install -d -m 0755 $PREFIX/$LIB_DIR/worker-jars

install -d -m 0755 $PREFIX/$NP_ETC_CELEBORN
install -d -m 0755 $PREFIX/$ETC_CELEBORN/conf.dist
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR

TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/apache-celeborn-*-bin.tgz -C $TMP_DIR

cp -ra ${TMP_DIR}/apache-celeborn-*-bin/bin/* ${PREFIX}/${LIB_DIR}/bin/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/charts/* ${PREFIX}/${LIB_DIR}/charts/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/docker/* ${PREFIX}/${LIB_DIR}/docker/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/flink/* ${PREFIX}/${LIB_DIR}/flink/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/jars/* ${PREFIX}/${LIB_DIR}/jars/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/master-jars/* ${PREFIX}/${LIB_DIR}/master-jars/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/mr/* ${PREFIX}/${LIB_DIR}/mr/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/sbin/* ${PREFIX}/${LIB_DIR}/sbin/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/spark/* ${PREFIX}/${LIB_DIR}/spark/
cp -ra ${TMP_DIR}/apache-celeborn-*-bin/worker-jars/* ${PREFIX}/${LIB_DIR}/worker-jars/

cp -ra ${TMP_DIR}/apache-celeborn-*-bin/conf/* $PREFIX/$ETC_CELEBORN/conf.dist/


ln -s $NP_ETC_CELEBORN/conf $PREFIX/$LIB_DIR/conf
ln -s $LOG_DIR $PREFIX/$LIB_DIR/logs
ln -s $RUN_DIR $PREFIX/$LIB_DIR/run

rm -rf $TMP_DIR
