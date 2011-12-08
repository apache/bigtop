#!/bin/bash -x
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
     --distro-dir=DIR            path to distro specific files (debian/RPM)
     --build-dir=DIR             path to hive/build/dist
     --prefix=PREFIX             path to install into

  Optional options:
     --native-build-string       eg Linux-amd-64 (optional - no native installed if not set)
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'distro-dir:' \
  -l 'build-dir:' \
  -l 'native-build-string:' \
  -l 'installed-lib-dir:' \
  -l 'hadoop-dir:' \
  -l 'system-include-dir:' \
  -l 'system-lib-dir:' \
  -l 'system-libexec-dir:' \
  -l 'hadoop-etc-dir:' \
  -l 'doc-dir:' \
  -l 'man-dir:' \
  -l 'example-dir:' \
  -l 'apache-branch:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --hadoop-dir)
        HADOOP_DIR=$2 ; shift 2
        ;;
        --system-include-dir)
        SYSTEM_INCLUDE_DIR=$2 ; shift 2
        ;;
        --system-lib-dir)
        SYSTEM_LIB_DIR=$2 ; shift 2
        ;;
        --system-libexec-dir)
        SYSTEM_LIBEXEC_DIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --native-build-string)
        NATIVE_BUILD_STRING=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --hadoop-etc-dir)
        HADOOP_ETC_DIR=$2 ; shift 2
        ;;
        --installed-lib-dir)
        INSTALLED_LIB_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --example-dir)
        EXAMPLE_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

HADOOP_DIR=${HADOOP_DIR:-$PREFIX/usr/lib/hadoop}
SYSTEM_LIB_DIR=${SYSTEM_LIB_DIR:-/usr/lib}
BIN_DIR=${BIN_DIR:-$PREFIX/usr/bin}
DOC_DIR=${DOC_DIR:-$PREFIX/usr/share/doc/hadoop}
MAN_DIR=${MAN_DIR:-$PREFIX/usr/man}
SYSTEM_INCLUDE_DIR=${SYSTEM_INCLUDE_DIR:-$PREFIX/usr/include}
SYSTEM_LIBEXEC_DIR=${SYSTEM_LIBEXEC_DIR:-$PREFIX/usr/libexec}
EXAMPLE_DIR=${EXAMPLE_DIR:-$DOC_DIR/examples}
HADOOP_ETC_DIR=${HADOOP_ETC_DIR:-$PREFIX/etc/hadoop}

INSTALLED_HADOOP_DIR=${INSTALLED_HADOOP_DIR:-/usr/lib/hadoop}

HADOOP_BIN_DIR=${HADOOP_DIR}/bin
HADOOP_SBIN_DIR=${HADOOP_DIR}/sbin
HADOOP_LIB_DIR=${HADOOP_DIR}/lib
HADOOP_NATIVE_LIB_DIR=${HADOOP_LIB_DIR}/native

HADOOP_VERSION=0.23.0-SNAPSHOT

##Needed for some distros to find ldconfig
export PATH="/sbin/:$PATH"

# Make bin wrappers
mkdir -p $BIN_DIR

for bin_wrapper in hadoop yarn hdfs mapred; do
  wrapper=$BIN_DIR/$bin_wrapper
  cat > $wrapper <<EOF
#!/bin/sh


# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
. /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
. /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

. /etc/default/hadoop
. /etc/default/yarn

# FIXME: this might need to be fixed upstream
HADOOP_CLASSPATH="\${HADOOP_CLASSPATH}:\${YARN_CONF_DIR}"

exec $INSTALLED_HADOOP_DIR/bin/$bin_wrapper "\$@"
EOF
  chmod 755 $wrapper
done

