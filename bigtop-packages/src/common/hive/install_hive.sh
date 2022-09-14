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
     --build-dir=DIR             path to hive/build/dist
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/hive]
     --bin-dir=DIR               path to install bins [/usr/bin]
     --man-dir=DIR               path to install mans [/usr/share/man]
     --etc-default=DIR           path to bigtop default dir [/etc/default]
     --hive-dir=DIR              path to install hive home [/usr/lib/hive]
     --var-hive-dir=DIR          path to install hive contents [/var/lib/hive]
     --hcatalog-dir=DIR          path to install hcatalog home [/usr/lib/hcatalog]
     --var-hcatalog-dir=DIR      path to install hcatalog contents [/var/lib/hcatalog]
     --etc-hive=DIR              path to install hive conf [/etc/hive]
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
  -l 'bin-dir:' \
  -l 'man-dir:' \
  -l 'etc-default:' \
  -l 'hive-dir:' \
  -l 'var-hive-dir:' \
  -l 'hcatalog-dir:' \
  -l 'var-hcatalog-dir:' \
  -l 'etc-hive:' \
  -l 'examples-dir:' \
  -l 'python-dir:' \
  -l 'build-dir:' \
  -l 'hive-version:' -- "$@")

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
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
        ;;
        --etc-default)
        ETC_DEFAULT=$2 ; shift 2
        ;;
        --hive-dir)
        HIVE_DIR=$2 ; shift 2
        ;;
        --var-hive-dir)
        VAR_HIVE_DIR=$2 ; shift 2
        ;;
        --hcatalog-dir)
        HCATALOG_DIR=$2 ; shift 2
        ;;
        --var-hcatalog-dir)
        VAR_HCATALOG_DIR=$2 ; shift 2
        ;;
        --etc-hive)
        ETC_HIVE=$2 ; shift 2
        ;;
        --examples-dir)
        EXAMPLES_DIR=$2 ; shift 2
        ;;
        --python-dir)
        PYTHON_DIR=$2 ; shift 2
        ;;
        --hive-version)
        HIVE_VERSION=$2 ; shift 2
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

MAN_DIR=${MAN_DIR:-/usr/share/man}/man1
DOC_DIR=${DOC_DIR:-/usr/share/doc/hive}
BIN_DIR=${BIN_DIR:-/usr/bin}
ETC_DEFAULT=${ETC_DEFAULT:-/etc/default}
HIVE_DIR=${HIVE_DIR:-/usr/lib/hive}
VAR_HIVE_DIR=${VAR_HIVE_DIR:-/var/lib/hive}
HCATALOG_DIR=${HCATALOG_DIR:-/usr/lib/hive-hcatalog}
VAR_HCATALOG_DIR=${VAR_HCATALOG_DIR:-/var/lib/hive-hcatalog}
HCATALOG_SHARE_DIR=${HCATALOG_DIR}/share/hcatalog
EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
PYTHON_DIR=${PYTHON_DIR:-$HIVE_DIR/lib/py}

ETC_HIVE=${ETC_HIVE:-/etc/hive}
# No prefix
NP_ETC_HIVE=/etc/hive

# First we'll move everything into lib
install -d -m 0755 $PREFIX/$HIVE_DIR
(cd ${BUILD_DIR} && tar -cf - .)|(cd $PREFIX/$HIVE_DIR && tar -xf -)
rm -f $PREFIX/$HIVE_DIR/lib/hive-shims-0.2*.jar
for jar in `ls $PREFIX/$HIVE_DIR/lib/hive-*.jar | grep -v 'standalone.jar'`; do
    base=`basename $jar`
    (cd $PREFIX/$HIVE_DIR/lib && ln -s $base ${base/-[0-9].*/.jar})
done

for thing in conf README.txt examples lib/py;
do
  rm -rf $PREFIX/$HIVE_DIR/$thing
done

install -d -m 0755 $PREFIX/$BIN_DIR
for file in hive beeline hiveserver2
do
  wrapper=$PREFIX/$BIN_DIR/$file
  cat >>$wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
if [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-$ETC_DEFAULT}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/hbase ] && . \${BIGTOP_DEFAULTS_DIR}/hbase

export HIVE_HOME=$HIVE_DIR
exec $HIVE_DIR/bin/$file "\$@"
EOF
  chmod 755 $wrapper
done

# Config
install -d -m 0755 $PREFIX/$NP_ETC_HIVE
install -d -m 0755 $PREFIX/$ETC_HIVE/conf.dist
(cd ${BUILD_DIR}/conf && tar -cf - .)|(cd $PREFIX/$ETC_HIVE/conf.dist && tar -xf -)
for template in hive-exec-log4j2.properties hive-log4j2.properties
do
  mv $PREFIX/$ETC_HIVE/conf.dist/${template}.template $PREFIX/$ETC_HIVE/conf.dist/${template}
done
cp hive-site.xml $PREFIX/$ETC_HIVE/conf.dist
sed -i -e "s|@VERSION@|${HIVE_VERSION}|" $PREFIX/$ETC_HIVE/conf.dist/hive-site.xml

