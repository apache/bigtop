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
     --prefix=PREFIX                  path to install into
     --source-dir=DIR                 path to package shared files dir
     --qfs-version=VERSION            version of qfs we are installing
     --python=PYTHON                  the path to python

  Optional options:
     --bin-dir=DIR                    path to install binaries (default: /usr/bin)
     --lib-dir=DIR                    path to install libraries (default: /usr/lib)
     --etc-dir=DIR                    path to install configuration (default: /etc/qfs)
     --include-dir=DIR                path to install devel headers (default: /usr/include)
     --data-dir=DIR                   path to install various data (default: /usr/share/qfs)
     --var-dir=DIR                    path to keep variable data (default: /var/qfs)
     --hadoop-home=DIR                path where hadoop lives (default: /usr/lib/hadoop)
     --python3=PYTHON3                path to python 3
     --python-extra=PYTHON_EXTRA      extra arguments to pass to python when installing

  Note: you will have to change the init scripts with the new paths you use
  above if you deviate from the defaults.
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'source-dir:' \
  -l 'prefix:' \
  -l 'bin-dir:' \
  -l 'lib-dir:' \
  -l 'etc-dir:' \
  -l 'include-dir:' \
  -l 'data-dir:' \
  -l 'var-dir:' \
  -l 'hadoop-home:' \
  -l 'qfs-version:' \
  -l 'python:' \
  -l 'python3:' \
  -l 'python-extra:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --source-dir)
            SOURCE_DIR=$2 ; shift 2
            ;;
        --prefix)
            PREFIX=$2 ; shift 2
            ;;
        --bin-dir)
            BIN_DIR=$2 ; shift 2
            ;;
        --lib-dir)
            LIB_DIR=$2 ; shift 2
            ;;
        --etc-dir)
            ETC_DIR=$2 ; shift 2
            ;;
        --include-dir)
            INCLUDE_DIR=$2 ; shift 2
            ;;
        --data-dir)
            DATA_DIR=$2 ; shift 2
            ;;
        --var-dir)
            VAR_DIR=$2 ; shift 2
            ;;
        --hadoop-home)
            HADOOP_HOME=$2 ; shift 2
            ;;
        --qfs-version)
            QFS_VERSION=$2 ; shift 2
            ;;
        --python)
            PYTHON_PATH=$2 ; shift 2
            ;;
        --python3)
            PYTHON3_PATH=$2 ; shift 2
            ;;
        --python-extra)
            PYTHON_EXTRA=$2 ; shift 2
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

for var in SOURCE_DIR PREFIX QFS_VERSION PYTHON_PATH ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing required param: $var
    usage
  fi
done

if [ -f "$SOURCE_DIR/bigtop.bom" ]; then
  . $SOURCE_DIR/bigtop.bom
fi

PREFIX=$(readlink -f $PREFIX)
BIN_DIR=$PREFIX/${BIN_DIR:-/usr/bin/qfs}
LIB_DIR=$PREFIX/${LIB_DIR:-/usr/lib/qfs}
ETC_DIR=$PREFIX/${ETC_DIR:-/etc/qfs}
INCLUDE_DIR=$PREFIX/${INCLUDE_DIR:-/usr/include}
DATA_DIR=$PREFIX/${DATA_DIR:-/usr/share/qfs}
VAR_DIR=$PREFIX/${VAR_DIR:-/var/qfs}
HADOOP_HOME=$PREFIX/${HADOOP_HOME:-/usr/lib/hadoop}
PYTHON_EXTRA=${PYTHON_EXTRA:-}

mkdir -p $BIN_DIR
install -m 755 build/release/bin/filelister $BIN_DIR
install -m 755 build/release/bin/logcompactor $BIN_DIR
install -m 755 build/release/bin/metaserver $BIN_DIR
install -m 755 build/release/bin/qfsfsck $BIN_DIR
install -m 755 build/release/bin/qfs_fuse $BIN_DIR
install -m 755 build/release/bin/chunkserver $BIN_DIR
install -m 755 build/release/bin/chunkscrubber $BIN_DIR

install -m 755 build/release/bin/devtools/checksum $BIN_DIR
install -m 755 build/release/bin/devtools/dirtree_creator $BIN_DIR
install -m 755 build/release/bin/devtools/dtokentest $BIN_DIR
install -m 755 build/release/bin/devtools/logger $BIN_DIR/qfslogger
install -m 755 build/release/bin/devtools/rand-sfmt $BIN_DIR
install -m 755 build/release/bin/devtools/requestparser $BIN_DIR
install -m 755 build/release/bin/devtools/sortedhash $BIN_DIR
install -m 755 build/release/bin/devtools/sslfiltertest $BIN_DIR
install -m 755 build/release/bin/devtools/stlset $BIN_DIR

