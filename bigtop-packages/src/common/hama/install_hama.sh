#!/bin/sh

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
     --build-dir=DIR             path to Hama dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --distro-dir=DIR            path to distro specific files (debian/RPM)
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hama]
     --lib-dir=DIR               path to install hama home [/usr/lib/hama]
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
  -l 'etc-dir:' \
  -l 'distro-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
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
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
        ;;
        --etc-dir)
        ETC_DIR=$2 ; shift 2
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

for var in PREFIX BUILD_DIR DISTRO_DIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/hama}
LIB_DIR=${LIB_DIR:-/usr/lib/hama}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/hama}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}

CONF_DIR=/etc/hama/
CONF_DIST_DIR=${CONF_DIR}/conf.dist/
ETC_DIR=${ETC_DIR:-${CONF_DIST_DIR}}

install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/conf
install -d -m 0755 $PREFIX/$DOC_DIR
install -d -m 0755 $PREFIX/$ETC_DIR
install -d -m 0777 $PREFIX/$LIB_DIR/logs

cp -ra ${BUILD_DIR}/lib/* $PREFIX/${LIB_DIR}/lib/
cp ${BUILD_DIR}/hama*.jar $PREFIX/$LIB_DIR
cp -a ${BUILD_DIR}/*.txt $PREFIX/$DOC_DIR
cp -a ${BUILD_DIR}/docs/* $PREFIX/$DOC_DIR
cp -a ${BUILD_DIR}/bin/* $PREFIX/${LIB_DIR}/bin
cp -a ${BUILD_DIR}/conf/* $PREFIX/${ETC_DIR}

# Remove unnecessary files
for f in hama-daemon.sh hama-daemons.sh start-bspd.sh stop-bspd.sh zookeepers.sh; do
  rm -fv $PREFIX/${LIB_DIR}/bin/$f
done

# Copy in the /usr/bin/hama wrapper
install -d -m 0755 $PREFIX/$BIN_DIR
cat > $PREFIX/$BIN_DIR/hama <<EOF

#!/bin/sh

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

. /etc/default/hadoop
. /etc/default/hama

#FIXME: the following line is a workaround for BIGTOP-259
export HADOOP_CLASSPATH="`echo /usr/lib/hama/hama-examples-*-job.jar`":\$HADOOP_CLASSPATH
exec $INSTALLED_LIB_DIR/bin/hama "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/hama


# Make the pseudo-distributed config
for conf in conf.pseudo ; do
  install -d -m 0755 $PREFIX/$CONF_DIR/$conf
  # Install the default configurations
  (cd ${BUILD_DIR}/conf && tar -cf - .) | (cd $PREFIX/$CONF_DIR/$conf && tar -xf -)
  # Overlay the -site files
  (cd $DISTRO_DIR/$conf && tar -cf - .) | (cd $PREFIX/$CONF_DIR/$conf && tar -xf -)
  # When building straight out of svn we have to account for pesky .svn subdirs
  rm -rf `find $PREFIX/$CONF_DIR/$conf -name .svn -type d`
done




