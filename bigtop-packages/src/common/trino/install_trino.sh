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
  -l 'var-dir:' \
  -l 'lib-dir:' \
  -l 'cli-dir:' \
  -l 'cli-build-dir:' \
  -l 'conf-dist-dir:' \
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
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --cli-build-dir)
        CLI_BUILD_DIR=$2 ; shift 2
        ;;
        --cli-dir)
        CLI_DIR=$2 ; shift 2
        ;;
        --var-dir)
        VAR_DIR=$2 ; shift 2
        ;;
		--conf-dist-dir)
		CONF_DIST_DIR=$2 ; shift 2
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

. /etc/os-release
OS="$ID"


CLI_DIR=${CLI_DIR:-/usr/lib/trino-cli}
LIB_DIR=${LIB_DIR:-/usr/lib/trino}
VAR_DIR=${VAR_DIR:-/var/lib/trino}
LOG_DIR=${LOG_DIR:-/var/log/trino}
RUN_DIR=${RUN_DIR:-/var/run/trino}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIST_DIR=${CONF_DIST_DIR:-/etc/trino.dist}

NP_ETC_TRINO=/etc/trino

install -d -m 0755 $PREFIX/$CONF_DIST_DIR
install -d -m 0755 $PREFIX/$CONF_DIST_DIR/catalog
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/var
install -d -m 0755 $PREFIX/$CLI_DIR
install -d -m 0755 $PREFIX/$VAR_DIR
install -d -m 0755 $PREFIX/$LOG_DIR
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$NP_ETC_TRINO

cp -ra ${BUILD_DIR}/* $PREFIX/$LIB_DIR/
cp -ra ${CLI_BUILD_DIR}/* $PREFIX/$CLI_DIR/

ln -s $NP_ETC_TRINO/conf $PREFIX/$LIB_DIR/etc
ln -s $RUN_DIR $PREFIX/$LIB_DIR/var/run
ln -s $LOG_DIR $PREFIX/$LIB_DIR/var/log

chmod +x $PREFIX/$LIB_DIR/bin/launcher



cat > $PREFIX/$CONF_DIST_DIR/node.properties <<EOF
node.environment=production
node.id=ffffffff-ffff-ffff-ffff-ffffffffffff
node.data-dir=/var/trino/data
EOF

cat > $PREFIX/$CONF_DIST_DIR/jvm.config <<EOF
-server
-Xmx16G
-XX:G1HeapRegionSize=32M
-XX:+ExplicitGCInvokesConcurrent
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
-XX:-OmitStackTraceInFastThrow
-XX:ReservedCodeCacheSize=512M
-XX:PerMethodRecompilationCutoff=10000
-XX:PerBytecodeRecompilationCutoff=10000
-Djdk.attach.allowAttachSelf=true
-Djdk.nio.maxCachedBufferSize=2000000
-XX:+UnlockDiagnosticVMOptions
EOF

cat > $PREFIX/$CONF_DIST_DIR/config.properties <<EOF
# A single machine for testing that will function as both a coordinator and worker
coordinator=true
node-scheduler.include-coordinator=true
http-server.http.port=8080
query.max-memory=4GB
query.max-memory-per-node=1GB
discovery-server.enabled=true
discovery.uri=http://localhost:8080
# Minimal configuration for the coordinator:
#coordinator=true
#node-scheduler.include-coordinator=false
#http-server.http.port=8080
#query.max-memory=4GB
#query.max-memory-per-node=1GB
#discovery-server.enabled=true
#discovery.uri=http://example.net:8080
# Minimal configuration for the workers:
#coordinator=false
#http-server.http.port=8080
#query.max-memory=4GB
#query.max-memory-per-node=1GB
#discovery.uri=http://example.net:8080
http-server.log.path=/var/log/presto/http-request.log
EOF

cat > $PREFIX/$CONF_DIST_DIR/log.properties <<EOF
io.trino=INFO
EOF

cat > $PREFIX/$CONF_DIST_DIR/catalog/tpch.properties <<EOF
connector.name=tpch
EOF

cat > $PREFIX/$CONF_DIST_DIR/catalog/tpcds.properties <<EOF
connector.name=tpcds
EOF

cat > $PREFIX/$CONF_DIST_DIR/catalog/jmx.properties <<EOF
connector.name=jmx
EOF

