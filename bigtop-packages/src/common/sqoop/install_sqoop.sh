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
     --build-dir=DIR             path to sqoopdist.dir
     --prefix=PREFIX             path to install into
     --extra-dir=DIR             path to Bigtop distribution files

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/sqoop]
     --lib-dir=DIR               path to install sqoop home [/usr/lib/sqoop]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
     --conf-dir=DIR              path to configuration files provided by the package [/etc/sqoop/conf.dist]
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
  -l 'conf-dir:' \
  -l 'installed-lib-dir:' \
  -l 'bin-dir:' \
  -l 'examples-dir:' \
  -l 'extra-dir:' \
  -l 'build-dir:' -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
set -ex
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
        --conf-dir)
        CONF_DIR=$2 ; shift 2
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
        --extra-dir)
        EXTRA_DIR=$2 ; shift 2
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

DOC_DIR=${DOC_DIR:-/usr/share/doc/sqoop}
LIB_DIR=${LIB_DIR:-/usr/lib/sqoop}
BIN_DIR=${BIN_DIR:-/usr/lib/sqoop/bin}
ETC_DIR=${ETC_DIR:-/etc/sqoop}
MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
CONF_DIR=${CONF_DIR:-${ETC_DIR}/conf.dist}

install -d -m 0755 ${PREFIX}/${LIB_DIR}

install -d -m 0755 ${PREFIX}/${LIB_DIR}
cp ${BUILD_DIR}/sqoop*.jar ${PREFIX}/${LIB_DIR}

install -d -m 0755 ${PREFIX}/${LIB_DIR}/lib
cp -a ${BUILD_DIR}/lib/*.jar ${PREFIX}/${LIB_DIR}/lib

#install -d -m 0755 ${PREFIX}/${LIB_DIR}/shims
#cp -a shims/*.jar ${PREFIX}/${LIB_DIR}/shims

install -d -m 0755 $PREFIX/usr/bin

install -d -m 0755 $PREFIX/${BIN_DIR}
for f in `ls ${BUILD_DIR}/bin/ | grep -v .cmd`; do
    cp ${BUILD_DIR}/bin/$f $PREFIX/${BIN_DIR}
done


install -d -m 0755 $PREFIX/${DOC_DIR}
cp ${BUILD_DIR}/docs/*.html  $PREFIX/${DOC_DIR}
cp ${BUILD_DIR}/docs/*.css $PREFIX/${DOC_DIR}
cp -r ${BUILD_DIR}/docs/api $PREFIX/${DOC_DIR}
cp -r ${BUILD_DIR}/docs/images $PREFIX/${DOC_DIR}


install -d -m 0755 $PREFIX/$MAN_DIR
for i in sqoop sqoop-codegen sqoop-export sqoop-import-all-tables sqoop-version sqoop-create-hive-table sqoop-help sqoop-list-databases sqoop-eval sqoop-import sqoop-list-tables sqoop-job sqoop-metastore sqoop-merge
    do echo "Copying manpage $i"
    cp ${BUILD_DIR}/docs/man/$i* $PREFIX/$MAN_DIR
    echo "Creating wrapper for $i"
    wrapper=$PREFIX/usr/bin/$i
    mkdir -p `dirname $wrapper`
    cat > $wrapper <<EOF
#!/bin/bash

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

SQOOP_JARS=\`ls /var/lib/sqoop/*.jar 2>/dev/null\`
if [ -n "\${SQOOP_JARS}" ]; then
    export HADOOP_CLASSPATH=\$(JARS=(\${SQOOP_JARS}); IFS=:; echo "\${HADOOP_CLASSPATH}:\${JARS[*]}")
fi

export SQOOP_HOME=$LIB_DIR
exec $BIN_DIR/$i "\$@"
EOF
   chmod 0755 $wrapper
done

install -d -m 0755 $PREFIX/$CONF_DIR
(cd ${BUILD_DIR}/conf && tar cf - .) | (cd $PREFIX/$CONF_DIR && tar xf - && rm -rf *.cmd .gitignore)

unlink $PREFIX/$LIB_DIR/conf || /bin/true
ln -s $ETC_DIR/conf $PREFIX/$LIB_DIR/conf

install -d -m 0755 ${PREFIX}/var/lib/sqoop

cp ${BUILD_DIR}/{LICENSE,NOTICE}.txt ${PREFIX}/${LIB_DIR}/

