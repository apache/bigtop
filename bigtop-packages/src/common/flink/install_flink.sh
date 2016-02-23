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
     --prefix=PREFIX             path to install into
  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/flink]
     --lib-dir=DIR               path to install flink home [/usr/lib/flink]
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

for var in PREFIX BUILD_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

#MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/flink}
LIB_DIR=${LIB_DIR:-/usr/lib/flink}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/flink}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/flink/conf.dist}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$DOC_DIR

cp -ra ${BUILD_DIR}/lib/* $PREFIX/${LIB_DIR}/lib/
#cp ${BUILD_DIR}/lib/flink*.jar $PREFIX/$LIB_DIR
cp -a ${BUILD_DIR}/*.txt $PREFIX/$DOC_DIR
cp -a ${BUILD_DIR}/bin/* $PREFIX/${LIB_DIR}/bin
rm -rf $PREFIX/${LIB_DIR}/bin/*.cmd

# Copy in the configuration files
install -d -m 0755 $PREFIX/$CONF_DIR
cp -a ${BUILD_DIR}/conf/* $PREFIX/$CONF_DIR
ln -s /etc/flink/conf $PREFIX/$LIB_DIR/conf

# Copy in the example files
cp -a ${BUILD_DIR}/examples/ $PREFIX/$DOC_DIR/
#cp -a ${BUILD_DIR}/h2o/ $PREFIX/$DOC_DIR/
 
# Copy in the /usr/bin/flink wrapper
install -d -m 0755 $PREFIX/$BIN_DIR
cat > $PREFIX/$BIN_DIR/flink <<EOF
#!/bin/bash
# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome
# FIXME: flink-994
export HADOOP_HOME=\${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=\${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export FLINK_HOME=\${FLINK_HOME:-$INSTALLED_LIB_DIR}
export FLINK_CONF_DIR=\${FLINK_CONF_DIR:-$CONF_DIR}
# FIXME: the following line is a workaround for BIGTOP-259 
export HADOOP_CLASSPATH="`echo /usr/lib/flink/flink-examples-*-job.jar`":\$HADOOP_CLASSPATH
exec $INSTALLED_LIB_DIR/bin/flink "\$@"
#export SPARK_HOME=\${SPARK_HOME:-/usr/lib/spark}
EOF
chmod 755 $PREFIX/$BIN_DIR/flink

