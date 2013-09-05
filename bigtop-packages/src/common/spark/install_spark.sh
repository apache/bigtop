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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/spark]
     --lib-dir=DIR               path to install Spark home [/usr/lib/spark]
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
  -l 'source-dir:' \
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

for var in PREFIX BUILD_DIR SOURCE_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

if [ -z "${SCALA_HOME}" ]; then
    echo Missing env. var SCALA_HOME
    usage
fi
if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/spark}
LIB_DIR=${LIB_DIR:-/usr/lib/spark}
SPARK_BIN_DIR=${BIN_DIR:-/usr/lib/spark/bin}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/spark}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/spark/conf.dist}
SCALA_HOME=${SCALA_HOME:-/usr/share/scala}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$SPARK_BIN_DIR
install -d -m 0755 $PREFIX/$DOC_DIR

tar --wildcards -C $PREFIX/$LIB_DIR -zxf ${BUILD_DIR}/assembly/target/spark-assembly-*-dist.tar.gz 'lib/*'

for comp in core repl bagel mllib streaming; do
  install -d -m 0755 $PREFIX/$LIB_DIR/$comp/lib
  tar --wildcards -C $PREFIX/$LIB_DIR/$comp/lib -zxf ${BUILD_DIR}/assembly/target/spark-assembly-*-dist.tar.gz spark-$comp\*
done
## FIXME: Spark maven assembly needs to include examples into it.
install -d -m 0755 $PREFIX/$LIB_DIR/examples/lib
cp ${BUILD_DIR}/examples/target/spark-examples-${SPARK_VERSION}.jar $PREFIX/$LIB_DIR/examples/lib

# FIXME: executor scripts need to reside in bin
cp -a $BUILD_DIR/spark-class $PREFIX/$LIB_DIR
cp -a $BUILD_DIR/spark-executor $PREFIX/$LIB_DIR
cp -a ${SOURCE_DIR}/compute-classpath.sh $PREFIX/$SPARK_BIN_DIR
cp -a ${BUILD_DIR}/spark-shell $PREFIX/$LIB_DIR
touch $PREFIX/$LIB_DIR/RELEASE

# Copy in the configuration files
install -d -m 0755 $PREFIX/$CONF_DIR
cp -a ${BUILD_DIR}/conf/* $PREFIX/$CONF_DIR
cp  $PREFIX/$CONF_DIR/spark-env.sh.template $PREFIX/$CONF_DIR/spark-env.sh
ln -s /etc/spark/conf $PREFIX/$LIB_DIR/conf

# Unpack static UI resources into install_dir/spark where it is expected to be
tar --wildcards --transform 's,ui-resources/spark,spark,' -C $PREFIX/$LIB_DIR -zxf ${BUILD_DIR}/assembly/target/spark-assembly-*-dist.tar.gz ui-resources/\*

# set correct permissions for exec. files
for execfile in spark-class spark-shell spark-executor ; do
  chmod 755 $PREFIX/$LIB_DIR/$execfile
done
chmod 755 $PREFIX/$SPARK_BIN_DIR/compute-classpath.sh

# Copy in the wrappers
install -d -m 0755 $PREFIX/$BIN_DIR
for wrap in spark-executor spark-shell ; do
  cat > $PREFIX/$BIN_DIR/$wrap <<EOF
#!/bin/bash 

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

exec $INSTALLED_LIB_DIR/$wrap "\$@"
EOF
  chmod 755 $PREFIX/$BIN_DIR/$wrap
done

cat >> $PREFIX/$CONF_DIR/spark-env.sh <<EOF

### Let's run everything with JVM runtime, instead of Scala
export SPARK_LAUNCH_WITH_SCALA=0
export SPARK_LIBRARY_PATH=\${SPARK_HOME}/lib
export SCALA_LIBRARY_PATH=\${SPARK_HOME}/lib
export SPARK_MASTER_WEBUI_PORT=18080
export SPARK_MASTER_PORT=7077

### Comment above 2 lines and uncomment the following if
### you want to run with scala version, that is included with the package
#export SCALA_HOME=\${SCALA_HOME:-$LIB_DIR/scala}
#export PATH=\$PATH:\$SCALA_HOME/bin

### change the following to specify a real cluster's Master host
export STANDALONE_SPARK_MASTER_HOST=\`hostname\`

EOF

ln -s /var/run/spark/work $PREFIX/$LIB_DIR/work