#libexec
install -d -m 0755 ${SYSTEM_LIBEXEC_DIR}
rm -fv ${BUILD_DIR}/libexec/jsvc
mv ${BUILD_DIR}/libexec/* ${SYSTEM_LIBEXEC_DIR}/
mv ${BUILD_DIR}/bin/*-config.sh ${SYSTEM_LIBEXEC_DIR}/


# bin
install -d -m 0755 ${HADOOP_BIN_DIR}
cp -a ${BUILD_DIR}/bin/* ${HADOOP_BIN_DIR}/

# sbin
install -d -m 0755 ${HADOOP_SBIN_DIR}
cp ${BUILD_DIR}/sbin/* ${HADOOP_SBIN_DIR}/

# jars
install -d -m 0755 ${HADOOP_LIB_DIR}
cp ${BUILD_DIR}/lib/*.jar ${HADOOP_LIB_DIR}/
cp ${BUILD_DIR}/share/hadoop/common/lib/*.jar ${HADOOP_LIB_DIR}/
cp ${BUILD_DIR}/share/hadoop/hdfs/lib/*.jar ${HADOOP_LIB_DIR}/
chmod 644 ${HADOOP_LIB_DIR}/*.jar

# Remove duplicate libraries:
rm -fv ${HADOOP_LIB_DIR}/slf4j-*-1.5.11.jar
rm -fv ${HADOOP_LIB_DIR}/stax-api-1.0.1.jar
rm -fv ${HADOOP_LIB_DIR}/netty-3.2.3.Final.jar

# hadoop jar
install -d -m 0755 ${HADOOP_DIR}
cp ${BUILD_DIR}/modules/*.jar ${HADOOP_DIR}/
cp ${BUILD_DIR}/share/hadoop/common/*.jar ${HADOOP_DIR}/
cp ${BUILD_DIR}/share/hadoop/hdfs/*.jar ${HADOOP_DIR}/
mv ${HADOOP_LIB_DIR}/hadoop*.jar ${HADOOP_DIR}/
chmod 644 ${HADOOP_DIR}/*.jar

# native libs
install -d -m 0755 ${SYSTEM_LIB_DIR}
install -d -m 0755 ${HADOOP_NATIVE_LIB_DIR}
for library in libhdfs.so.0.0.0; do
  cp ${BUILD_DIR}/lib/${library} ${SYSTEM_LIB_DIR}/
  ldconfig -vlN ${SYSTEM_LIB_DIR}/${library}
done
install -d -m 0755 ${SYSTEM_INCLUDE_DIR}
cp ${BUILD_DIR}/../hadoop-hdfs-project/hadoop-hdfs/src/main/native/hdfs.h ${SYSTEM_INCLUDE_DIR}/

cp ${BUILD_DIR}/lib/*.a ${HADOOP_NATIVE_LIB_DIR}/
for library in `ls libsnappy.so.1.* 2>/dev/null` libhadoop.so.1.0.0; do
  cp ${BUILD_DIR}/lib/${library} ${HADOOP_NATIVE_LIB_DIR}/
  ldconfig -vlN ${HADOOP_NATIVE_LIB_DIR}/${library}
done

# conf
install -d -m 0755 $HADOOP_ETC_DIR/conf.empty

cp ${BUILD_DIR}/conf/* $HADOOP_ETC_DIR/conf.empty
cp ${BUILD_DIR}/etc/hadoop/* $HADOOP_ETC_DIR/conf.empty
cp $DISTRO_DIR/mrapp-generated-classpath $HADOOP_ETC_DIR/conf.empty

# docs
install -d -m 0755 ${DOC_DIR}
pushd  ${BUILD_DIR}/../
  cp hadoop-common-project/hadoop-common/CHANGES.txt target/staging/hadoop-project/hadoop-project-dist/hadoop-common
  cp hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt target/staging/hadoop-project/hadoop-project-dist/hadoop-hdfs
  mkdir target/staging/hadoop-project/hadoop-project-dist/hadoop-mapreduce
  cp hadoop-mapreduce-project/CHANGES.txt target/staging/hadoop-project/hadoop-project-dist/hadoop-mapreduce
popd
cp -r ${BUILD_DIR}/../target/staging/hadoop-project/* ${DOC_DIR}/

# man pages
mkdir -p $MAN_DIR/man1
gzip -c < $DISTRO_DIR/hadoop.1 > $MAN_DIR/man1/hadoop.1.gz
chmod 644 $MAN_DIR/man1/hadoop.1.gz

# Make the pseudo-distributed config
for conf in conf.pseudo ; do
  install -d -m 0755 $HADOOP_ETC_DIR/$conf
  # Overlay the -site files
  (cd $DISTRO_DIR/$conf && tar -cf - .) | (cd $HADOOP_ETC_DIR/$conf && tar -xf -)
  cp $DISTRO_DIR/mrapp-generated-classpath $HADOOP_ETC_DIR/$conf
done
cp ${BUILD_DIR}/etc/hadoop/log4j.properties $HADOOP_ETC_DIR/conf.pseudo

# Remove all hadoop test jars
rm -fv ${HADOOP_DIR}/*test*.jar

# Install webapps
cp -ra ${BUILD_DIR}/share/hadoop/hdfs/webapps ${HADOOP_DIR}/


# Create version-less symlinks to offer integration point with other projects
(cd $HADOOP_DIR &&
for j in hadoop-*.jar; do
  if [[ $j =~ hadoop-(.*)-${HADOOP_VERSION}.jar ]]; then
    name=${BASH_REMATCH[1]}
    ln -s $j hadoop-$name.jar
  fi
done)

