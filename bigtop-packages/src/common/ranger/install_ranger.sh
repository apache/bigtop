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
     --comp-dir=DIR               path to install ranger comp [/usr/lib/ranger/admin]
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


RANGER_HOME=${RANGER_HOME:-/usr/lib/ranger}
ETC_DIR=${ETC_DIR:-/etc/ranger}
ADMIN_CONF_DIR=${CONF_DIR:-${ETC_DIR}/admin/conf.dist}
USERSYNC_CONF_DIR=${CONF_DIR:-${ETC_DIR}/usersync/conf.dist}
KMS_CONF_DIR=${CONF_DIR:-${ETC_DIR}/kms/conf.dist}
TAGSYNC_CONF_DIR=${CONF_DIR:-${ETC_DIR}/tagsync/conf.dist}

if [ "${COMP_DIR}" == "" ]
then
	COMP_DIR=ranger-${COMPONENT}
fi

# Create the required directories.
install -d -m 0755 ${PREFIX}/usr/lib/$COMP_DIR

install -d -m 0755 ${PREFIX}/$ETC_DIR/{admin,usersync,kms,tagsync}

install -d -m 0755 ${PREFIX}/var/{log,run}/ranger/{admin,usersync,kms,tagsync}


# Copy artifacts to the appropriate Linux locations.
cp -r ${BUILD_DIR}/ranger-*-${COMPONENT}/* ${PREFIX}/usr/lib/${COMP_DIR}/


if [[ "${COMPONENT}" = "admin" ]]
then
cp -a ${BUILD_DIR}/ranger-*-${COMPONENT}/ews/webapp/WEB-INF/classes/conf.dist ${PREFIX}/${ADMIN_CONF_DIR}
ln -s /etc/ranger/admin/conf ${PREFIX}/usr/lib/${COMP_DIR}/conf
ln -s /usr/lib/${COMP_DIR}/conf ${PREFIX}/usr/lib/${COMP_DIR}/ews/webapp/WEB-INF/classes/conf
ln -s /usr/lib/${COMP_DIR}/ews/start-ranger-admin.sh ${PREFIX}/usr/lib/${COMP_DIR}/ews/ranger-admin-start
ln -s /usr/lib/${COMP_DIR}/ews/stop-ranger-admin.sh ${PREFIX}/usr/lib/${COMP_DIR}/ews/ranger-admin-stop
fi

if [[ "${COMPONENT}" = "usersync" ]]
then
echo "usersync"
cp -a ${BUILD_DIR}/ranger-*-${COMPONENT}/conf.dist ${PREFIX}/${USERSYNC_CONF_DIR}
ln -s /etc/ranger/usersync/conf ${PREFIX}/usr/lib/${COMP_DIR}/conf
ln -s /usr/lib/${COMP_DIR}/start.sh ${PREFIX}/usr/lib/${COMP_DIR}/ranger-usersync-start
ln -s /usr/lib/${COMP_DIR}/stop.sh ${PREFIX}/usr/lib/${COMP_DIR}/ranger-usersync-stop
fi

if [[ "${COMPONENT}" = "kms" ]]
then
echo "kms"
cp -a ${BUILD_DIR}/ranger-*-${COMPONENT}/ews/webapp/WEB-INF/classes/conf.dist ${PREFIX}/${KMS_CONF_DIR}
ln -s /etc/ranger/kms/conf ${PREFIX}//usr/lib/${COMP_DIR}/conf
ln -s /usr/lib/${COMP_DIR}/conf ${PREFIX}/usr/lib/${COMP_DIR}/ews/webapp/WEB-INF/classes/conf
fi

if [[ "${COMPONENT}" = "tagsync" ]]
then
echo "tagsync"
cp -a ${BUILD_DIR}/ranger-*-${COMPONENT}/conf.dist ${PREFIX}/${TAGSYNC_CONF_DIR}
ln -s /etc/ranger/tagsync/conf ${PREFIX}/usr/lib/${COMP_DIR}/conf
fi

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
  install -d -m 0755 ${PREFIX}/usr/lib/${RANGER_COMPONENT}/lib
  cp -r $BUILD_DIR/ranger-*-${COMPONENT}/lib/* ${PREFIX}/usr/lib/${RANGER_COMPONENT}/lib/
fi
