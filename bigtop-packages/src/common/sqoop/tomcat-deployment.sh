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

# This script must be sourced so that it can set CATALINA_BASE for the parent process

TOMCAT_CONF=${TOMCAT_CONF:-`readlink -e /etc/sqoop/tomcat-conf`}
TOMCAT_DEPLOYMENT=${TOMCAT_DEPLOYMENT:-/var/lib/sqoop/tomcat-deployment}
SQOOP_HOME=${SQOOP_HOME:-/usr/lib/sqoop}

rm -rf ${TOMCAT_DEPLOYMENT}
mkdir ${TOMCAT_DEPLOYMENT}
cp -r ${TOMCAT_CONF}/conf ${TOMCAT_DEPLOYMENT}
cp -r ${SQOOP_HOME}/webapps ${TOMCAT_DEPLOYMENT}/webapps
cp -r ${TOMCAT_CONF}/WEB-INF/* ${TOMCAT_DEPLOYMENT}/webapps/sqoop/WEB-INF/
cp -r ${SQOOP_HOME}/bin ${TOMCAT_DEPLOYMENT}/

export CATALINA_BASE=${TOMCAT_DEPLOYMENT}

if [ -n "${BIGTOP_CLASSPATH}" ] ; then
  sed -i -e "s#^\(common.loader=.*\)\$#\1,${BIGTOP_CLASSPATH/:/,}#" ${TOMCAT_DEPLOYMENT}/conf/catalina.properties
fi

chown -R sqoop:sqoop ${TOMCAT_DEPLOYMENT}

export CATALINA_BASE=${TOMCAT_DEPLOYMENT}

