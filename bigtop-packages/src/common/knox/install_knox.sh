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
     --lib-dir=DIR               path to install knox home [/usr/lib/knox]
     --bin-dir=DIR               path to install bins [/usr/bin]
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
  -l 'bin-dir:' \
  -l 'lib-dir:' \
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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --lib-dir)
        LIB_DIR=$2 ; shift 2
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

LIB_DIR=${LIB_DIR:-/usr/lib/knox}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_KNOX=${ETC_KNOX:-/etc/knox}
RUN_DIR=${RUN_DIR:-/var/run/knox}
LOG_DIR=${LOG_DIR:-/var/log/knox}

NP_ETC_KNOX=/etc/knox
NP_VAR_LIB_KNOX_DATA=/var/lib/knox/data


install -d -m 0755 $PREFIX/$LIB_DIR
install -d -m 0755 $PREFIX/$LIB_DIR/bin
install -d -m 0755 $PREFIX/$LIB_DIR/lib
install -d -m 0755 $PREFIX/$LIB_DIR/dep
install -d -m 0755 $PREFIX/$NP_ETC_KNOX
install -d -m 0755 $PREFIX/$NP_VAR_LIB_KNOX_DATA
install -d -m 0755 $PREFIX/$ETC_KNOX/conf.dist
install -d -m 0755 $PREFIX/$LIB_DIR/samples
install -d -m 0755 $PREFIX/$LIB_DIR/templates
install -d -m 0755 $PREFIX/$RUN_DIR
install -d -m 0755 $PREFIX/$LOG_DIR

TMP_DIR=$BUILD_DIR/tmp
mkdir -p $BUILD_DIR/tmp
tar -zxf $BUILD_DIR/target/*.*.*/knox-*.tar.gz -C $TMP_DIR

cp -ra ${TMP_DIR}/knox-*/dep/* ${PREFIX}/${LIB_DIR}/dep/
cp -ra ${TMP_DIR}/knox-*/lib/* ${PREFIX}/${LIB_DIR}/lib/
cp -a ${TMP_DIR}/knox-*/bin/* ${PREFIX}/${LIB_DIR}/bin/
cp -a ${TMP_DIR}/knox-*/samples/* ${PREFIX}/${LIB_DIR}/samples/
cp -a ${TMP_DIR}/knox-*/templates/* ${PREFIX}/${LIB_DIR}/templates/
cp -ra ${TMP_DIR}/knox-*/data/* ${PREFIX}/${NP_VAR_LIB_KNOX_DATA}
cp -ra ${TMP_DIR}/knox-*/conf/* ${PREFIX}/${ETC_KNOX}/conf.dist

ln -s $NP_ETC_KNOX/conf $PREFIX/$LIB_DIR/conf
ln -s $NP_VAR_LIB_KNOX_DATA $PREFIX/$LIB_DIR/data
ln -s $LOG_DIR $PREFIX/$LIB_DIR/logs
ln -s $RUN_DIR $PREFIX/$LIB_DIR/pids

rm -rf $TMP_DIR

# Copy in the /usr/bin/knox wrapper
install -d -m 0755 $PREFIX/$BIN_DIR
cat > $PREFIX/$BIN_DIR/gateway <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec $LIB_DIR/bin/gateway.sh \$@
EOF
chmod 755 $PREFIX/$BIN_DIR/gateway
