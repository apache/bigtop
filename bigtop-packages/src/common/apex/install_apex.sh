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
     --prefix=PREFIX             path to install into

  Optional options:
     --doc-dir=DIR               path to install docs into [/usr/share/doc/apex]
     --lib-dir=DIR               path to install Apex home [/usr/lib/apex]
     --bin-dir=DIR               path to install bins [/usr/bin]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'doc-dir:' \
  -l 'lib-dir:' \
  -l 'bin-dir:' -- "$@")

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
        --apex-dir)
        APEX_DIR=$2 ; shift 2
        ;;
        --bin-dir)
        BIN_DIR=$2 ; shift 2
        ;;
        --man-dir)
        MAN_DIR=$2 ; shift 2
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

for var in PREFIX ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

APEX_DIR=${APEX_DIR:-$PREFIX/usr/lib/apex}
BIN_DIR=${BIN_DIR:-$PREFIX/usr/bin}
DOC_DIR=${DOC_DIR:-$PREFIX/usr/share/doc/apex}
MAN_DIR=${MAN_DIR:-$PREFIX/usr/share/man/man1}

install -d -m 0755 $APEX_DIR
install -d -m 0755 $APEX_DIR/bin
install -d -m 0755 $APEX_DIR/lib
install -d -m 0755 $BIN_DIR
install -d -m 0755 $DOC_DIR
install -d -m 0755 $MAN_DIR

# Install apex cli
BINARY_FILE=$APEX_DIR/bin/apex
cp engine/src/main/scripts/apex $APEX_DIR/bin/apex
chmod 755 $BINARY_FILE

# Make bin wrappers
for file in $BIN_DIR/apex
do
  cat > $file <<EOF
#!/bin/bash
# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

exec ${BINARY_FILE#${PREFIX}} "\$@"
EOF
chmod 755 $file
done

# Libraries
cp -a deps/*.jar $APEX_DIR/lib

#Install man page
gzip -c apex.1 > $MAN_DIR/apex.1.gz

# Install license here.
cp LICENSE $DOC_DIR
cp *-LICENSE.txt $DOC_DIR
cp NOTICE $DOC_DIR
