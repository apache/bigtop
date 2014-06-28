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
     --build-dir=DIR             path to dist dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/giraph]
     --lib-dir=DIR               path to install giraph home [/usr/lib/giraph]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
     --conf-dir=DIR              path to configuration files provided by the package [/etc/giraph/conf.dist]
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
  -l 'conf-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
  -l 'examples-dir:' \
  -l 'build-dir:' -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
set -ex
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
        --conf-dir)
        CONF_DIR=$2 ; shift 2
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

DOC_DIR=${DOC_DIR:-/usr/share/doc/giraph}
LIB_DIR=${LIB_DIR:-/usr/lib/giraph}
BIN_DIR=${BIN_DIR:-/usr/lib/giraph/bin}
ETC_DIR=${ETC_DIR:-/etc/giraph}
MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf.dist}

install -d -m 0755 ${PREFIX}/${LIB_DIR}

# Installing Giraph core
install -d -m 0755 ${PREFIX}/${LIB_DIR}
cp -r $BUILD_DIR/giraph-dist/target/giraph*-bin/*/* ${PREFIX}/${LIB_DIR}

# Installing docs and examples
install -d -m 0755 $PREFIX/${DOC_DIR}
cp -r $BUILD_DIR/target/staging/*  $PREFIX/${DOC_DIR}
mv ${PREFIX}/${LIB_DIR}/giraph-examples*.jar $PREFIX/${DOC_DIR}

# Install executable wrappers
install -d -m 0755 $PREFIX/usr/bin

for i in giraph ; do
	#echo "Copying manpage $i"
	#cp ${BUILD_DIR}/docs/man/$i* $PREFIX/$MAN_DIR
	echo "Creating wrapper for $i"
	wrapper=$PREFIX/usr/bin/$i
	mkdir -p `dirname $wrapper`
	cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

# Workaround for GIRAPH-199
export HADOOP_HOME=\${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=\${HADOOP_CONF_DIR:-/etc/hadoop/conf}

export GIRAPH_HOME=$LIB_DIR
exec $BIN_DIR/$i "\$@"
EOF
   chmod 0755 $wrapper
done

install -d -m 0755 $PREFIX/$CONF_DIR
(cd ${BUILD_DIR}/conf && tar cf - .) | (cd $PREFIX/$CONF_DIR && tar xf -)

unlink $PREFIX/$LIB_DIR/conf || /bin/true
ln -s $ETC_DIR/conf $PREFIX/$LIB_DIR/conf

# Create version independent symlinks
for i in accumulo core gora hbase hcatalog hive kibble rexster ; do
  (cd $PREFIX/$LIB_DIR ; ln -s `ls giraph-$i-*jar` giraph-$i.jar)
done

# Enforcing dependency on the Bigtop's version of Zookeeper
ln -fs /usr/lib/zookeeper/zookeeper.jar $PREFIX/$LIB_DIR/lib/zookeeper*.jar
