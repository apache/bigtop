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
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hadoop]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --man-dir=DIR               path to install mans [/usr/share/man]
     --etc-default=DIR           path to bigtop default dir [/etc/default]
     --hadoop-dir=DIR            path to install hadoop home [/usr/lib/hadoop]
     --hdfs-dir=DIR              path to install hdfs home [/usr/lib/hadoop-hdfs]
     --yarn-dir=DIR              path to install yarn home [/usr/lib/hadoop-yarn]
     --mapreduce-dir=DIR         path to install mapreduce home [/usr/lib/hadoop-mapreduce]
     --var-hdfs=DIR              path to install hdfs contents [/var/lib/hadoop-hdfs]
     --var-yarn=DIR              path to install yarn contents [/var/lib/hadoop-yarn]
     --var-mapreduce=DIR         path to install mapreduce contents [/var/lib/hadoop-mapreduce]
     --var-httpfs=DIR            path to install httpfs contents [/var/lib/hadoop-httpfs]
     --var-kms=DIR               path to install kms contents [/var/lib/hadoop-kms]
     --system-include-dir=DIR    path to install development headers [/usr/include]
     --system-lib-dir=DIR        path to install native libraries [/usr/lib]
     --etc-hadoop=DIR            path to install hadoop conf [/etc/hive]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'distro-dir:' \
  -l 'build-dir:' \
  -l 'prefix:' \
  -l 'doc-dir:' \
  -l 'bin-dir:' \
  -l 'man-dir:' \
  -l 'etc-default:' \
  -l 'hadoop-dir:' \
  -l 'hdfs-dir:' \
  -l 'yarn-dir:' \
  -l 'mapreduce-dir:' \
  -l 'var-hdfs:' \
  -l 'var-yarn:' \
  -l 'var-mapreduce:' \
  -l 'var-httpfs:' \
  -l 'var-kms:' \
  -l 'system-include-dir:' \
  -l 'system-lib-dir:' \
  -l 'etc-hadoop:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
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
        --var-hdfs)
        VAR_HDFS=$2 ; shift 2
        ;;
        --var-yarn)
        VAR_YARN=$2 ; shift 2
        ;;
        --var-mapreduce)
        VAR_MAPREDUCE=$2 ; shift 2
        ;;
        --var-httpfs)
        VAR_HTTPFS=$2 ; shift 2
        ;;
        --var-kms)
        VAR_KMS=$2 ; shift 2
        ;;
        --system-include-dir)
        SYSTEM_INCLUDE_DIR=$2 ; shift 2
        ;;
        --system-lib-dir)
        SYSTEM_LIB_DIR=$2 ; shift 2
        ;;
        --etc-hadoop)
        ETC_HADOOP=$2 ; shift 2
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

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/hadoop}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DEFAULT=${ETC_DEFAULT:-/etc/default}
HADOOP_DIR=${HADOOP_DIR:-/usr/lib/hadoop}
HDFS_DIR=${HDFS_DIR:-/usr/lib/hadoop-hdfs}
YARN_DIR=${YARN_DIR:-/usr/lib/hadoop-yarn}
MAPREDUCE_DIR=${MAPREDUCE_DIR:-/usr/lib/hadoop-mapreduce}
VAR_HDFS=${VAR_HDFS:-/var/lib/hadoop-hdfs}
VAR_YARN=${VAR_YARN:-/var/lib/hadoop-hdfs}
VAR_MAPREDUCE=${VAR_MAPREDUCE:-/var/lib/hadoop-mapreduce}
VAR_HTTPFS=${VAR_HTTPFS:-/var/lib/hadoop-httpfs}
VAR_KMS=${VAR_KMS:-/var/lib/hadoop-kms}
SYSTEM_INCLUDE_DIR=${SYSTEM_INCLUDE_DIR:-/usr/include}
SYSTEM_LIB_DIR=${SYSTEM_LIB_DIR:-/usr/lib}

BASH_COMPLETION_DIR=${BASH_COMPLETION_DIR:-/etc/bash_completion.d}
HADOOP_NATIVE_LIB_DIR=$HADOOP_DIR/lib/native

ETC_HADOOP=${ETC_HADOOP:-/etc/hadoop}
# No prefix
NP_ETC_HADOOP=/etc/hadoop

##Needed for some distros to find ldconfig
export PATH="/sbin/:$PATH"

# Make bin wrappers
mkdir -p $PREFIX/$BIN_DIR

