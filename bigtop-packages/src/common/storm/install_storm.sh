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

set -xe

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to storm dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/storm]
     --lib-dir=DIR               path to install storm home [/usr/lib/storm]
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
  -l 'examples-dir:' \
  -l 'conf-dir:' \
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
        --installed-lib-dir)
        INSTALLED_LIB_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --examples-dir)
        EXAMPLES_DIR=$2 ; shift 2
        ;;
        --conf-dir)
        CONF_DIR=$2 ; shift 2
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

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/storm}
LIB_DIR=${LIB_DIR:-/usr/lib/storm}
BIN_DIR=${BIN_DIR:-/usr/lib/storm/bin}
ETC_DIR=${ETC_DIR:-/etc/storm}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf.dist}


install -d -m 0755 ${PREFIX}/$LIB_DIR/
install -d -m 0755 ${PREFIX}/$LIB_DIR/lib

install -d -m 0755 ${PREFIX}/$LIB_DIR/contrib
install -d -m 0755 ${PREFIX}/$LIB_DIR/log4j2
install -d -m 0755 ${PREFIX}/$LIB_DIR/public
install -d -m 0755 ${PREFIX}/$LIB_DIR/extlib
install -d -m 0755 ${PREFIX}/$LIB_DIR/extlib-daemon
install -d -m 0755 ${PREFIX}/$DOC_DIR
install -d -m 0755 ${PREFIX}/$BIN_DIR
install -d -m 0755 ${PREFIX}/$ETC_DIR
install -d -m 0755 ${PREFIX}/$MAN_DIR

cp $BUILD_DIR/CHANGELOG.md ${PREFIX}/${LIB_DIR}/
cp $BUILD_DIR/RELEASE ${PREFIX}/${LIB_DIR}/
cp $BUILD_DIR/README.markdown ${PREFIX}/${LIB_DIR}/
rm -f $BUILD_DIR/lib/zookeeper-*.jar
cp -ra $BUILD_DIR/lib/* ${PREFIX}/$LIB_DIR/lib/
cp -ra $BUILD_DIR/log4j2/* ${PREFIX}/$LIB_DIR/log4j2/
cp -ra $BUILD_DIR/public/* ${PREFIX}/$LIB_DIR/public/
cp -ra $BUILD_DIR/bin/* ${PREFIX}/$BIN_DIR
ln -sf /usr/lib/zookeeper/zookeeper.jar ${PREFIX}/$LIB_DIR/lib/

if [ ! -e "${PREFIX}/${ETC_DIR}" ]; then
    rm -f ${PREFIX}/${ETC_DIR}
    mkdir -p ${PREFIX}/${ETC_DIR}
fi
cp -a $BUILD_DIR/conf ${PREFIX}/$CONF_DIR
ln -s /etc/storm/conf ${PREFIX}/$LIB_DIR/conf


# Copy in the /usr/bin/storm wrapper
mv ${PREFIX}/$BIN_DIR/storm ${PREFIX}/$BIN_DIR/storm.distro

cat > ${PREFIX}/$BIN_DIR/storm <<EOF
#!/bin/bash
. /etc/default/hadoop

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-/etc/default}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/storm ] && . \${BIGTOP_DEFAULTS_DIR}/storm

exec /usr/lib/storm/bin/storm.distro "\$@"
EOF

chmod 755 ${PREFIX}/${BIN_DIR}/storm

cp -ra $BUILD_DIR/external/* ${PREFIX}/$LIB_DIR/contrib/
cp -ra $BUILD_DIR/examples/* ${PREFIX}/$LIB_DIR/contrib/
