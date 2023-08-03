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
  -l 'etc-ranger:' \
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
        --etc-ranger)
        ETC_RANGER=$2 ; shift 2
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

for var in PREFIX BUILD_DIR COMPONENT; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

COMP_DIR=${COMP_DIR:-/usr/lib/ranger-${COMPONENT}}
VAR_RANGER=${VAR_RANGER:-/var/lib/ranger}
ETC_RANGER=${ETC_RANGER:-/etc/ranger}
NP_ETC_RANGER=/etc/ranger
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

# Config
if [[ "${COMPONENT}" =~ ^(admin|usersync|tagsync|kms)$ ]]; then
  install -d -m 0755 ${PREFIX}/${NP_ETC_RANGER}/${COMPONENT}
  install -d -m 0755 ${PREFIX}/${ETC_RANGER}/${COMPONENT}/conf.dist

  if [[ "${COMPONENT}" = "admin" || "${COMPONENT}" = "kms" ]]; then
    cp -a ${PREFIX}/${COMP_DIR}/ews/webapp/WEB-INF/classes/conf.dist/* ${PREFIX}/${ETC_RANGER}/${COMPONENT}/conf.dist
    ln -s ${NP_ETC_RANGER}/${COMPONENT}/conf ${PREFIX}/${COMP_DIR}/conf
    ln -s ${NP_ETC_RANGER}/${COMPONENT}/conf ${PREFIX}/$COMP_DIR/ews/webapp/WEB-INF/classes/conf
  else
    cp -a ${PREFIX}/${COMP_DIR}/conf.dist/* ${PREFIX}/${ETC_RANGER}/${COMPONENT}/conf.dist
    ln -s ${NP_ETC_RANGER}/${COMPONENT}/conf ${PREFIX}/${COMP_DIR}/conf
  fi
else
  RANGER_COMPONENT=${COMPONENT}
  if [[ "${COMPONENT}" = "hdfs-plugin" || "${COMPONENT}" = "yarn-plugin" ]];then
    RANGER_COMPONENT="hadoop"
  else
    RANGER_COMPONENT=$(echo $COMPONENT | cut -d '-' -f 1)
  fi
  RANGER_COMPONENT_DIR=${COMP_DIR}/../${RANGER_COMPONENT}
  COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/lib/

  if [ "${RANGER_COMPONENT}" = "knox" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/ext
  elif [ "${RANGER_COMPONENT}" = "solr" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/server/solr-webapp/webapp/WEB-INF/lib/
  elif [ "${RANGER_COMPONENT}" = "kafka" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/libs
  elif [ "${RANGER_COMPONENT}" = "storm" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/extlib-daemon
  elif [ "${RANGER_COMPONENT}" = "atlas" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/libext
  elif [ "${RANGER_COMPONENT}" = "sqoop" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/server/lib
  elif [ "${RANGER_COMPONENT}" = "kylin" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/tomcat/webapps/kylin/WEB-INF/lib
  elif [ "${RANGER_COMPONENT}" = "elasticsearch" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/plugins
  elif [ "${RANGER_COMPONENT}" = "trino" ]; then
    COMPONENT_LIB_DIR=${PREFIX}/${RANGER_COMPONENT_DIR}/plugin/ranger
    if [ ! -d "${COMPONENT_LIB_DIR}" ]; then
      echo "INFO: Creating ${COMPONENT_LIB_DIR}"
      mkdir -p ${COMPONENT_LIB_DIR}
    fi
  fi

  install -d -m 0755 ${COMPONENT_LIB_DIR}
  cp -r $BUILD_DIR/ranger-*-${COMPONENT}/lib/* ${COMPONENT_LIB_DIR}
fi
