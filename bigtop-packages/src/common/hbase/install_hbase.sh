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
     --build-dir=DIR             path to hbase dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hbase]
     --lib-dir=DIR               path to install hbase home [/usr/lib/hbase]
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

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/hbase}
LIB_DIR=${LIB_DIR:-/usr/lib/hbase}
BIN_DIR=${BIN_DIR:-/usr/lib/hbase/bin}
ETC_DIR=${ETC_DIR:-/etc/hbase}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$BIN_DIR
install -d -m 0755 $PREFIX/$ETC_DIR
install -d -m 0755 $PREFIX/$MAN_DIR

cp -ra lib/* ${PREFIX}/${LIB_DIR}/lib/
cp hbase*.jar $PREFIX/$LIB_DIR
cp -a docs/* $PREFIX/$DOC_DIR
cp *.txt $PREFIX/$DOC_DIR/
cp -a hbase-webapps $PREFIX/$LIB_DIR

cp -a conf $PREFIX/$ETC_DIR/conf
cp -a bin/* $PREFIX/$BIN_DIR

ln -s $ETC_DIR/conf $PREFIX/$LIB_DIR/conf

wrapper=$PREFIX/usr/bin/hbase
mkdir -p `dirname $wrapper`
cat > $wrapper <<EOF
#!/bin/sh

. /etc/default/hadoop
. /etc/default/hbase

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

export HADOOP_CONF=\${HADOOP_CONF:-/etc/hadoop/conf}
export ZOOKEEPER_HOME=\${ZOOKEEPER_HOME:-/usr/lib/zookeeper}
export HBASE_CLASSPATH=\$HADOOP_CONF:\$HADOOP_HOME/*:\$HADOOP_HOME/lib/*:\$ZOOKEEPER_HOME/*:\$ZOOKEEPER_HOME/lib/*:\$HBASE_CLASSPATH

exec /usr/lib/hbase/bin/hbase "\$@"
EOF
chmod 755 $wrapper

install -d -m 0755 $PREFIX/usr/bin
