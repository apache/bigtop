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

set -ex

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to flumedist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/flume]
     --lib-dir=DIR               path to install flume home [/usr/lib/flume]
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

MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
DOC_DIR=${DOC_DIR:-/usr/share/doc/flume}
LIB_DIR=${LIB_DIR:-/usr/lib/flume}
BIN_DIR=${BIN_DIR:-/usr/lib/flume/bin}
CONF_DIST_DIR=/etc/flume/conf.dist/
ETC_DIR=${ETC_DIR:-/etc/flume}

install -d -m 0755 ${PREFIX}/${LIB_DIR}

(cd ${PREFIX}/${LIB_DIR} &&
  tar --strip-components=1 -xvzf ${BUILD_DIR}/flume-ng-dist/target/*flume-*-bin.tar.gz)

# Take out useless things or we've installed elsewhere
for x in flume-ng-* \
          .gitignore \
          conf \
          pom.xml \
          CHANGELOG \
          DEVNOTES \
          DISCLAIMER \
          LICENSE \
          NOTICE \
          README.md \
          RELEASE-NOTES \
          doap_Flume.rdf \
          bin/ia64 \
          bin/amd64; do
  rm -rf ${PREFIX}/$LIB_DIR/$x 
done


wrapper=$PREFIX/usr/bin/flume-ng
mkdir -p `dirname $wrapper`
cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

if [ -n "\$FLUME_PID_FILE" ]; then
  echo \$$ > \$FLUME_PID_FILE
fi

# See FLUME-920
exec bash /usr/lib/flume/bin/flume-ng "\$@"
EOF
chmod 755 $wrapper


install -d -m 0755 $PREFIX/$ETC_DIR/conf.empty
(cd ${BUILD_DIR}/conf && tar cf - .) | (cd $PREFIX/$ETC_DIR/conf.empty && tar xf -)
# XXX FIXME: We should handle this upstream more gracefully
sed -i -e "s|flume\.log\.dir=.*|flume.log.dir=/var/log/flume|" $PREFIX/$ETC_DIR/conf.empty/log4j.properties
touch $PREFIX/$ETC_DIR/conf.empty/flume.conf

(cd $PREFIX/$ETC_DIR/conf.empty/; cp flume-env.sh.template flume-env.sh)
cat >> $PREFIX/$ETC_DIR/conf.empty/flume-env.sh <<EOF

HADOOP_HOME=\${HADOOP_HOME:-/usr/lib/hadoop}

EOF

unlink $PREFIX/$LIB_DIR/conf || /bin/true
ln -s /etc/flume/conf $PREFIX/$LIB_DIR/conf


mkdir -p $PREFIX/$LIB_DIR/plugins.d

# Docs
rm -rf $PREFIX/$LIB_DIR/docs
install -d -m 0755 $PREFIX/${DOC_DIR}
for x in CHANGELOG \
          DEVNOTES \
          LICENSE \
          NOTICE \
          README.md \
          RELEASE-NOTES \
          doap_Flume.rdf ; do
  if [ -e $x ] ; then
    cp -r $x $PREFIX/${DOC_DIR}
  fi
done

# Remove Windows files
rm -rf ${PREFIX}/${BIN_DIR}/*.cmd
rm -rf ${PREFIX}/${BIN_DIR}/*.ps1
