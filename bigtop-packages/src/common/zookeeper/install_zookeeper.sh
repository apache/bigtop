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
     --build-dir=DIR             path to zookeeper dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/zookeeper]
     --lib-dir=DIR               path to install zookeeper home [/usr/lib/zookeeper]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --examples-dir=DIR          path to install examples [doc-dir/examples]
     --system-include-dir=DIR    path to install development headers [/usr/include]
     --system-lib-dir=DIR        path to install native libraries [/usr/lib]
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
  -l 'examples-dir:' \
  -l 'system-include-dir:' \
  -l 'system-lib-dir:' \
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
        --examples-dir)
        EXAMPLES_DIR=$2 ; shift 2
        ;;
        --system-include-dir)
        SYSTEM_INCLUDE_DIR=$2 ; shift 2
        ;;
        --system-lib-dir)
        SYSTEM_LIB_DIR=$2 ; shift 2
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/zookeeper}
LIB_DIR=${LIB_DIR:-/usr/lib/zookeeper}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=/etc/zookeeper/conf
CONF_DIST_DIR=/etc/zookeeper/conf.dist/
SYSTEM_INCLUDE_DIR=${SYSTEM_INCLUDE_DIR:-/usr/include}
SYSTEM_LIB_DIR=${SYSTEM_LIB_DIR:-/usr/lib}

