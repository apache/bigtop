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

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

LIB_DIR=${LIB_DIR:-/usr/lib}

SQOOP_HOME=${LIB_DIR}/sqoop
TOMCAT_HOME=${LIB_DIR}/bigtop-tomcat

export CATALINA_BIN=${CATALINA_BIN:-${TOMCAT_HOME}/bin}
export CATALINA_BASE=${CATALINA_BASE:-${SQOOP_HOME}/sqoop-server}
export CATALINA_OPTS=${CATALINA_OPTS:--Xmx1024m}
export CATALINA_OUT=${CATALINE_OUT:-/var/log/sqoop/sqoop-tomcat.log}

env CLASSPATH=$CLASSPATH $SQOOP_HOME/bin/sqoop.sh server $@

