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
     --distro-dir=DIR            path to distro specific files (debian/RPM)
     --build-dir=DIR             path to build directory
     --prefix=PREFIX             path to install into
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'distro-dir:' \
  -l 'build-dir:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"

while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
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

install -d -p -m 755 ${PREFIX}/etc/default
install -d -p -m 755 ${PREFIX}/usr/lib/airflow
install -d -p -m 755 ${PREFIX}/usr/lib/systemd/system
install -d -p -m 755 ${PREFIX}/usr/lib/tmpfiles.d

find ${BUILD_DIR}/bin -type f -exec sed -i -e "s,$(cd ${BUILD_DIR} && pwd),/usr/lib/airflow," {} \;
find ${BUILD_DIR}/lib -type f -exec sed -i -e "s,/usr/bin/env python$,/usr/bin/env python3," {} \;

cp -r ${BUILD_DIR}/{bin,include,lib,pyvenv.cfg} ${PREFIX}/usr/lib/airflow
cp ${DISTRO_DIR}/airflow.default ${PREFIX}/etc/default/airflow
cp ${DISTRO_DIR}/airflow-{scheduler,webserver}.service ${PREFIX}/usr/lib/systemd/system
cp ${DISTRO_DIR}/airflow.conf ${PREFIX}/usr/lib/tmpfiles.d