tar -z -x -f zookeeper-assembly/target/apache-zookeeper-*-bin.tar.gz
install -d -m 0755 $PREFIX/$LIB_DIR/
rm -f $BUILD_DIR/zookeeper-*-javadoc.jar $BUILD_DIR/zookeeper-*-bin.jar $BUILD_DIR/zookeeper-*-sources.jar $BUILD_DIR/zookeeper-*-test.jar
cp $BUILD_DIR/lib/zookeeper*.jar $PREFIX/$LIB_DIR/
install -d -m 0755 ${PREFIX}/${LIB_DIR}/contrib/rest
install -d -m 0755 ${PREFIX}/${CONF_DIST_DIR}/rest
cp ${BUILD_DIR}/contrib/rest/zookeeper-contrib-rest-*.jar ${PREFIX}/${LIB_DIR}/contrib/rest/
cp -r ${BUILD_DIR}/contrib/rest/lib ${PREFIX}/${LIB_DIR}/contrib/rest/
cp -r ${BUILD_DIR}/contrib/rest/conf/* ${PREFIX}/${CONF_DIST_DIR}/rest/

# Make a symlink of zookeeper.jar to zookeeper-version.jar
for x in $PREFIX/$LIB_DIR/zookeeper-[:digit:]*.jar ; do
  x=$(basename $x)
  ln -s $x $PREFIX/$LIB_DIR/zookeeper.jar
done

install -d -m 0755 $PREFIX/$LIB_DIR/lib
cp $BUILD_DIR/lib/*.jar $PREFIX/$LIB_DIR/lib

# Copy in the configuration files
install -d -m 0755 $PREFIX/$CONF_DIST_DIR
cp zoo.cfg $BUILD_DIR/conf/* $PREFIX/$CONF_DIST_DIR/
ln -s $CONF_DIR $PREFIX/$LIB_DIR/conf

# Copy in the /usr/bin/zookeeper-server wrapper
install -d -m 0755 $PREFIX/$LIB_DIR/bin

for i in zkServer.sh zkEnv.sh zkCli.sh zkCleanup.sh zkServer-initialize.sh
	do cp $BUILD_DIR/bin/$i $PREFIX/$LIB_DIR/bin
	chmod 755 $PREFIX/$LIB_DIR/bin/$i
done

wrapper=$PREFIX/usr/bin/zookeeper-client
install -d -m 0755 `dirname $wrapper`
cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export ZOOKEEPER_HOME=\${ZOOKEEPER_CONF:-/usr/lib/zookeeper}
export ZOOKEEPER_CONF=\${ZOOKEEPER_CONF:-/etc/zookeeper/conf}
export CLASSPATH=\$CLASSPATH:\$ZOOKEEPER_CONF:\$ZOOKEEPER_HOME/*:\$ZOOKEEPER_HOME/lib/*
export ZOOCFGDIR=\${ZOOCFGDIR:-\$ZOOKEEPER_CONF}
env CLASSPATH=\$CLASSPATH /usr/lib/zookeeper/bin/zkCli.sh "\$@"
EOF
chmod 755 $wrapper

for pairs in zkServer.sh/zookeeper-server zkServer-initialize.sh/zookeeper-server-initialize zkCleanup.sh/zookeeper-server-cleanup ; do
  wrapper=$PREFIX/usr/bin/`basename $pairs`
  upstream_script=`dirname $pairs`
  cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export ZOOPIDFILE=\${ZOOPIDFILE:-/var/run/zookeeper/zookeeper_server.pid}
export ZOOKEEPER_HOME=\${ZOOKEEPER_CONF:-/usr/lib/zookeeper}
export ZOOKEEPER_CONF=\${ZOOKEEPER_CONF:-/etc/zookeeper/conf}
export ZOOCFGDIR=\${ZOOCFGDIR:-\$ZOOKEEPER_CONF}
export CLASSPATH=\$CLASSPATH:\$ZOOKEEPER_CONF:\$ZOOKEEPER_HOME/*:\$ZOOKEEPER_HOME/lib/*
export ZOO_LOG_DIR=\${ZOO_LOG_DIR:-/var/log/zookeeper}
export ZOO_LOG4J_PROP=\${ZOO_LOG4J_PROP:-INFO,ROLLINGFILE}
export JVMFLAGS=\${JVMFLAGS:--Dzookeeper.log.threshold=INFO}
export ZOO_DATADIR_AUTOCREATE_DISABLE=\${ZOO_DATADIR_AUTOCREATE_DISABLE:-true}
env CLASSPATH=\$CLASSPATH /usr/lib/zookeeper/bin/${upstream_script} "\$@"
EOF
  chmod 755 $wrapper
done

# Copy in the docs
install -d -m 0755 $PREFIX/$DOC_DIR
cp -a $BUILD_DIR/docs/* $PREFIX/$DOC_DIR
cp $BUILD_DIR/*.txt $PREFIX/$DOC_DIR/

install -d -m 0755 ${PREFIX}/etc/default
cp zookeeper.default ${PREFIX}/etc/default/zookeeper

install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c zookeeper.1 > $PREFIX/$MAN_DIR/zookeeper.1.gz

# Zookeeper log and tx log directory
install -d -m 1766 $PREFIX/var/log/zookeeper
install -d -m 1766 $PREFIX/var/log/zookeeper/txlog

# ZooKeeper native libraries
install -d ${PREFIX}/$SYSTEM_INCLUDE_DIR
install -d ${PREFIX}/$SYSTEM_LIB_DIR
install -d ${PREFIX}/${LIB_DIR}-native

cp -R ${BUILD_DIR}/native/include/* ${PREFIX}/${SYSTEM_INCLUDE_DIR}/
cp -R ${BUILD_DIR}/native/lib*/* ${PREFIX}/${SYSTEM_LIB_DIR}/
cp -R ${BUILD_DIR}/native/bin/* ${PREFIX}/${LIB_DIR}-native/
for binary in ${PREFIX}/${LIB_DIR}-native/*; do
  cat > ${PREFIX}/${BIN_DIR}/`basename ${binary}` <<EOF
#!/bin/bash

PREFIX=\$(dirname \$(readlink -f \$0))
export LD_LIBRARY_PATH=\${LD_LIBRARY_PATH}:\${PREFIX}/../lib:\${PREFIX}/../lib64
/usr/lib/zookeeper-native/`basename ${binary}` \$@

EOF
done
chmod 755 ${PREFIX}/${BIN_DIR}/* ${PREFIX}/${LIB_DIR}-native/*
