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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/kafka]
     --lib-dir=DIR               path to install Kafka home [/usr/lib/kafka]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --examples-dir=DIR          path to install examples [doc-dir/examples]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'doc-dir:' \
  -l 'lib-dir:' \
  -l 'installed-lib-dir:' \
  -l 'source-dir:' \
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
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --source-dir)
        SOURCE_DIR=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR SOURCE_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/kafka}
LIB_DIR=${LIB_DIR:-/usr/lib/kafka}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
CONF_DIR=${CONF_DIR:-/etc/kafka/conf.dist}
KAFKA_HOME=${KAFKA_HOME:-/usr/lib/kafka}
ETC_KAFKA_DIR=${ETC_KAFKA_DIR:-/etc/kafka}
BIN_DIR=/usr/bin
INSTALLED_KAFKA_DIR=${LIB_DIR}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$EXAMPLES_DIR

install -d -m 0755 $PREFIX/var/lib/kafka/
install -d -m 0755 $PREFIX/var/log/kafka/
install -d -m 0755 $PREFIX/var/run/kafka/

# Create the data directory.
install -d -m 0755 $PREFIX/var/lib/kafka/data

# Unzip binary tarball contents to lib directory.
for jar in `ls ${BUILD_DIR}/build/kafka*.tgz | grep -v 'site-docs'`; do
    base=`basename $jar`
    tar zxf ${BUILD_DIR}/build/${base} -C ${PREFIX}/${LIB_DIR} --strip 1
done
rm -f ${PREFIX}/${LIB_DIR}/{LICENSE,NOTICE}
rm -rf ${PREFIX}/${LIB_DIR}/site-docs

# Site docs
tar zxf ${BUILD_DIR}/build/kafka*site-docs.tgz -C ${PREFIX}/${DOC_DIR} 

#Remove config directory. Creating symlink below.
rm -rf ${PREFIX}/${LIB_DIR}/config

#Removing:
# - javadoc scaladoc source jars from under libs.
# - bat files from under bin
rm -rf ${PREFIX}/${LIB_DIR}/libs/kafka_*source*
rm -rf ${PREFIX}/${LIB_DIR}/libs/kafka_*javadoc*
rm -rf ${PREFIX}/${LIB_DIR}/libs/kafka_*scaladoc*
rm -rf ${PREFIX}/${LIB_DIR}/bin/windows

chmod 755 $PREFIX/$LIB_DIR/bin/*

# Exposing a few scripts by placing them under /usr/bin
install -d -m 0755 $PREFIX/$BIN_DIR
for file in kafka-console-consumer.sh kafka-console-producer.sh kafka-run-class.sh kafka-topics.sh
do
  wrapper=$PREFIX/$BIN_DIR/$file
  cat >>$wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $INSTALLED_KAFKA_DIR/bin/$file "\$@"
EOF
  chmod 755 $wrapper
done

# Copy in the configuration files
install -d -m 0755 $PREFIX/$CONF_DIR
cp -a ${BUILD_DIR}/config/{tools-log4j.properties,server.properties,log4j.properties} $PREFIX/$CONF_DIR/
# BIGTOP-1886
sed -i -e "s|=log-cleaner.log|=$\{kafka.logs.dir\}/log-cleaner.log|" $PREFIX/$CONF_DIR/log4j.properties
ln -s ${ETC_KAFKA_DIR}/conf $PREFIX/$LIB_DIR/config

# Creating symlink to /var/log/kafka
ln -s /var/log/kafka ${PREFIX}/$LIB_DIR/logs

# Removing zookeeper*.jar from under libs and dropping a symlink in place
rm -f ${PREFIX}/${LIB_DIR}/libs/zookeeper-*.jar
ln -sf /usr/lib/zookeeper/zookeeper.jar ${PREFIX}/${LIB_DIR}/libs/

# BIGTOP-3712
ln -s /usr/lib/zookeeper/zookeeper-jute.jar ${PREFIX}/${LIB_DIR}/libs/

# Copy in the defaults file
install -d -m 0755 ${PREFIX}/etc/default
cp ${SOURCE_DIR}/kafka.default ${PREFIX}/etc/default/kafka

cp ${BUILD_DIR}/{LICENSE,NOTICE} ${PREFIX}/${LIB_DIR}/
