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
     --build-dir=DIR             path to flink dist.dir
     --source-dir=DIR            path to package shared files dir
     --prefix=PREFIX             path to install into

  Optional options:
     --lib-dir=DIR               path to install flink home [/usr/lib/flink]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --lib-hadoop=DIR            path to hadoop home [/usr/lib/hadoop]
     --etc-flink=DIR             path to install flink conf [/etc/flink]
     --etc-hadoop=DIR            path to hadoop conf [/etc/hadoop]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'lib-dir:' \
  -l 'bin-dir:' \
  -l 'lib-hadoop:' \
  -l 'etc-flink:' \
  -l 'etc-hadoop:' \
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
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-hadoop)
        LIB_HADOOP=$2 ; shift 2
        ;;
        --etc-flink)
        ETC_FLINK=$2 ; shift 2
        ;;
        --etc-hadoop)
        ETC_HADOOP=$2 ; shift 2
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

# load bigtop component versions
if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi


LIB_DIR=${LIB_DIR:-/usr/lib/flink}
BIN_DIR=${BIN_DIR:-/usr/bin}
LIB_HADOOP=${LIB_HADOOP:-/usr/lib/hadoop}

ETC_FLINK=${ETC_FLINK:-/etc/flink}
ETC_HADOOP=${ETC_HADOOP:-/etc/hadoop}
# No prefix
NP_ETC_FLINK=/etc/flink

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/examples
install -d -m 0755 $PREFIX/$LIB_DIR/opt
install -d -m 0755 $PREFIX/$LIB_DIR/plugins
install -d -m 0755 $PREFIX/$NP_ETC_FLINK
install -d -m 0755 $PREFIX/$ETC_FLINK/conf.dist
install -d -m 0755 $PREFIX/var/log/flink
install -d -m 0755 $PREFIX/var/log/flink-cli
install -d -m 0755 $PREFIX/var/run/flink

cp -ra ${BUILD_DIR}/lib/* $PREFIX/${LIB_DIR}/lib/
cp -a ${BUILD_DIR}/bin/* $PREFIX/${LIB_DIR}/bin/
# delete Windows start scripts
rm -rf $PREFIX/${LIB_DIR}/bin/*.cmd
# remove log directory
rm -rf  $PREFIX/${LIB_DIR}/log

# Copy the configuration files
cp -a ${BUILD_DIR}/conf/* $PREFIX/$ETC_FLINK/conf.dist
ln -s $NP_ETC_FLINK/conf $PREFIX/$LIB_DIR/conf

cp -ra ${BUILD_DIR}/examples/* $PREFIX/${LIB_DIR}/examples/

cp -ra ${BUILD_DIR}/opt/* $PREFIX/${LIB_DIR}/opt/
cp -ra ${BUILD_DIR}/plugins/* $PREFIX/${LIB_DIR}/plugins/

cp ${BUILD_DIR}/{LICENSE,README.txt} ${PREFIX}/${LIB_DIR}/

# Copy in the /usr/bin/flink wrapper
install -d -m 0755 $PREFIX/$BIN_DIR
cat > $PREFIX/$BIN_DIR/flink <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_HOME=\${HADOOP_HOME:-$LIB_HADOOP}
export HADOOP_CONF_DIR=\${HADOOP_CONF_DIR:-$ETC_HADOOP/conf}
export FLINK_HOME=\${FLINK_HOME:-$LIB_DIR}
export FLINK_CONF_DIR=\${FLINK_CONF_DIR:-$ETC_FLINK/conf.dist}
export FLINK_LOG_DIR=\${FLINK_LOG_DIR:-/var/log/flink-cli}

exec $LIB_DIR/bin/flink "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/flink
