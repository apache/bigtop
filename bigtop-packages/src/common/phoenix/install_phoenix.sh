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
     --build-dir=DIR             path to phoenix dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/phoenix]
     --lib-dir=DIR               path to install phoenix home [/usr/lib/phoenix]
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
  -l 'conf-dir:' \
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
        --conf-dir)
        CONF_DIR=$2 ; shift 2
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/phoenix}
LIB_DIR=${LIB_DIR:-/usr/lib/phoenix}
BIN_DIR=${BIN_DIR:-/usr/lib/phoenix/bin}
PHERF_DIR=${PHERF_DIR:-/usr/lib/phoenix/pherf}
ETC_DIR=${ETC_DIR:-/etc/phoenix}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf.dist}

install -d -m 0755 $PREFIX/$BIN_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$MAN_DIR
install -d -m 0755 $PREFIX/$ETC_DIR
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$PHERF_DIR
install -d -m 0755 $PREFIX/var/lib/phoenix
install -d -m 0755 $PREFIX/var/log/phoenix

cp -ra $BUILD_DIR/{bin,lib} $PREFIX/$LIB_DIR/
rm $PREFIX/$LIB_DIR/lib/phoenix-*.jar
chmod 755 $PREFIX/$BIN_DIR/*.py
cp -a $BUILD_DIR/*.jar $PREFIX/$LIB_DIR/
cp -a $BUILD_DIR/lib/phoenix-*.jar $PREFIX/$LIB_DIR/
# Remove sources jar
rm $PREFIX/$LIB_DIR/phoenix-*-sources.jar
cp -a $BUILD_DIR/examples $PREFIX/$DOC_DIR

# Remove the executable bit from jars to avoid lintian warnings
find $PREFIX/$LIB_DIR -name '*.jar' -exec chmod a-x {} \;

# Create version independent symlinks
# phoenix-client for clients like sqlline
ln -s `cd $PREFIX/$LIB_DIR ; ls phoenix*-client.jar | grep -v thin` $PREFIX/$LIB_DIR/phoenix-client.jar

# phoenix-thin-client
ln -s `cd $PREFIX/$LIB_DIR ; ls phoenix*-thin-client.jar` $PREFIX/$LIB_DIR/phoenix-thin-client.jar

# phoenix-server for placing on the HBase regionserver classpath
ln -s `cd $PREFIX/$LIB_DIR ; ls phoenix*-server.jar` $PREFIX/$LIB_DIR/phoenix-server.jar

# Pherf
unzip $BUILD_DIR/phoenix-pherf/phoenix-pherf-*-cluster.zip -d $BUILD_DIR/phoenix-pherf/
mv $BUILD_DIR/phoenix-pherf/phoenix-pherf-*-cluster/* $PREFIX/$PHERF_DIR/
rm -rf $PREFIX/$PHERF_DIR/config/env.sh

cat >>$PREFIX/$PHERF_DIR/config/env.sh <<EOF
#!/bin/sh

#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Required variable to point to Java installation
#JAVA_HOME=

# Autodetect JAVA_HOME if not defined
if [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

# Absolute path the the unzipped directory of the HBase installation
# This is required if you build using the default or cluster profile
# Cluster profile assumes you want to pick up dependencies from HBase classpath
# Not required in standalone.
HBASE_PATH=/usr/lib/hbase

# Add a space seperated list of -D environment args. "-Dkey1-val1 -Dkey2=val2"
ENV_PROPS=""

# Uncomment if you would like to remotely debug
#REMOTE_DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6666"
EOF

