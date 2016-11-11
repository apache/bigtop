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
     --pyspark-python            executable to use for Python interpreter [python]
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
  -l 'pyspark-python:' \
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
        --pyspark-python)
        PYSPARK_PYTHON=$2 ; shift 2
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/spark}
LIB_DIR=${LIB_DIR:-/usr/lib/spark}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/spark}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}
CONF_DIR=${CONF_DIR:-/etc/spark/conf.dist}
PYSPARK_PYTHON=${PYSPARK_PYTHON:-python}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/sbin
install -d -m 0755 $PREFIX/$LIB_DIR/extras
install -d -m 0755 $PREFIX/$LIB_DIR/extras/lib
install -d -m 0755 $PREFIX/$LIB_DIR/yarn
install -d -m 0755 $PREFIX/$LIB_DIR/yarn/lib
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$EXAMPLES_DIR

install -d -m 0755 $PREFIX/var/lib/spark/
install -d -m 0755 $PREFIX/var/log/spark/
install -d -m 0755 $PREFIX/var/run/spark/
install -d -m 0755 $PREFIX/var/run/spark/work/

tar --wildcards -C $PREFIX/$LIB_DIR/lib -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz spark-assembly\*
tar --wildcards --strip-components=1 -C $PREFIX/$LIB_DIR/lib -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz \*datanucleus\*
tar --wildcards -C $PREFIX/$LIB_DIR/ -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz bin\*
tar --wildcards -C $PREFIX/$LIB_DIR/ -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz sbin\*

rm -rf $PREFIX/$LIB_DIR/bin/*.cmd

# External/extra jars
ls ${BUILD_DIR}/{external,extras}/*/target/*${SPARK_VERSION}.jar | grep -v 'original-\|assembly' | xargs -IJARS cp JARS $PREFIX/$LIB_DIR/extras/lib

# Examples jar
cp ${BUILD_DIR}/examples/target/spark-examples*${SPARK_VERSION}.jar $PREFIX/$LIB_DIR/lib/spark-examples-${SPARK_VERSION}-hadoop${HADOOP_VERSION}.jar

# Spark YARN Shuffle jar
cp ${BUILD_DIR}/network/yarn/target/*/spark-${SPARK_VERSION}-yarn-shuffle.jar $PREFIX/$LIB_DIR/lib/

# Examples src
cp -ra ${BUILD_DIR}/examples/src $PREFIX/$EXAMPLES_DIR/
ln -s $EXAMPLES_DIR $PREFIX/$LIB_DIR/examples

# Data
cp -ra ${BUILD_DIR}/data $PREFIX/$LIB_DIR/

chmod 755 $PREFIX/$LIB_DIR/bin/*
chmod 755 $PREFIX/$LIB_DIR/sbin/*

# Copy in the configuration files
install -d -m 0755 $PREFIX/$CONF_DIR
cp -a ${BUILD_DIR}/conf/* $PREFIX/$CONF_DIR
cp $SOURCE_DIR/spark-env.sh $PREFIX/$CONF_DIR
ln -s /etc/spark/conf $PREFIX/$LIB_DIR/conf

# Copy in the wrappers
install -d -m 0755 $PREFIX/$BIN_DIR
for wrap in bin/spark-class bin/spark-shell bin/spark-sql bin/spark-submit; do
  cat > $PREFIX/$BIN_DIR/`basename $wrap` <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $INSTALLED_LIB_DIR/$wrap "\$@"
EOF
  chmod 755 $PREFIX/$BIN_DIR/`basename $wrap`
done

ln -s /var/run/spark/work $PREFIX/$LIB_DIR/work

cp -r ${BUILD_DIR}/python ${PREFIX}/${INSTALLED_LIB_DIR}/
rm -f ${PREFIX}/${INSTALLED_LIB_DIR}/python/.gitignore
cat > $PREFIX/$BIN_DIR/pyspark <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export PYSPARK_PYTHON=${PYSPARK_PYTHON}

exec $INSTALLED_LIB_DIR/bin/pyspark "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/pyspark

touch $PREFIX/$LIB_DIR/RELEASE
cp ${BUILD_DIR}/{LICENSE,NOTICE} ${PREFIX}/${LIB_DIR}/

# Version-less symlinks
(cd $PREFIX/$LIB_DIR/lib; ln -s spark-assembly*.jar spark-assembly.jar; ln -s spark-examples*.jar spark-examples.jar)
pushd $PREFIX/$LIB_DIR/yarn/lib
ln -s ../../lib/spark-*-yarn-shuffle.jar spark-yarn-shuffle.jar
ln -s ../../lib/datanucleus-api-jdo*.jar datanucleus-api-jdo.jar
ln -s ../../lib/datanucleus-core*.jar datanucleus-core.jar
ln -s ../../lib/datanucleus-rdbms*.jar datanucleus-rdbms.jar
popd
pushd $PREFIX/$LIB_DIR/extras/lib
for j in $(ls *.jar); do
  ln -s $j $(echo $j | sed -n 's/\(.*\)\(_[0-9.]\+-[0-9.]\+\)\(.jar\)/\1\3/p')
done
popd
