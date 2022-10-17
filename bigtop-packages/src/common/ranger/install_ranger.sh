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

set -e

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to ranger dist.dir
     --prefix=PREFIX             path to install into
     --component=rangerComponentName  Ranger component name [admin|hdfs-plugin|yarn-plugin|hive-plugin|hbase-plugin|kafka-plugin|atlas-plugin|...|usersync|kms|tagsync]
  Optional options:
     --comp-dir=DIR              path to install ranger comp [/usr/lib/ranger/admin]
     --var-ranger=DIR            path to install ranger contents [/var/lib/ranger]
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'build-dir:' \
  -l 'prefix:' \
  -l 'doc-dir:' \
  -l 'comp-dir:' \
  -l 'var-ranger:' \
  -l 'component:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --component)
        COMPONENT=$2 ; shift 2
        ;;
        --doc-dir)
        DOC_DIR=$2 ; shift 2
        ;;
        --comp-dir)
        COMP_DIR=$2 ; shift 2
        ;;
        --var-ranger)
        VAR_RANGER=$2 ; shift 2
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

for var in PREFIX BUILD_DIR COMPONENT ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

COMP_DIR=${COMP_DIR:-/usr/lib/ranger-${COMPONENT}}
VAR_RANGER=${VAR_RANGER:-/var/lib/ranger}
# if [ "${COMP_DIR}" == "" ]
# then
	# COMP_DIR=/usr/lib/ranger-${COMPONENT}
# fi

# Create the required directories.
install -d -m 0755 ${PREFIX}/$COMP_DIR

install -d -m 0755 ${PREFIX}/$VAR_RANGER
install -d -m 0755 ${PREFIX}/var/{log,run}/ranger

# Copy artifacts to the appropriate Linux locations.
cp -r ${BUILD_DIR}/ranger-*-${COMPONENT}/* ${PREFIX}/${COMP_DIR}/

# For other Components
if [[ "${COMPONENT}" = "hive-plugin" || "${COMPONENT}" = "hbase-plugin" || "${COMPONENT}" = "storm-plugin" || "${COMPONENT}" = "hdfs-plugin" || "${COMPONENT}" = "yarn-plugin" || "${COMPONENT}" = "kafka-plugin" || "${COMPONENT}" = "atlas-plugin" || "${COMPONENT}" = "knox-plugin" ]]
then
  RANGER_COMPONENT=${COMPONENT}
  [[ "${COMPONENT}" = "hdfs-plugin" ]] && RANGER_COMPONENT="hadoop"
  [[ "${COMPONENT}" = "yarn-plugin" ]] && RANGER_COMPONENT="hadoop"
  [[ "${COMPONENT}" = "storm-plugin" ]] && RANGER_COMPONENT="storm"
  [[ "${COMPONENT}" = "hbase-plugin" ]] && RANGER_COMPONENT="hbase"
  [[ "${COMPONENT}" = "hive-plugin" ]] && RANGER_COMPONENT="hive"
  [[ "${COMPONENT}" = "kafka-plugin" ]] && RANGER_COMPONENT="kafka"
  [[ "${COMPONENT}" = "atlas-plugin" ]] && RANGER_COMPONENT="atlas"
  [[ "${COMPONENT}" = "knox-plugin" ]] && RANGER_COMPONENT="knox"
  RANGER_COMPONENT_DIR=${COMP_DIR}/../${RANGER_COMPONENT}
  install -d -m 0755 ${PREFIX}/${RANGER_COMPONENT_DIR}/lib
  cp -r $BUILD_DIR/ranger-*-${COMPONENT}/lib/* ${PREFIX}/${RANGER_COMPONENT_DIR}/lib/
fi
