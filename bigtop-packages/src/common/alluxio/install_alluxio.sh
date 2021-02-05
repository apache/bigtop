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
     --build-dir=DIR             path to Alluxio dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --bin-dir=DIR               path to install bin
     --data-dir=DIR              path to install local Alluxio data
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'bin-dir:' \
  -l 'libexec-dir:' \
  -l 'var-dir:' \
  -l 'lib-dir:' \
  -l 'data-dir:' \
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
        --libexec-dir)
        LIBEXEC_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --var-dir)
        VAR_DIR=$2 ; shift 2
        ;;
        --data-dir)
        DATA_DIR=$2 ; shift 2
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

LIB_DIR=${LIB_DIR:-/usr/lib/alluxio}
LIBEXEC_DIR=${INSTALLED_LIB_DIR:-/usr/libexec}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIST_DIR=/etc/alluxio/conf.dist/

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/assembly/client/target
install -d -m 0755 $PREFIX/$LIB_DIR/assembly/server/target
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/libexec
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/client
install -d -m 0755 $PREFIX/$LIB_DIR/integration
install -d -m 0755 $PREFIX/$LIB_DIR/share
install -d -m 0755 $PREFIX/$LIB_DIR/core/server/common/src/main/webapp
install -d -m 0755 $PREFIX/$DATA_DIR
install -d -m 0755 $PREFIX/$DATA_DIR/alluxio
install -d -m 0755 $PREFIX/etc
install -d -m 0755 $PREFIX/etc/alluxio
install -d -m 0755 $PREFIX/etc/alluxio/conf
install -d -m 0755 $PREFIX/$VAR_DIR/log/alluxio
install -d -m 0755 $PREFIX/$VAR_DIR/lib/alluxio/journal
install -d -m 0755 $PREFIX/$VAR_DIR/run/alluxio
install -d -m 0755 $PREFIX/$CONF_DIST_DIR

ln -s $CONF_DIST_DIR $PREFIX/$LIB_DIR/conf
ln -s $VAR_DIR/log/alluxio $PREFIX/$LIB_DIR/logs

cp assembly/server/target/alluxio*dependencies.jar $PREFIX/$LIB_DIR/assembly/server/target
cp assembly/client/target/alluxio*dependencies.jar $PREFIX/$LIB_DIR/assembly/client/target
cp -a bin/* $PREFIX/$LIB_DIR/bin
cp -a lib/* $PREFIX/$LIB_DIR/lib
cp -a libexec/* $PREFIX/$LIB_DIR/libexec
cp -a client/* $PREFIX/$LIB_DIR/client
cp -a integration/* $PREFIX/$LIB_DIR/integration
cp integration/fuse/target/alluxio-integration-fuse-*-jar-with-dependencies.jar $PREFIX/$LIB_DIR/integration/fuse
cp integration/yarn/target/alluxio-integration-yarn-*-jar-with-dependencies.jar $PREFIX/$LIB_DIR/integration/yarn
rm -rf $PREFIX/$LIB_DIR/integration/pom.xml $PREFIX/$LIB_DIR/integration/**/pom.xml
rm -rf $PREFIX/$LIB_DIR/integration/target $PREFIX/$LIB_DIR/integration/**/target
rm -rf $PREFIX/$LIB_DIR/integration/**/src
rm -rf $PREFIX/$LIB_DIR/integration/**/README.md

# Copy in the configuration files
install -m 0644 conf/log4j.properties $PREFIX/$CONF_DIST_DIR
cp conf/alluxio-env.sh.template $PREFIX/$CONF_DIST_DIR/alluxio-env.sh
cp conf/alluxio-site.properties.template $PREFIX/$CONF_DIST_DIR/alluxio-site.properties
cp conf/core-site.xml.template $PREFIX/$CONF_DIST_DIR/core-site.xml
cp conf/metrics.properties.template $PREFIX/$CONF_DIST_DIR/metrics.properties
cp conf/masters $PREFIX/$CONF_DIST_DIR/masters
cp conf/workers $PREFIX/$CONF_DIST_DIR/workers

# Copy in the /usr/bin/alluxio wrapper
install -d -m 0755 $PREFIX/$BIN_DIR

# Prefix is correct at time of install,
# but we dont want to escape it before that point.
cat > $PREFIX/$BIN_DIR/alluxio <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome
# Lib dir => ${LIB_DIR}
#!/usr/bin/env bash
exec ${LIB_DIR}/bin/alluxio "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/alluxio

