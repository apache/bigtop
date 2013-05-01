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
     --build-dir=DIR             path to hcatalog/build/dist
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hcatalog]
     --lib-dir=DIR               path to install hcatalog home [/usr/lib/hcatalog]
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
DOC_DIR=${DOC_DIR:-/usr/share/hcatalog}
LIB_DIR=${LIB_DIR:-/usr/lib/hcatalog}
LIB_SHARE_DIR=${LIB_DIR}/share/hcatalog
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/hcatalog}
BIN_DIR=${BIN_DIR:-/usr/bin}


# First we'll move everything into lib
install -d -m 0755 ${PREFIX}/${LIB_DIR}
(cd $BUILD_DIR && tar -cf - .) | (cd ${PREFIX}/$LIB_DIR && tar -xf -)
install -d -m 0755 ${PREFIX}/${BIN_DIR}

# Take care of the configs
install -d -m 0755 ${PREFIX}/etc/default
for conf in `cd ${PREFIX}/${LIB_DIR}/etc ; ls -d *` ; do
  install -d -m 0755 ${PREFIX}/etc/$conf
  mv ${PREFIX}/${LIB_DIR}/etc/$conf ${PREFIX}/etc/$conf/conf.dist
  ln -s /etc/$conf/conf ${PREFIX}/${LIB_DIR}/etc/$conf
  touch ${PREFIX}/etc/default/$conf-server
done

wrapper=${PREFIX}/$BIN_DIR/hcat
cat >>$wrapper <<EOF
#!/bin/sh
. /etc/default/hadoop

# look for HBase
if [ -f /etc/default/hbase ] ; then
  . /etc/default/hbase
fi

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

# FIXME: HCATALOG-636 (and also HIVE-2757)
export HIVE_HOME=/usr/lib/hive
export HIVE_CONF_DIR=/etc/hive/conf
export HCAT_HOME=$INSTALLED_LIB_DIR

export HCATALOG_HOME=$INSTALLED_LIB_DIR
exec $INSTALLED_LIB_DIR/bin/hcat "\$@"
EOF
chmod 755 $wrapper

# Install the docs
install -d -m 0755 ${DOC_DIR}
mv ${PREFIX}/$LIB_DIR/share/doc/hcatalog/* ${DOC_DIR}
install -d -m 0755 ${PREFIX}/$MAN_DIR
gzip -c hcatalog.1 > ${PREFIX}/$MAN_DIR/hcatalog.1.gz

# Provide the runtime dirs
install -d -m 0755 $PREFIX/var/run/hcatalog
install -d -m 0755 $PREFIX/var/log/hcatalog
