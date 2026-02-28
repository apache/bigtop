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
  Required:
    --distro-dir=DIR    path to distro-specific files (SOURCES)
    --build-dir=DIR     path to Nutch source tree (contains runtime/local and runtime/deploy)
    --prefix=PREFIX     path to install into (e.g. \$RPM_BUILD_ROOT)

  Optional:
    --bin-dir=DIR       path for binaries [/usr/bin]
    --lib-dir=DIR       path for Nutch home [/usr/lib/nutch]
    --etc-default=DIR   path for defaults [/etc/default]
    --conf-dir=DIR      path for config [/etc/nutch]
  "
  exit 1
}

OPTS=$(getopt -n $0 -o '' \
  -l 'prefix:' -l 'distro-dir:' -l 'build-dir:' \
  -l 'bin-dir:' -l 'lib-dir:' -l 'etc-default:' -l 'conf-dir:' -- "$@")
[ $? = 0 ] || usage
eval set -- "$OPTS"

PREFIX= BUILD_DIR= DISTRO_DIR= BIN_DIR= LIB_DIR= ETC_DEFAULT= CONF_DIR=
while true; do
  case "$1" in
    --prefix)       PREFIX=$2; shift 2 ;;
    --distro-dir)   DISTRO_DIR=$2; shift 2 ;;
    --build-dir)    BUILD_DIR=$2; shift 2 ;;
    --bin-dir)      BIN_DIR=$2; shift 2 ;;
    --lib-dir)      LIB_DIR=$2; shift 2 ;;
    --etc-default)  ETC_DEFAULT=$2; shift 2 ;;
    --conf-dir)     CONF_DIR=$2; shift 2 ;;
    --) shift; break ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

for var in PREFIX BUILD_DIR DISTRO_DIR; do
  [ -n "$(eval "echo \$$var")" ] || { echo "Missing param: $var"; usage; }
done

BIN_DIR=${BIN_DIR:-$PREFIX/usr/bin}
LIB_DIR=${LIB_DIR:-$PREFIX/usr/lib/nutch}
ETC_DEFAULT=${ETC_DEFAULT:-$PREFIX/etc/default}
CONF_DIR=${CONF_DIR:-$PREFIX/etc/nutch}

RUNTIME_LOCAL="$BUILD_DIR/runtime/local"
RUNTIME_DEPLOY="$BUILD_DIR/runtime/deploy"
[ -d "$RUNTIME_LOCAL" ] || { echo "Build dir has no runtime/local: $RUNTIME_LOCAL"; exit 1; }
[ -d "$RUNTIME_DEPLOY" ] || { echo "Build dir has no runtime/deploy: $RUNTIME_DEPLOY"; exit 1; }

# Install runtime/deploy for cluster (uber jar + bin that uses hadoop jar)
install -d -m 0755 "$(dirname "$LIB_DIR")"
cp -a "$RUNTIME_DEPLOY" "$LIB_DIR"

# Conf from runtime/local (deploy may not include full conf)
install -d -m 0755 "$(dirname "$CONF_DIR")"
install -d -m 0755 "$CONF_DIR/conf.dist"
cp -a "$RUNTIME_LOCAL/conf/"* "$CONF_DIR/conf.dist/" 2>/dev/null || true

install -d -m 0755 "$(dirname "$ETC_DEFAULT")"
install -m 0644 "$DISTRO_DIR/nutch.default" "$ETC_DEFAULT/nutch"

# Wrapper script for /usr/bin/nutch
install -d -m 0755 "$BIN_DIR"
cat > "$BIN_DIR/nutch" << 'WRAPPER'
#!/bin/bash
# Nutch launcher - sources /etc/default/nutch and runs nutch from NUTCH_HOME
if [ -f /etc/default/nutch ]; then
  . /etc/default/nutch
fi
NUTCH_HOME=${NUTCH_HOME:-/usr/lib/nutch}
exec "$NUTCH_HOME/bin/nutch" "$@"
WRAPPER
chmod 755 "$BIN_DIR/nutch"
