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
  -l 'hdfs-dir:' \
  -l 'yarn-dir:' \
  -l 'mapreduce-dir:' \
  -l 'client-dir:' \
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
        --hdfs-dir)
        HDFS_DIR=$2 ; shift 2
        ;;
        --yarn-dir)
        YARN_DIR=$2 ; shift 2
        ;;
        --mapreduce-dir)
        MAPREDUCE_DIR=$2 ; shift 2
        ;;
        --client-dir)
        CLIENT_DIR=$2 ; shift 2
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
HDFS_DIR=${HDFS_DIR:-$PREFIX/usr/lib/hadoop-hdfs}
YARN_DIR=${YARN_DIR:-$PREFIX/usr/lib/hadoop-yarn}
MAPREDUCE_DIR=${MAPREDUCE_DIR:-$PREFIX/usr/lib/hadoop-mapreduce}
CLIENT_DIR=${CLIENT_DIR:-$PREFIX/usr/lib/hadoop/client}
SYSTEM_LIB_DIR=${SYSTEM_LIB_DIR:-/usr/lib}
BIN_DIR=${BIN_DIR:-$PREFIX/usr/bin}
DOC_DIR=${DOC_DIR:-$PREFIX/usr/share/doc/hadoop}
MAN_DIR=${MAN_DIR:-$PREFIX/usr/man}
SYSTEM_INCLUDE_DIR=${SYSTEM_INCLUDE_DIR:-$PREFIX/usr/include}
SYSTEM_LIBEXEC_DIR=${SYSTEM_LIBEXEC_DIR:-$PREFIX/usr/libexec}
EXAMPLE_DIR=${EXAMPLE_DIR:-$DOC_DIR/examples}
HADOOP_ETC_DIR=${HADOOP_ETC_DIR:-$PREFIX/etc/hadoop}
BASH_COMPLETION_DIR=${BASH_COMPLETION_DIR:-$PREFIX/etc/bash_completion.d}

INSTALLED_HADOOP_DIR=${INSTALLED_HADOOP_DIR:-/usr/lib/hadoop}
HADOOP_NATIVE_LIB_DIR=${HADOOP_DIR}/lib/native

##Needed for some distros to find ldconfig
export PATH="/sbin/:$PATH"

# Make bin wrappers
mkdir -p $BIN_DIR