for component in $PREFIX/$HADOOP_DIR/bin/hadoop $PREFIX/$HDFS_DIR/bin/hdfs $PREFIX/$YARN_DIR/bin/yarn $PREFIX/$MAPREDUCE_DIR/bin/mapred ; do
  wrapper=$PREFIX/$BIN_DIR/${component#*/bin/}
  cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_LIBEXEC_DIR=/$HADOOP_DIR/libexec

exec ${component#${PREFIX}} "\$@"
EOF
  chmod 755 $wrapper
done

#libexec
install -d -m 0755 $PREFIX/$HADOOP_DIR/libexec
cp -r ${BUILD_DIR}/libexec/* $PREFIX/$HADOOP_DIR/libexec/
cp ${DISTRO_DIR}/hadoop-layout.sh $PREFIX/$HADOOP_DIR/libexec/
install -m 0755 ${DISTRO_DIR}/init-hdfs.sh $PREFIX/$HADOOP_DIR/libexec/
install -m 0755 ${DISTRO_DIR}/init-hcfs.json $PREFIX/$HADOOP_DIR/libexec/
install -m 0755 ${DISTRO_DIR}/init-hcfs.groovy $PREFIX/$HADOOP_DIR/libexec/
rm -rf $PREFIX/$HADOOP_DIR/libexec/*.cmd

# hadoop jar
install -d -m 0755 $PREFIX/$HADOOP_DIR
cp ${BUILD_DIR}/share/hadoop/common/*.jar $PREFIX/$HADOOP_DIR/
cp ${BUILD_DIR}/share/hadoop/common/lib/hadoop-auth*.jar $PREFIX/$HADOOP_DIR/
cp ${BUILD_DIR}/share/hadoop/common/lib/hadoop-annotations*.jar $PREFIX/$HADOOP_DIR/
install -d -m 0755 $PREFIX/$HADOOP_DIR/tools
install -d -m 0755 $PREFIX/$MAPREDUCE_DIR
cp ${BUILD_DIR}/share/hadoop/mapreduce/hadoop-mapreduce*.jar $PREFIX/$MAPREDUCE_DIR
install -d -m 0755 $PREFIX/$HDFS_DIR
cp ${BUILD_DIR}/share/hadoop/hdfs/*.jar $PREFIX/$HDFS_DIR/
install -d -m 0755 $PREFIX/$YARN_DIR
cp ${BUILD_DIR}/share/hadoop/yarn/hadoop-yarn*.jar $PREFIX/$YARN_DIR/
install -d -m 0755 $PREFIX/$YARN_DIR/timelineservice
cp ${BUILD_DIR}/share/hadoop/yarn/timelineservice/hadoop-yarn*.jar $PREFIX/$YARN_DIR/timelineservice
chmod 644 $PREFIX/$HADOOP_DIR/*.jar $PREFIX/$MAPREDUCE_DIR/*.jar $PREFIX/$HDFS_DIR/*.jar $PREFIX/$YARN_DIR/*.jar

# lib jars
install -d -m 0755 $PREFIX/$HADOOP_DIR/lib
cp ${BUILD_DIR}/share/hadoop/common/lib/*.jar $PREFIX/$HADOOP_DIR/lib
install -d -m 0755 $PREFIX/$HADOOP_DIR/tools/lib
cp ${BUILD_DIR}/share/hadoop/tools/lib/*.jar $PREFIX/$HADOOP_DIR/tools/lib
install -d -m 0755 $PREFIX/$HDFS_DIR/lib 
cp ${BUILD_DIR}/share/hadoop/hdfs/lib/*.jar $PREFIX/$HDFS_DIR/lib
install -d -m 0755 $PREFIX/$YARN_DIR/lib
cp ${BUILD_DIR}/share/hadoop/yarn/lib/*.jar $PREFIX/$YARN_DIR/lib
install -d -m 0755 $PREFIX/$YARN_DIR/timelineservice/lib
cp ${BUILD_DIR}/share/hadoop/yarn/timelineservice/lib/*.jar $PREFIX/$YARN_DIR/timelineservice/lib
chmod 644 $PREFIX/$HADOOP_DIR/lib/*.jar $PREFIX/$HDFS_DIR/lib/*.jar $PREFIX/$YARN_DIR/lib/*.jar $PREFIX/$YARN_DIR/timelineservice/lib/*.jar

# Install webapps
cp -ra ${BUILD_DIR}/share/hadoop/hdfs/webapps $PREFIX/$HDFS_DIR/
cp -ra ${BUILD_DIR}/share/hadoop/yarn/webapps $PREFIX/$YARN_DIR/

# bin
install -d -m 0755 $PREFIX/$HADOOP_DIR/bin
cp -a ${BUILD_DIR}/bin/{hadoop,fuse_dfs} $PREFIX/$HADOOP_DIR/bin
install -d -m 0755 $PREFIX/$HDFS_DIR/bin
cp -a ${BUILD_DIR}/bin/hdfs $PREFIX/$HDFS_DIR/bin
install -d -m 0755 $PREFIX/$YARN_DIR/bin
cp -a ${BUILD_DIR}/bin/{yarn,container-executor} $PREFIX/$YARN_DIR/bin
install -d -m 0755 $PREFIX/$MAPREDUCE_DIR/bin
cp -a ${BUILD_DIR}/bin/mapred $PREFIX/$MAPREDUCE_DIR/bin
# FIXME: MAPREDUCE-3980
cp -a ${BUILD_DIR}/bin/mapred $PREFIX/$YARN_DIR/bin

# sbin
install -d -m 0755 $PREFIX/$HADOOP_DIR/sbin
cp -a ${BUILD_DIR}/sbin/{hadoop-daemon,hadoop-daemons,workers,httpfs,kms}.sh $PREFIX/$HADOOP_DIR/sbin
install -d -m 0755 $PREFIX/$HDFS_DIR/sbin
cp -a ${BUILD_DIR}/sbin/{distribute-exclude,refresh-namenodes}.sh $PREFIX/$HDFS_DIR/sbin
install -d -m 0755 $PREFIX/$YARN_DIR/sbin
cp -a ${BUILD_DIR}/sbin/{yarn-daemon,yarn-daemons}.sh $PREFIX/$YARN_DIR/sbin
install -d -m 0755 $PREFIX/$MAPREDUCE_DIR/sbin
cp -a ${BUILD_DIR}/sbin/mr-jobhistory-daemon.sh $PREFIX/$MAPREDUCE_DIR/sbin

# native libs
install -d -m 0755 $PREFIX/$SYSTEM_LIB_DIR
install -d -m 0755 $PREFIX/$HADOOP_NATIVE_LIB_DIR

for library in libhdfs.so.0.0.0 libhdfspp.so.0.1.0 ; do
  cp ${BUILD_DIR}/lib/native/${library} $PREFIX/$SYSTEM_LIB_DIR/
  ldconfig -vlN $PREFIX/$SYSTEM_LIB_DIR/${library}
  ln -s ${library} $PREFIX/$SYSTEM_LIB_DIR/${library/.so.*/}.so
done

install -d -m 0755 $PREFIX/$SYSTEM_INCLUDE_DIR
cp ${BUILD_DIR}/include/hdfs.h $PREFIX/$SYSTEM_INCLUDE_DIR/
cp -r ${BUILD_DIR}/include/hdfspp $PREFIX/$SYSTEM_INCLUDE_DIR/

cp ${BUILD_DIR}/lib/native/*.a $PREFIX/$HADOOP_NATIVE_LIB_DIR/
for library in `cd ${BUILD_DIR}/lib/native ; ls libsnappy.so.1.* 2>/dev/null` libhadoop.so.1.0.0 libnativetask.so.1.0.0; do
  cp ${BUILD_DIR}/lib/native/${library} $PREFIX/$HADOOP_NATIVE_LIB_DIR/
  ldconfig -vlN $PREFIX/$HADOOP_NATIVE_LIB_DIR/${library}
  ln -s ${library} $PREFIX/$HADOOP_NATIVE_LIB_DIR/${library/.so.*/}.so
