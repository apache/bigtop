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
     --build-dir=DIR             path to tajo dist.dir
     --source-dir=DIR            path to package shared files dir
     --prefix=PREFIX             path to install into

  Optional options:
     --bin-dir=DIR               path to install docs into [/usr/bin]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'bin-dir:' \
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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
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

MAN_DIR=/usr/share/man/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/tajo}
LIB_DIR=${LIB_DIR:-/usr/lib/tajo}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/tajo}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/tajo/conf.dist}

WORK_DIR=${WORK_DIR:-/var/lib/tajo}
LOG_DIR=${LOG_DIR:-/var/log/tajo}
PID_DIR=${PID_DIR:-/var/run/tajo}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/extlib
install -d -m 0755 $PREFIX/$LIB_DIR/share
install -d -m 0755 $PREFIX/$LIB_DIR/share/jdbc-dist
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$WORK_DIR
install -d -m 0755 $PREFIX/$LOG_DIR
install -d -m 0755 $PREFIX/$PID_DIR

ln -s $LOG_DIR $PREFIX/$LIB_DIR/logs

# Copy in the binary files
cp -ra ${BUILD_DIR}/lib/* $PREFIX/${LIB_DIR}/lib/
cp ${BUILD_DIR}/tajo*.jar $PREFIX/$LIB_DIR
cp -a ${BUILD_DIR}/bin/* $PREFIX/${LIB_DIR}/bin
rm -rf $PREFIX/${LIB_DIR}/bin/*.cmd

# Copy in the library files
for module in ${BUILD_DIR}/lib/*; do
  if [ -f $module ]; then
    x=$(basename $module)
    cp -ra ${BUILD_DIR}/lib/$x $PREFIX/${LIB_DIR}/lib/
  fi
done
for module in ${BUILD_DIR}/extlib/*; do
  if [ -f $module ]; then
    x=$(basename $module)
    cp -ra ${BUILD_DIR}/extlib/$x $PREFIX/${LIB_DIR}/extlib/
  fi
done
for module in ${BUILD_DIR}/share/jdbc-dist/*; do
  if [ -f $module ]; then
    x=$(basename $module)
    cp -ra ${BUILD_DIR}/share/jdbc-dist/$x $PREFIX/${LIB_DIR}/share/jdbc-dist/
  fi
done

# Copy in the configuration files
cp -a ${BUILD_DIR}/conf/* $PREFIX/$CONF_DIR
cp -a $SOURCE_DIR/tajo-env.default $PREFIX/$CONF_DIR/tajo-env.sh
chmod +x $PREFIX/$CONF_DIR/tajo-env.sh
ln -s /etc/tajo/conf $PREFIX/$LIB_DIR/conf

# Copy in the wrappers
install -d -m 0755 $PREFIX/$BIN_DIR
for wrap in bin/tsql bin/tajo; do
  cat > $PREFIX/$BIN_DIR/`basename $wrap` <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $INSTALLED_LIB_DIR/$wrap "\$@"
EOF
  chmod 755 $PREFIX/$BIN_DIR/`basename $wrap`
done