for component in $HADOOP_DIR/bin/hadoop $HDFS_DIR/bin/hdfs $YARN_DIR/bin/yarn $MAPREDUCE_DIR/bin/mapred ; do
  wrapper=$BIN_DIR/${component#*/bin/}
  cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_LIBEXEC_DIR=/${SYSTEM_LIBEXEC_DIR#${PREFIX}}

exec ${component#${PREFIX}} "\$@"
EOF
  chmod 755 $wrapper
done

#libexec
install -d -m 0755 ${SYSTEM_LIBEXEC_DIR}
cp -r ${BUILD_DIR}/libexec/* ${SYSTEM_LIBEXEC_DIR}/
cp ${DISTRO_DIR}/hadoop-layout.sh ${SYSTEM_LIBEXEC_DIR}/
install -m 0755 ${DISTRO_DIR}/init-hdfs.sh ${SYSTEM_LIBEXEC_DIR}/
install -m 0755 ${DISTRO_DIR}/init-hcfs.json ${SYSTEM_LIBEXEC_DIR}/
install -m 0755 ${DISTRO_DIR}/init-hcfs.groovy ${SYSTEM_LIBEXEC_DIR}/
rm -rf ${SYSTEM_LIBEXEC_DIR}/*.cmd

# hadoop jar
install -d -m 0755 ${HADOOP_DIR}
cp ${BUILD_DIR}/share/hadoop/common/*.jar ${HADOOP_DIR}/
cp ${BUILD_DIR}/share/hadoop/common/lib/hadoop-auth*.jar ${HADOOP_DIR}/
cp ${BUILD_DIR}/share/hadoop/common/lib/hadoop-annotations*.jar ${HADOOP_DIR}/
install -d -m 0755 ${HADOOP_DIR}/tools
install -d -m 0755 ${MAPREDUCE_DIR}
cp ${BUILD_DIR}/share/hadoop/mapreduce/hadoop-mapreduce*.jar ${MAPREDUCE_DIR}
install -d -m 0755 ${HDFS_DIR}
cp ${BUILD_DIR}/share/hadoop/hdfs/*.jar ${HDFS_DIR}/
install -d -m 0755 ${YARN_DIR}
cp ${BUILD_DIR}/share/hadoop/yarn/hadoop-yarn*.jar ${YARN_DIR}/
install -d -m 0755 ${YARN_DIR}/timelineservice
cp ${BUILD_DIR}/share/hadoop/yarn/timelineservice/hadoop-yarn*.jar ${YARN_DIR}/timelineservice
chmod 644 ${HADOOP_DIR}/*.jar ${MAPREDUCE_DIR}/*.jar ${HDFS_DIR}/*.jar ${YARN_DIR}/*.jar

# lib jars
install -d -m 0755 ${HADOOP_DIR}/lib
cp ${BUILD_DIR}/share/hadoop/common/lib/*.jar ${HADOOP_DIR}/lib
install -d -m 0755 ${HADOOP_DIR}/tools/lib
cp ${BUILD_DIR}/share/hadoop/tools/lib/*.jar ${HADOOP_DIR}/tools/lib
install -d -m 0755 ${HDFS_DIR}/lib 
cp ${BUILD_DIR}/share/hadoop/hdfs/lib/*.jar ${HDFS_DIR}/lib
install -d -m 0755 ${YARN_DIR}/lib
cp ${BUILD_DIR}/share/hadoop/yarn/lib/*.jar ${YARN_DIR}/lib
install -d -m 0755 ${YARN_DIR}/timelineservice/lib
cp ${BUILD_DIR}/share/hadoop/yarn/timelineservice/lib/*.jar ${YARN_DIR}/timelineservice/lib
chmod 644 ${HADOOP_DIR}/lib/*.jar ${HDFS_DIR}/lib/*.jar ${YARN_DIR}/lib/*.jar ${YARN_DIR}/timelineservice/lib/*.jar

# Install webapps
cp -ra ${BUILD_DIR}/share/hadoop/hdfs/webapps ${HDFS_DIR}/
cp -ra ${BUILD_DIR}/share/hadoop/yarn/webapps ${YARN_DIR}/

# bin
install -d -m 0755 ${HADOOP_DIR}/bin
cp -a ${BUILD_DIR}/bin/{hadoop,fuse_dfs} ${HADOOP_DIR}/bin
install -d -m 0755 ${HDFS_DIR}/bin
cp -a ${BUILD_DIR}/bin/hdfs ${HDFS_DIR}/bin
install -d -m 0755 ${YARN_DIR}/bin
cp -a ${BUILD_DIR}/bin/{yarn,container-executor} ${YARN_DIR}/bin
install -d -m 0755 ${MAPREDUCE_DIR}/bin
cp -a ${BUILD_DIR}/bin/mapred ${MAPREDUCE_DIR}/bin
# FIXME: MAPREDUCE-3980
cp -a ${BUILD_DIR}/bin/mapred ${YARN_DIR}/bin

# sbin
install -d -m 0755 ${HADOOP_DIR}/sbin
cp -a ${BUILD_DIR}/sbin/{hadoop-daemon,hadoop-daemons,workers,httpfs,kms}.sh ${HADOOP_DIR}/sbin
install -d -m 0755 ${HDFS_DIR}/sbin
cp -a ${BUILD_DIR}/sbin/{distribute-exclude,refresh-namenodes}.sh ${HDFS_DIR}/sbin
install -d -m 0755 ${YARN_DIR}/sbin
cp -a ${BUILD_DIR}/sbin/{yarn-daemon,yarn-daemons}.sh ${YARN_DIR}/sbin
install -d -m 0755 ${MAPREDUCE_DIR}/sbin
cp -a ${BUILD_DIR}/sbin/mr-jobhistory-daemon.sh ${MAPREDUCE_DIR}/sbin

# native libs
install -d -m 0755 ${SYSTEM_LIB_DIR}
install -d -m 0755 ${HADOOP_NATIVE_LIB_DIR}

for library in libhdfs.so.0.0.0 libhdfspp.so.0.1.0 ; do
  cp ${BUILD_DIR}/lib/native/${library} ${SYSTEM_LIB_DIR}/
  ldconfig -vlN ${SYSTEM_LIB_DIR}/${library}
  ln -s ${library} ${SYSTEM_LIB_DIR}/${library/.so.*/}.so
done

install -d -m 0755 ${SYSTEM_INCLUDE_DIR}
cp ${BUILD_DIR}/include/hdfs.h ${SYSTEM_INCLUDE_DIR}/
cp -r ${BUILD_DIR}/include/hdfspp ${SYSTEM_INCLUDE_DIR}/

cp ${BUILD_DIR}/lib/native/*.a ${HADOOP_NATIVE_LIB_DIR}/
for library in `cd ${BUILD_DIR}/lib/native ; ls libsnappy.so.1.* 2>/dev/null` libhadoop.so.1.0.0 libnativetask.so.1.0.0; do
  cp ${BUILD_DIR}/lib/native/${library} ${HADOOP_NATIVE_LIB_DIR}/
  ldconfig -vlN ${HADOOP_NATIVE_LIB_DIR}/${library}
  ln -s ${library} ${HADOOP_NATIVE_LIB_DIR}/${library/.so.*/}.so
done

# Install fuse wrapper
fuse_wrapper=${BIN_DIR}/hadoop-fuse-dfs
cat > $fuse_wrapper << EOF
#!/bin/bash

/sbin/modprobe fuse

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_HOME=\${HADOOP_HOME:-${HADOOP_DIR#${PREFIX}}}

BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-/etc/default}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/hadoop-fuse ] && . \${BIGTOP_DEFAULTS_DIR}/hadoop-fuse

export HADOOP_LIBEXEC_DIR=${SYSTEM_LIBEXEC_DIR#${PREFIX}}

if [ "\${LD_LIBRARY_PATH}" = "" ]; then
  export JAVA_NATIVE_LIBS="libjvm.so"
  . /usr/lib/bigtop-utils/bigtop-detect-javalibs
  export LD_LIBRARY_PATH=\${JAVA_NATIVE_PATH}:/usr/lib
fi

# Pulls all jars from hadoop client package
for jar in \${HADOOP_HOME}/client/*.jar; do
  CLASSPATH+="\$jar:"
done
CLASSPATH="/etc/hadoop/conf:\${CLASSPATH}"

env CLASSPATH="\${CLASSPATH}" \${HADOOP_HOME}/bin/fuse_dfs \$@
EOF

chmod 755 $fuse_wrapper

# Bash tab completion
install -d -m 0755 $BASH_COMPLETION_DIR
install -m 0644 \
  hadoop-common-project/hadoop-common/src/contrib/bash-tab-completion/hadoop.sh \
  $BASH_COMPLETION_DIR/hadoop

# conf
install -d -m 0755 $HADOOP_ETC_DIR/conf.empty
cp ${DISTRO_DIR}/conf.empty/mapred-site.xml $HADOOP_ETC_DIR/conf.empty
# disable everything that's definied in hadoop-env.sh
# so that it can still be used as example, but doesn't affect anything
# by default
sed -i -e '/^[^#]/s,^,#,' ${BUILD_DIR}/etc/hadoop/hadoop-env.sh
cp -r ${BUILD_DIR}/etc/hadoop/* $HADOOP_ETC_DIR/conf.empty
rm -rf $HADOOP_ETC_DIR/conf.empty/*.cmd

# docs
install -d -m 0755 ${DOC_DIR}
cp -r ${BUILD_DIR}/share/doc/* ${DOC_DIR}/

# man pages
mkdir -p $MAN_DIR/man1
for manpage in hadoop hdfs yarn mapred; do
	gzip -c < $DISTRO_DIR/$manpage.1 > $MAN_DIR/man1/$manpage.1.gz
	chmod 644 $MAN_DIR/man1/$manpage.1.gz
done

# HTTPFS
install -d -m 0755 ${PREFIX}/var/lib/hadoop-httpfs

# KMS
install -d -m 0755 ${PREFIX}/var/lib/hadoop-kms


for conf in conf.pseudo ; do
  install -d -m 0755 $HADOOP_ETC_DIR/$conf
  # Install the upstream config files
  cp -r ${BUILD_DIR}/etc/hadoop/* $HADOOP_ETC_DIR/$conf
  # Remove the ones that shouldn't be installed
  rm -rf $HADOOP_ETC_DIR/$conf/*.cmd
  # Overlay the -site files
  (cd $DISTRO_DIR/$conf && tar -cf - .) | (cd $HADOOP_ETC_DIR/$conf && tar -xf -)
  find $HADOOP_ETC_DIR/$conf/ -type f -print -exec chmod 0644 {} \;
  find $HADOOP_ETC_DIR/$conf/ -type d -print -exec chmod 0755 {} \;
  # When building straight out of svn we have to account for pesky .svn subdirs 
  rm -rf `find $HADOOP_ETC_DIR/$conf -name .svn -type d` 
done
cp ${BUILD_DIR}/etc/hadoop/log4j.properties $HADOOP_ETC_DIR/conf.pseudo

# FIXME: Provide a convenience link for configuration (HADOOP-7939)
install -d -m 0755 ${HADOOP_DIR}/etc
ln -s ${HADOOP_ETC_DIR##${PREFIX}}/conf ${HADOOP_DIR}/etc/hadoop
install -d -m 0755 ${YARN_DIR}/etc
ln -s ${HADOOP_ETC_DIR##${PREFIX}}/conf ${YARN_DIR}/etc/hadoop

# Create log, var and lib
install -d -m 0755 $PREFIX/var/{log,run,lib}/hadoop-hdfs
install -d -m 0755 $PREFIX/var/{log,run,lib}/hadoop-yarn
install -d -m 0755 $PREFIX/var/{log,run,lib}/hadoop-mapreduce

# Remove all source and create version-less symlinks to offer integration point with other projects
for DIR in ${HADOOP_DIR} ${HDFS_DIR} ${YARN_DIR} ${MAPREDUCE_DIR} ; do
  (cd $DIR &&
   rm -fv *-sources.jar
   for j in hadoop-*.jar; do
     if [[ $j =~ hadoop-(.*)-${HADOOP_VERSION}.jar ]]; then
       name=${BASH_REMATCH[1]}
       ln -s $j hadoop-$name.jar
     fi
   done)
done

# Now create a client installation area full of symlinks
install -d -m 0755 ${CLIENT_DIR}
for file in `cat ${BUILD_DIR}/hadoop-client.list` ; do
  for dir in ${HADOOP_DIR}/{lib,} ${HDFS_DIR}/{lib,} ${YARN_DIR}/{lib,} ${MAPREDUCE_DIR}/{lib,} ; do
    [ -e $dir/$file ] && \
    ln -fs ${dir#$PREFIX}/$file ${CLIENT_DIR}/${file} && \
    ln -fs ${dir#$PREFIX}/$file ${CLIENT_DIR}/${file/-[[:digit:]]*/.jar} && \
    continue 2
  done
  exit 1
done