done

# Install fuse wrapper
fuse_wrapper=$PREFIX/$BIN_DIR/hadoop-fuse-dfs
cat > $fuse_wrapper << EOF
#!/bin/bash

/sbin/modprobe fuse

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HADOOP_HOME=\${HADOOP_HOME:-$HADOOP_DIR}

BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-$ETC_DEFAULT}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/hadoop-fuse ] && . \${BIGTOP_DEFAULTS_DIR}/hadoop-fuse

export HADOOP_LIBEXEC_DIR=$HADOOP_DIR/libexec

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
install -d -m 0755 $PREFIX/$BASH_COMPLETION_DIR
install -m 0644 \
  hadoop-common-project/hadoop-common/src/contrib/bash-tab-completion/hadoop.sh \
  $PREFIX/$BASH_COMPLETION_DIR/hadoop

# conf
install -d -m 0755 $PREFIX/$NP_ETC_HADOOP
install -d -m 0755 $PREFIX/$ETC_HADOOP/conf.empty
cp ${DISTRO_DIR}/conf.empty/mapred-site.xml $PREFIX/$ETC_HADOOP/conf.empty
# disable everything that's definied in hadoop-env.sh
# so that it can still be used as example, but doesn't affect anything
# by default
sed -i -e '/^[^#]/s,^,#,' ${BUILD_DIR}/etc/hadoop/hadoop-env.sh
cp -r ${BUILD_DIR}/etc/hadoop/* $PREFIX/$ETC_HADOOP/conf.empty
rm -rf $PREFIX/$ETC_HADOOP/conf.empty/*.cmd

# docs
install -d -m 0755 $PREFIX/$DOC_DIR
cp -r ${BUILD_DIR}/share/doc/* $PREFIX/$DOC_DIR/

# man pages
mkdir -p $PREFIX/$MAN_DIR
for manpage in hadoop hdfs yarn mapred; do
	gzip -c < $DISTRO_DIR/$manpage.1 > $PREFIX/$MAN_DIR/$manpage.1.gz
	chmod 644 $PREFIX/$MAN_DIR/$manpage.1.gz
done

# HTTPFS
install -d -m 0755 ${PREFIX}/${VAR_HTTPFS}

# KMS
install -d -m 0755 ${PREFIX}/${VAR_KMS}


for conf in conf.pseudo ; do
  install -d -m 0755 $PREFIX/$ETC_HADOOP/$conf
  # Install the upstream config files
  cp -r ${BUILD_DIR}/etc/hadoop/* $PREFIX/$ETC_HADOOP/$conf
  # Remove the ones that shouldn't be installed
  rm -rf $PREFIX/$ETC_HADOOP/$conf/*.cmd
  # Overlay the -site files
  (cd $DISTRO_DIR/$conf && tar -cf - .) | (cd $PREFIX/$ETC_HADOOP/$conf && tar -xf -)
  find $PREFIX/$ETC_HADOOP/$conf/ -type f -print -exec chmod 0644 {} \;
  find $PREFIX/$ETC_HADOOP/$conf/ -type d -print -exec chmod 0755 {} \;
  # When building straight out of svn we have to account for pesky .svn subdirs 
  rm -rf `find $PREFIX/$ETC_HADOOP/$conf -name .svn -type d` 
done
cp ${BUILD_DIR}/etc/hadoop/log4j.properties $PREFIX/$ETC_HADOOP/conf.pseudo

# FIXME: Provide a convenience link for configuration (HADOOP-7939)
install -d -m 0755 $PREFIX/$HADOOP_DIR/etc
ln -s $NP_ETC_HADOOP/conf $PREFIX/$HADOOP_DIR/etc/hadoop
install -d -m 0755 $PREFIX/$YARN_DIR/etc
ln -s $NP_ETC_HADOOP/conf $PREFIX/$YARN_DIR/etc/hadoop

# Create log, var and lib
install -d -m 0755 ${PREFIX}/${VAR_HDFS}
install -d -m 0755 ${PREFIX}/${VAR_YARN}
install -d -m 0755 ${PREFIX}/${VAR_MAPREDUCE}
install -d -m 0755 $PREFIX/var/{log,run}/hadoop-hdfs
install -d -m 0755 $PREFIX/var/{log,run}/hadoop-yarn
install -d -m 0755 $PREFIX/var/{log,run}/hadoop-mapreduce

# Remove all source and create version-less symlinks to offer integration point with other projects
for DIR in $PREFIX/$HADOOP_DIR $PREFIX/$HDFS_DIR $PREFIX/$YARN_DIR $PREFIX/$MAPREDUCE_DIR ; do
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
install -d -m 0755 $PREFIX/$HADOOP_DIR/client
for file in `cat ${BUILD_DIR}/hadoop-client.list` ; do
  for dir in $PREFIX/$HADOOP_DIR/{lib,} $PREFIX/$HDFS_DIR/{lib,} $PREFIX/$YARN_DIR/{lib,} $PREFIX/$MAPREDUCE_DIR/{lib,} ; do
    [ -e $dir/$file ] && \
    ln -fs ${dir#$PREFIX}/$file $PREFIX/$HADOOP_DIR/client/${file} && \
    ln -fs ${dir#$PREFIX}/$file $PREFIX/$HADOOP_DIR/client/${file/-[[:digit:]]*/.jar} && \
    continue 2
  done
  exit 1
done
