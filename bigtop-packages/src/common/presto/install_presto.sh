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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/presto]
     --lib-dir=DIR               path to install Presto home [/usr/lib/presto]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
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

if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/presto}
LIB_DIR=${LIB_DIR:-/usr/lib/presto}
VAR_DIR=${VAR_DIR:-/var/lib/presto}
LOG_DIR=${LOG_DIR:-/var/log/presto}
RUN_DIR=${RUN_DIR:-/var/run/presto}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/presto}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/presto}
CONF_DIST_DIR=${CONF_DIST_DIR:-/etc/presto.dist}
DEFAULT_DIR=${DEFAULT_DIR:-/etc/default}

install -d -m 0755 $PREFIX/$CONF_DIST_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/var
install -d -m 0755 $PREFIX/$LIB_DIR/plugin
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$VAR_DIR
install -d -m 0755 $PREFIX/$LOG_DIR
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$DEFAULT_DIR

cp -ra ${BUILD_DIR}/* $PREFIX/$LIB_DIR/
mv $PREFIX/$LIB_DIR/docs/* $PREFIX/$DOC_DIR/

chmod +x $PREFIX/$LIB_DIR/bin/{launcher,presto}

install -d -m 0755 $PREFIX/$CONF_DIST_DIR
install -d -m 0755 $PREFIX/$CONF_DIST_DIR/catalog

cat > $PREFIX/$CONF_DIST_DIR/node.properties <<EOF
node.environment=production
node.id=ffffffff-ffff-ffff-ffff-ffffffffffff
node.data-dir=/var/lib/presto/
EOF

cat > $PREFIX/$CONF_DIST_DIR/jvm.config <<EOF
-server
-Xmx1G
-XX:+UseG1GC
-XX:G1HeapRegionSize=32M
-XX:+UseGCOverheadLimit
-XX:+ExplicitGCInvokesConcurrent
-XX:+HeapDumpOnOutOfMemoryError
-XX:+ExitOnOutOfMemoryError
-DHADOOP_USER_NAME=hive
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
com.facebook.presto=INFO
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

# Wrappers
install -d -m 0755 $PREFIX/$BIN_DIR
for wrap in bin/presto ; do
  cat > $PREFIX/$BIN_DIR/`basename $wrap` <<EOF
#!/bin/bash 

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $INSTALLED_LIB_DIR/$wrap "\$@"
EOF
  chmod 755 $PREFIX/$BIN_DIR/`basename $wrap`
done

ln -s ${LOG_DIR} $PREFIX/$LIB_DIR/log
ln -s ${CONF_DIR} $PREFIX/$LIB_DIR/etc
