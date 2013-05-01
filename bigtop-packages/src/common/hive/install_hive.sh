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

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to hive/build/dist
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hive]
     --lib-dir=DIR               path to install hive home [/usr/lib/hive]
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
  -l 'python-dir:' \
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
        --python-dir)
        PYTHON_DIR=$2 ; shift 2
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/hive}
LIB_DIR=${LIB_DIR:-/usr/lib/hive}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/hive}
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
BIN_DIR=${BIN_DIR:-/usr/bin}
PYTHON_DIR=${PYTHON_DIR:-$LIB_DIR/lib/py}
CONF_DIR=/etc/hive
CONF_DIST_DIR=/etc/hive/conf.dist

# First we'll move everything into lib
install -d -m 0755 ${PREFIX}/${LIB_DIR}
(cd ${BUILD_DIR} && tar -cf - .)|(cd ${PREFIX}/${LIB_DIR} && tar -xf -)

for thing in conf README.txt examples lib/py;
do
  rm -rf ${PREFIX}/${LIB_DIR}/$thing
done

install -d -m 0755 ${PREFIX}/${BIN_DIR}
for file in hive
do
  wrapper=${PREFIX}/$BIN_DIR/$file
  cat >>$wrapper <<EOF
#!/bin/sh

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

# look for HBase
if [ -f /etc/default/hbase ] ; then
  . /etc/default/hbase
fi

export HIVE_HOME=$INSTALLED_LIB_DIR
exec $INSTALLED_LIB_DIR/bin/$file "\$@"
EOF
  chmod 755 $wrapper
done

# Config
install -d -m 0755 ${PREFIX}${CONF_DIST_DIR}
(cd ${BUILD_DIR}/conf && tar -cf - .)|(cd ${PREFIX}${CONF_DIST_DIR} && tar -xf -)
for template in hive-exec-log4j.properties hive-log4j.properties
do
  mv ${PREFIX}${CONF_DIST_DIR}/${template}.template ${PREFIX}${CONF_DIST_DIR}/${template}
done
cp hive-site.xml ${PREFIX}${CONF_DIST_DIR}
sed -i -e "s|@VERSION@|${HIVE_VERSION}|" ${PREFIX}${CONF_DIST_DIR}/hive-site.xml

ln -s ${CONF_DIR}/conf $PREFIX/$LIB_DIR/conf

install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c hive.1 > $PREFIX/$MAN_DIR/hive.1.gz

# Docs
install -d -m 0755 ${PREFIX}/${DOC_DIR}
cp ${BUILD_DIR}/README.txt ${PREFIX}/${DOC_DIR}
mv ${PREFIX}/${LIB_DIR}/NOTICE ${PREFIX}/${DOC_DIR}
mv ${PREFIX}/${LIB_DIR}/LICENSE ${PREFIX}/${DOC_DIR}
mv ${PREFIX}/${LIB_DIR}/RELEASE_NOTES.txt ${PREFIX}/${DOC_DIR}


# Examples
install -d -m 0755 ${PREFIX}/${EXAMPLES_DIR}
cp -a ${BUILD_DIR}/examples/* ${PREFIX}/${EXAMPLES_DIR}

# Python libs
install -d -m 0755 ${PREFIX}/${PYTHON_DIR}
(cd $BUILD_DIR/lib/py && tar cf - .) | (cd ${PREFIX}/${PYTHON_DIR} && tar xf -)
chmod 755 ${PREFIX}/${PYTHON_DIR}/hive_metastore/*-remote

# Dir for Metastore DB
install -d -m 1777 $PREFIX/var/lib/hive/metastore/

# Remove some source which gets installed
rm -rf ${PREFIX}/${LIB_DIR}/lib/php/ext
