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


usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to knox dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --home-dir=DIR               path to install knox home [/usr/lib/knox]
     --etc-knox=DIR             path to install knox conf [/etc/knox]
     ... [ see source for more similar options ]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'build-dir:' \
  -l 'home-dir:' \
  -l 'etc-knox:' -- "$@")

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
        --home-dir)
        HOME_DIR=$2 ; shift 2
        ;;
        --etc-knox)
        ETC_KNOX=$2 ; shift 2
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

HOME_DIR=${HOME_DIR:-/usr/lib/knox}
LIB_DIR=${LIB_DIR:-$HOME_DIR/lib}
DEP_DIR=${DEP_DIR:-$HOME_DIR/dep}
BIN_DIR=${BIN_DIR:-$HOME_DIR/bin}
CONF_DIR=${CONF_DIR:-/etc/knox}
SAMPLES_DIR=${SAMPLES_DIR:-$HOME_DIR/samples}
TEMPLATES_DIR=${TEMPLATES_DIR:-$HOME_DIR/templates}

DATA_DIR=${DATA_DIR:-/var/lib/knox/data}
RUN_DIR=${RUN_DIR:-/var/run/knox}
LOG_DIR=${LOG_DIR:-/var/log/knox}
NP_ETC_KNOX=/etc/knox

install -d -m 0755 $PREFIX/$HOME_DIR
install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$DEP_DIR
install -d -m 0755 $PREFIX/$BIN_DIR
install -d -m 0755 $PREFIX/$NP_ETC_KNOX
install -d -m 0755 $PREFIX/$CONF_DIR
install -d -m 0755 $PREFIX/$SAMPLES_DIR
install -d -m 0755 $PREFIX/$TEMPLATES_DIR
install -d -m 0755 $PREFIX/$DATA_DIR
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR

TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/target/*.*.*/knox-*.tar.gz -C $TMP_DIR

cp -ra $TMP_DIR/knox-*/lib/* ${PREFIX}/${LIB_DIR}
cp -ra $TMP_DIR/knox-*/dep/* ${PREFIX}/${DEP_DIR}
cp -ra $TMP_DIR/knox-*/bin/* ${PREFIX}/${BIN_DIR}
cp -ra $TMP_DIR/knox-*/conf/* ${PREFIX}/${CONF_DIR}
cp -ra $TMP_DIR/knox-*/samples ${PREFIX}/${SAMPLES_DIR}
cp -ra $TMP_DIR/knox-*/templates/* ${PREFIX}/${TEMPLATES_DIR}
cp -ra $TMP_DIR/knox-*/data/* ${PREFIX}/${DATA_DIR}

ln -s $CONF_DIR ${PREFIX}/$HOME_DIR/conf
ln -s $LOG_DIR ${PREFIX}/$HOME_DIR/logs
ln -s $DATA_DIR ${PREFIX}/$HOME_DIR/data
ln -s $RUN_DIR ${PREFIX}/$HOME_DIR/pids
ln -s $NP_ETC_KNOX/conf $PREFIX/$LIB_DIR/conf

rm -rf ${PREFIX}/README
rm -rf ${PREFIX}/native
rm -rf $TMP_DIR

# Copy in the /usr/bin/knox wrapper
install -d -m 0755 $PREFIX/usr/bin
cat > $PREFIX/usr/bin/gateway <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

export HOME_DIR=\${HOME_DIR}

exec $HOME_DIR/bin/gateway.sh \$@
EOF
chmod 755 $PREFIX/usr/bin/gateway