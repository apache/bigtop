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
     --var-dir=DIR               path to install Spark contents [/var/lib/spark]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --man-dir=DIR               path to install mans [/usr/share/man]
     --etc-default=DIR           path to bigtop default dir [/etc/default]
     --etc-spark=DIR             path to install hive conf [/etc/spark]
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
  -l 'var-dir:' \
  -l 'bin-dir:' \
  -l 'man-dir:' \
  -l 'etc-default:' \
  -l 'etc-spark:' \
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
        --var-dir)
        VAR_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --etc-default)
        ETC_DEFAULT=$2 ; shift 2
        ;;
        --etc-spark)
        ETC_SPARK=$2 ; shift 2
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

DIST_DIR=${BUILD_DIR}/dist

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/spark}
LIB_DIR=${LIB_DIR:-/usr/lib/spark}
VAR_DIR=${VAR_DIR:-/var/lib/spark}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DEFAULT=${ETC_DEFAULT:-/etc/default}

ETC_SPARK=${ETC_SPARK:-/etc/spark}
# No prefix
NP_ETC_SPARK=/etc/spark

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/external/lib
install -d -m 0755 $PREFIX/$LIB_DIR/yarn/lib
install -d -m 0755 $PREFIX/$NP_ETC_SPARK
install -d -m 0755 $PREFIX/$ETC_SPARK/conf.dist
install -d -m 0755 $PREFIX/$DOC_DIR

install -d -m 0755 $PREFIX/$VAR_DIR/
install -d -m 0755 $PREFIX/var/log/spark/
install -d -m 0755 $PREFIX/var/run/spark/
install -d -m 0755 $PREFIX/var/run/spark/work/

rm $DIST_DIR/bin/*.cmd
cp -r $DIST_DIR/* $PREFIX/$LIB_DIR

ln -s $LIB_DIR/examples $PREFIX/$DOC_DIR/

# External libraries not included in dist
find_external_modules() {
  # If corresponding assembly sub-module exists, copy the assembly jar instead of the non-assembly jar
  find ${BUILD_DIR}/external -mindepth 1 -maxdepth 1 -not -name '*-assembly' \
      -exec sh -c '([ -d "$1/target" ] && echo "$1") || ([ -d "$0/target" ] && echo "$0")' {} {}-assembly \;
}
copy_external_jars() {
  xargs -L1 -r -IMODULE find MODULE/target -name "*${SPARK_VERSION}.jar" -and -not -name "original-*" \
      -exec cp {} $PREFIX/$LIB_DIR/external/lib \;
}
find_external_modules | copy_external_jars

# Move the configuration files to the correct location
mv $PREFIX/$LIB_DIR/conf/* $PREFIX/$ETC_SPARK/conf.dist
cp $SOURCE_DIR/spark-env.sh $PREFIX/$ETC_SPARK/conf.dist
rmdir $PREFIX/$LIB_DIR/conf
ln -s $NP_ETC_SPARK/conf $PREFIX/$LIB_DIR/conf

# Copy in the wrappers
install -d -m 0755 $PREFIX/$BIN_DIR
for wrap in bin/spark-class bin/spark-shell bin/spark-sql bin/spark-submit bin/find-spark-home bin/sparkR; do
  cat > $PREFIX/$BIN_DIR/$(basename $wrap) <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $LIB_DIR/$wrap "\$@"
EOF
  chmod 755 $PREFIX/$BIN_DIR/$(basename $wrap)
done

ln -s /var/run/spark/work $PREFIX/$LIB_DIR/work

rm -f $PREFIX/$LIB_DIR/python/.gitignore
cat > $PREFIX/$BIN_DIR/pyspark <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export PYSPARK_PYTHON=python

exec $LIB_DIR/bin/pyspark "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/pyspark

cat > $PREFIX/$BIN_DIR/spark-example <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $LIB_DIR/bin/run-example "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/spark-example

touch $PREFIX/$LIB_DIR/RELEASE
cp ${BUILD_DIR}/NOTICE ${PREFIX}/${LIB_DIR}/

# For Spark 2.4, we need to add the license files here
# since the Spark community doesn't include licenses for binary in the source tar ball
# after they splitted license files between source and binary licenses.
# See the discussion on the followings:
#   - https://issues.apache.org/jira/browse/SPARK-25840
#   - https://github.com/apache/spark/pull/22840
#   - https://issues.apache.org/jira/browse/SPARK-24654
# Remenber to fetch the new LICENSE-binary and licenses-binary files when upgrading Spark version.
cp ${SOURCE_DIR}/LICENSE-binary ${PREFIX}/${LIB_DIR}/LICENSE
cp -r ${SOURCE_DIR}/licenses-binary ${PREFIX}/${LIB_DIR}/licenses

# Version-less symlinks
(cd $PREFIX/$LIB_DIR/examples/jars; ln -s spark-examples*.jar spark-examples.jar)
pushd $PREFIX/$LIB_DIR/yarn/lib
ln -s ../spark-*-yarn-shuffle.jar spark-yarn-shuffle.jar
ln -s ../../jars/datanucleus-api-jdo*.jar datanucleus-api-jdo.jar
ln -s ../../jars/datanucleus-core*.jar datanucleus-core.jar
ln -s ../../jars/datanucleus-rdbms*.jar datanucleus-rdbms.jar
popd
pushd $PREFIX/$LIB_DIR/external/lib
for j in $(ls *.jar); do
  ln -s $j $(echo $j | sed -n 's/\(.*\)\(_.\+\)\(.jar\)/\1\3/p')
done
popd