ln -s $NP_ETC_HIVE/conf $PREFIX/$HIVE_DIR/conf

install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c hive.1 > $PREFIX/$MAN_DIR/hive.1.gz

# Docs
install -d -m 0755 $PREFIX/$DOC_DIR
mv $PREFIX/$HIVE_DIR/NOTICE $PREFIX/$DOC_DIR
mv $PREFIX/$HIVE_DIR/LICENSE $PREFIX/$DOC_DIR
mv $PREFIX/$HIVE_DIR/RELEASE_NOTES.txt $PREFIX/$DOC_DIR


# Examples
install -d -m 0755 $PREFIX/$EXAMPLES_DIR
cp -a ${BUILD_DIR}/examples/* $PREFIX/$EXAMPLES_DIR

# Python libs
install -d -m 0755 $PREFIX/$PYTHON_DIR
(cd $BUILD_DIR/lib/py && tar cf - .) | (cd $PREFIX/$PYTHON_DIR && tar xf -)
chmod 755 $PREFIX/$PYTHON_DIR/hive_metastore/*-remote

# Dir for Metastore DB
install -d -m 1777 $PREFIX/$VAR_HIVE_DIR/metastore/

# We need to remove the .war files. No longer supported.
rm -f $PREFIX/$HIVE_DIR/lib/hive-hwi*.war

# Remove some source which gets installed
rm -rf $PREFIX/$HIVE_DIR/lib/php/ext

install -d -m 0755 $PREFIX/$HCATALOG_DIR
mv $PREFIX/$HIVE_DIR/hcatalog/* $PREFIX/$HCATALOG_DIR
rm -rf $PREFIX/$HIVE_DIR/hcatalog

# Workaround for HIVE-5534
find $PREFIX/$HCATALOG_DIR -name *.sh | xargs chmod 755
chmod 755 $PREFIX/$HCATALOG_DIR/bin/hcat

install -d -m 0755 $PREFIX/$ETC_DEFAULT
for conf in `cd $PREFIX/$HCATALOG_DIR/etc ; ls -d *` ; do
  install -d -m 0755 ${PREFIX}/$NP_ETC_HIVE-$conf
  install -d -m 0755 ${PREFIX}/$ETC_HIVE-$conf
  mv $PREFIX/$HCATALOG_DIR/etc/$conf ${PREFIX}/$ETC_HIVE-$conf/conf.dist
  ln -s $NP_ETC_HIVE-$conf/conf $PREFIX/$HCATALOG_DIR/etc/$conf
  touch $PREFIX/$ETC_DEFAULT/hive-$conf-server
done

wrapper=$PREFIX/$BIN_DIR/hcat
cat >>$wrapper <<EOF
#!/bin/sh


BIGTOP_DEFAULTS_DIR=\${BIGTOP_DEFAULTS_DIR-$ETC_DEFAULT}
[ -n "\${BIGTOP_DEFAULTS_DIR}" -a -r \${BIGTOP_DEFAULTS_DIR}/hadoop ] && . \${BIGTOP_DEFAULTS_DIR}/hadoop

# Autodetect JAVA_HOME if not defined
if [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

# FIXME: HCATALOG-636 (and also HIVE-2757)
export HIVE_HOME=$HIVE_DIR
export HIVE_CONF_DIR=$NP_ETC_HIVE/conf
export HCAT_HOME=$HCATALOG_DIR

export HCATALOG_HOME=$HCATALOG_DIR
exec $HCATALOG_DIR/bin/hcat "\$@"
EOF
chmod 755 $wrapper

# Install the docs
install -d -m 0755 $PREFIX/$DOC_DIR
mv $PREFIX/$HCATALOG_DIR/share/doc/hcatalog/* $PREFIX/$DOC_DIR
# Might as delete the directory since it's empty now
rm -rf $PREFIX/$HCATALOG_DIR/share/doc
install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c hive-hcatalog.1 > $PREFIX/$MAN_DIR/hive-hcatalog.1.gz

# Provide the runtime dirs
install -d -m 0755 $PREFIX/$VAR_HIVE_DIR
install -d -m 0755 $PREFIX/var/log/hive

install -d -m 0755 $PREFIX/$VAR_HCATALOG_DIR
install -d -m 0755 $PREFIX/var/log/hive-hcatalog
for DIR in $PREFIX/$HCATALOG_SHARE_DIR ; do
    (cd $DIR &&
     for j in hive-hcatalog-*.jar; do
       if [[ $j =~ hive-hcatalog-(.*)-${HIVE_VERSION}.jar ]]; then
         name=${BASH_REMATCH[1]}
         ln -s $j hive-hcatalog-$name.jar
       fi
    done)
done

# Remove Windows files
find $PREFIX/$HIVE_DIR/bin -name '*.cmd' | xargs rm -f
find $PREFIX/$HCATALOG_DIR/bin -name '*.cmd' | xargs rm -f
