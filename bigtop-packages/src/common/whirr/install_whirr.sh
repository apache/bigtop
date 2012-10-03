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

set -e

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to Whirr dist.dir
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/whirr]
     --lib-dir=DIR               path to install Whirr home [/usr/lib/whirr]
     --installed-lib-dir=DIR     path where lib-dir will end up on target system
     --bin-dir=DIR               path to install bins [/usr/bin]
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
DOC_DIR=${DOC_DIR:-/usr/share/doc/whirr}
LIB_DIR=${LIB_DIR:-/usr/lib/whirr}
INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/whirr}
BIN_DIR=${BIN_DIR:-/usr/bin}

# First we'll move everything into lib
install -d -m 0755 $PREFIX/$LIB_DIR
(cd $BUILD_DIR && tar -cf - .) | (cd $PREFIX/$LIB_DIR && tar -xf -)

# Copy in the /usr/bin/whirr wrapper
install -d -m 0755 $PREFIX/$BIN_DIR
cat > $PREFIX/$BIN_DIR/whirr <<EOF
#!/bin/sh

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

exec $INSTALLED_LIB_DIR/bin/whirr "\$@"
EOF
chmod 755 $PREFIX/$BIN_DIR/whirr

install -d -m 0755 $PREFIX/$MAN_DIR
gzip -c whirr.1 > $PREFIX/$MAN_DIR/whirr.1.gz

# Move the docs, but leave a symlink in place for compat. reasons
install -d -m 0755 $PREFIX/$DOC_DIR
mv $PREFIX/$LIB_DIR/docs/* $PREFIX/$DOC_DIR
mv $PREFIX/$LIB_DIR/{NOTICE.txt,LICENSE.txt,BUILD.txt,CHANGES.txt,doap_Whirr.rdf,README.txt} $PREFIX/$DOC_DIR
rmdir $PREFIX/$LIB_DIR/docs
ln -s /${DOC_DIR/#$PREFIX/} $PREFIX/$LIB_DIR/docs

# Remove some bits which sould not be shipped.
for dir in src services pom.xml patch-stamp examples debian core cli build-tools bigtop-empty 
do
  rm -rf $PREFIX/$LIB_DIR/$dir
done