install -m 755 build/release/bin/emulator/rebalanceexecutor $BIN_DIR
install -m 755 build/release/bin/emulator/rebalanceplanner $BIN_DIR
install -m 755 build/release/bin/emulator/replicachecker $BIN_DIR

install -m 755 build/release/bin/examples/qfssample $BIN_DIR

install -m 755 build/release/bin/tools/cpfromqfs $BIN_DIR
install -m 755 build/release/bin/tools/cptoqfs $BIN_DIR
install -m 755 build/release/bin/tools/qfs $BIN_DIR
install -m 755 build/release/bin/tools/qfsadmin $BIN_DIR
install -m 755 build/release/bin/tools/qfscat $BIN_DIR
install -m 755 build/release/bin/tools/qfsdataverify $BIN_DIR
install -m 755 build/release/bin/tools/qfsfileenum $BIN_DIR
install -m 755 build/release/bin/tools/qfshibernate $BIN_DIR
install -m 755 build/release/bin/tools/qfsping $BIN_DIR
install -m 755 build/release/bin/tools/qfsput $BIN_DIR
install -m 755 build/release/bin/tools/qfsshell $BIN_DIR
install -m 755 build/release/bin/tools/qfsstats $BIN_DIR
install -m 755 build/release/bin/tools/qfstoggleworm $BIN_DIR

install -m 755 scripts/qfs_backup $BIN_DIR
install -m 755 scripts/qfs_checkpoint_prune.py $BIN_DIR/qfs_checkpoint_prune
install -m 755 scripts/qfs_log_prune.py $BIN_DIR/qfs_log_prune

mkdir -p $LIB_DIR
install -m 644 build/release/lib/libgf_complete.so $LIB_DIR
install -m 644 build/release/lib/libgf_complete.so.1 $LIB_DIR
install -m 644 build/release/lib/libgf_complete.so.1.0.0 $LIB_DIR
install -m 644 build/release/lib/libJerasure.so $LIB_DIR
install -m 644 build/release/lib/libJerasure.so.2 $LIB_DIR
install -m 644 build/release/lib/libJerasure.so.2.0.0 $LIB_DIR
install -m 644 build/release/lib/libqfs_access.so $LIB_DIR
install -m 644 build/release/lib/libqfs_client.so $LIB_DIR
install -m 644 build/release/lib/libqfs_common.so $LIB_DIR
install -m 644 build/release/lib/libqfsc.so $LIB_DIR
install -m 644 build/release/lib/libqfs_io.so $LIB_DIR
install -m 644 build/release/lib/libqfskrb.so $LIB_DIR
install -m 644 build/release/lib/libqfs_qcdio.so $LIB_DIR
install -m 644 build/release/lib/libqfs_qcrs.so $LIB_DIR

install -m 644 build/release/lib/static/libgf_complete.a $LIB_DIR
install -m 644 build/release/lib/static/libJerasure.a $LIB_DIR
install -m 644 build/release/lib/static/libqfsc.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_client.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_common.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_emulator.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_io.a $LIB_DIR
install -m 644 build/release/lib/static/libqfskrb.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_meta.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_qcdio.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_qcrs.a $LIB_DIR
install -m 644 build/release/lib/static/libqfs_tools.a $LIB_DIR

mkdir -p $ETC_DIR/logrotate.d
install -m 644 contrib/logrotate/qfs-chunkserver $ETC_DIR/logrotate.d
install -m 644 contrib/logrotate/qfs-metaserver $ETC_DIR/logrotate.d
install -m 644 contrib/logrotate/qfs-webui $ETC_DIR/logrotate.d

mkdir -p $INCLUDE_DIR
cp -a build/release/include/* $INCLUDE_DIR

mkdir -p $DATA_DIR/webui
cp -rp webui/* $DATA_DIR/webui

mkdir -p $DATA_DIR/java
install -m 644 build/java/qfs-access/qfs-access-${QFS_VERSION}.jar $DATA_DIR/java

mkdir -p $HADOOP_HOME/lib
install -m 644 build/java/hadoop-qfs/hadoop-${HADOOP_VERSION}-qfs-${QFS_VERSION}.jar ${HADOOP_HOME}/lib

cd build/release
$PYTHON_PATH ../../src/cc/access/kfs_setup.py install --skip-build --root=$PREFIX --prefix=/usr $PYTHON_EXTRA
if [ ! -z "$PYTHON3_PATH" ]; then
    $PYTHON3_PATH ../../src/cc/access/kfs_setup.py install --skip-build --root=$PREFIX --prefix=/usr $PYTHON_EXTRA
fi
cd ..

mkdir -p $VAR_DIR/metaserver/checkpoint
mkdir -p $VAR_DIR/log/qfs
mkdir -p $VAR_DIR/lib/qfs
