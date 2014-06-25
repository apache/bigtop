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
     --build-dir=DIR             path to accumulo dist.dir
     --prefix=PREFIX             path to install into
     --extra-dir=DIR             path to additional files

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/accumulo]
     --lib-dir=DIR               path to install accumulo home [/usr/lib/accumulo]
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
  -l 'bin-dir:' \
  -l 'conf-dir:' \
  -l 'extra-dir:' \
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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --conf-dir)
        CONF_DIR=$2 ; shift 2
        ;;
        --extra-dir)
        EXTRA_DIR=$2 ; shift 2
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

DOC_DIR=${DOC_DIR:-/usr/share/doc/accumulo}
LIB_DIR=${LIB_DIR:-/usr/lib/accumulo}
BIN_DIR=${BIN_DIR:-/usr/lib/accumulo/bin}
CONF_DIR=${CONF_DIR:-/etc/accumulo/conf}

install -d -m 0755 ${PREFIX}/${LIB_DIR}
install -d -m 0755 ${PREFIX}/${DOC_DIR}
install -d -m 0755 ${PREFIX}/${BIN_DIR}
install -d -m 0755 ${PREFIX}/${CONF_DIR}.dist
install -d -m 0755 ${PREFIX}/var/log/accumulo
install -d -m 0755 ${PREFIX}/var/lib/accumulo

ASSEMBLY_DIR=assemble/target/accumulo*/accumulo*
cp -ra ${BUILD_DIR}/${ASSEMBLY_DIR}/lib ${PREFIX}/${LIB_DIR}/
cp -ra ${BUILD_DIR}/docs/* ${PREFIX}/${DOC_DIR}
cp -ra ${BUILD_DIR}/bin/* ${PREFIX}/${BIN_DIR}

# System burn-in tests expect to read the examples here
install -d -m 0755 ${PREFIX}/${LIB_DIR}/conf
cp -ra ${BUILD_DIR}/conf/examples ${PREFIX}/${LIB_DIR}/conf/

cp -ra ${EXTRA_DIR}/conf/* ${PREFIX}/${CONF_DIR}.dist/

install -d -m 0755 $PREFIX/etc/default
defaults=$PREFIX/etc/default/accumulo
mkdir -p `dirname $defaults`
cat > $defaults <<'EOF'
ACCUMULO_TSERVER_OPTS="-Xmx1g -Xms1g -XX:NewSize=500m -XX:MaxNewSize=500m"
ACCUMULO_MASTER_OPTS="-Xmx2g -Xms1g"
ACCUMULO_MONITOR_OPTS="-Xmx2g -Xms256m"
ACCUMULO_GC_OPTS="-Xmx256m -Xms256m"
ACCUMULO_GENERAL_OPTS="-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75"
ACCUMULO_OTHER_OPTS="-Xmx1g -Xms256m"
EOF

for role in masters slaves tracers monitor gc; do
    echo "localhost" > ${PREFIX}/${CONF_DIR}.dist/${role}
done

install -d -m 0755 $PREFIX/usr/bin
wrapper=$PREFIX/usr/bin/accumulo
mkdir -p `dirname $wrapper`
cat > $wrapper <<'EOF'
#!/bin/bash

. /etc/default/accumulo

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_HOME=${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CLIENT_HOME=${HADOOP_CLIENT_HOME:-${HADOOP_HOME}/client-0.20}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export ZOOKEEPER_HOME=${ZOOKEEPER_HOME:-/usr/lib/zookeeper}
export ACCUMULO_LOG_DIR=${ACCUMULO_LOG_DIR:-/var/log/accumulo}
export ACCUMULO_CONF_DIR=${ACCUMULO_CONF_DIR:-/etc/accumulo/conf}

export HADOOP_PREFIX=${HADOOP_HOME}
if [ -f ${ACCUMULO_CONF_DIR}/accumulo.policy ]
then
   POLICY="-Djava.security.manager -Djava.security.policy=${ACCUMULO_CONF_DIR}/accumulo.policy"
fi
export ACCUMULO_TEST="_"
export ACCUMULO_TSERVER_OPTS="${POLICY} ${ACCUMULO_TSERVER_OPTS}"
export ACCUMULO_MASTER_OPTS="${POLICY} ${ACCUMULO_MASTER_OPTS}"
export ACCUMULO_MONITOR_OPTS="${POLICY} ${ACCUMULO_MONITOR_OPTS}"
export ACCUMULO_GC_OPTS
export ACCUMULO_GENERAL_OPTS
export ACCUMULO_OTHER_OPTS

exec /usr/lib/accumulo/bin/accumulo "$@"
EOF
chmod 755 $wrapper

