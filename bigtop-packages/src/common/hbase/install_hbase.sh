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
     --build-dir=DIR             path to hbase dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hbase]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --lib-dir=DIR               path to install hbase home [/usr/lib/hbase]
     --man-dir=DIR               path to install mans [/usr/share/man]
     --etc-default=DIR           path to bigtop default dir [/etc/default]
     --etc-hbase=DIR             path to install hbase conf [/etc/hbase]
     --lib-zookeeper-dir=DIR     path to Zookeeper home [/usr/lib/zookeeper]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'doc-dir:' \
  -l 'bin-dir:' \
  -l 'lib-dir:' \
  -l 'man-dir:' \
  -l 'etc-default:' \
  -l 'etc-hbase:' \
  -l 'lib-zookeeper-dir:' \
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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --etc-default)
        ETC_DEFAULT=$2 ; shift 2
        ;;
        --etc-hbase)
        ETC_HBASE=$2 ; shift 2
        ;;
        --lib-zookeeper-dir)
        LIB_ZOOKEEPER_DIR=$2 ; shift 2
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/hbase}
LIB_DIR=${LIB_DIR:-/usr/lib/hbase}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DEFAULT=${ETC_DEFAULT:-/etc/default}
LIB_ZOOKEEPER_DIR=${LIB_ZOOKEEPER_DIR:-/usr/lib/zookeeper}

ETC_HBASE=${ETC_HBASE:-/etc/hbase}
# No prefix
NP_ETC_HBASE=/etc/hbase

ETC_DIR=${ETC_DIR:-/etc/hbase}
THRIFT_DIR=${THRIFT_DIR:-${LIB_DIR}/include/thrift}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$NP_ETC_HBASE
install -d -m 0755 $PREFIX/$ETC_HBASE
install -d -m 0755 $PREFIX/$MAN_DIR
install -d -m 0755 $PREFIX/$THRIFT_DIR

cp -ra $BUILD_DIR/lib/* ${PREFIX}/${LIB_DIR}/lib/
cp $BUILD_DIR/lib/hbase*.jar $PREFIX/$LIB_DIR
cp -a $BUILD_DIR/docs/* $PREFIX/$DOC_DIR
cp $BUILD_DIR/*.txt $PREFIX/$DOC_DIR/
cp -a $BUILD_DIR/hbase-webapps $PREFIX/$LIB_DIR

cp -a $BUILD_DIR/conf $PREFIX/$ETC_HBASE/conf.dist
cp -a $BUILD_DIR/bin/* $PREFIX/$LIB_DIR/bin
# Purge scripts that don't work with packages
for file in rolling-restart.sh graceful_stop.sh local-regionservers.sh \
            master-backup.sh regionservers.sh zookeepers.sh hbase-daemons.sh \
            start-hbase.sh stop-hbase.sh local-master-backup.sh ; do
  rm -f $PREFIX/$LIB_DIR/bin/$file
done

cp $BUILD_DIR/../hbase-thrift/src/main/resources/org/apache/hadoop/hbase/thrift/Hbase.thrift $PREFIX/$THRIFT_DIR/hbase1.thrift
cp $BUILD_DIR/../hbase-thrift/src/main/resources/org/apache/hadoop/hbase/thrift2/hbase.thrift $PREFIX/$THRIFT_DIR/hbase2.thrift

ln -s $NP_ETC_HBASE/conf $PREFIX/$LIB_DIR/conf

# Make a symlink of hbase.jar to hbase-version.jar
pushd `pwd`
cd $PREFIX/$LIB_DIR
for i in `ls hbase*jar | grep -v tests.jar`
do
    ln -s $i `echo $i | sed -n 's/\(.*\)\(-[0-9].*\)\(.jar\)/\1\3/p'`
done
popd

wrapper=$PREFIX/$BIN_DIR/hbase
mkdir -p `dirname $wrapper`
cat > $wrapper <<EOF
#!/bin/bash

BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-$ETC_DEFAULT}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/hbase ] && . \${BIGTOP_DEFAULTS_DIR}/hbase

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_CONF=\${HADOOP_CONF:-/etc/hadoop/conf}
export ZOOKEEPER_HOME=\${ZOOKEEPER_HOME:-$LIB_ZOOKEEPER_DIR}
export HBASE_CLASSPATH=\$HADOOP_CONF:\$HADOOP_HOME/*:\$HADOOP_HOME/lib/*:\$ZOOKEEPER_HOME/*:\$HBASE_CLASSPATH

exec $LIB_DIR/bin/hbase "\$@"
EOF
chmod 755 $wrapper

install -d -m 0755 $PREFIX/$BIN_DIR

rm -f $PREFIX/$ETC_HBASE/conf.dist/*.cmd
rm -f $PREFIX/$LIB_DIR/bin/*.cmd

# Install default wrapper
install -d -m 0755 $PREFIX/$ETC_DEFAULT
default_hbase_wrapper=$PREFIX/$ETC_DEFAULT/hbase
cat > $default_hbase_wrapper << EOF
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

export HBASE_HOME="${LIB_DIR}"
export HBASE_CONF_DIR="/etc/hbase/conf"

export HBASE_PID_DIR="/var/run/hbase"
export HBASE_LOG_DIR="/var/log/hbase"
export HBASE_IDENT_STRING=hbase

# Up to 100 region servers can be run on a single host by specifying offsets
# here or as CLI args when using init scripts. Each offset identifies an
# instance and is used to determine the network ports it uses. Each instance
# will have have its own log and pid files.
#
# REGIONSERVER_OFFSETS="1 2 3"

#
# Set the starting port to be assigned for HBASE RS JMX monitoring when  
# running multiple region servers on a node. Each RS will be assigned a JMX port
# which will be equal to starting JMX port + offset
# 
# export JMXPORT=10103

#
# Set a directory which will be used for Java temp directory. For multi RS the
# directory will be appended with offset to make it unique for each JVM process
#
# export JAVA_TMP_DIR="/tmp/java_tmp_dir"

EOF

chmod 644 $default_hbase_wrapper