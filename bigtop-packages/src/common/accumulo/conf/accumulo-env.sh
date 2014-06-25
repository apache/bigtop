#! /usr/bin/env bash

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

###
### Configure these environment variables to point to your local installations.
###
### The functional tests require conditional values, so keep this style:
###
### test -z "$JAVA_HOME" && export JAVA_HOME=/usr/local/lib/jdk-1.6.0
###
###
### Note that the -Xmx -Xms settings below require substantial free memory: 
### you may want to use smaller values, especially when running everything
### on a single machine.
###

export HADOOP_PREFIX=${HADOOP_PREFIX:-/usr/lib/hadoop}
export HADOOP_HOME=${HADOOP_HOME:-HADOOP_PREFIX}

export HADOOP_CLIENT_HOME=/usr/lib/hadoop/client
export HADOOP_CONF_DIR="$HADOOP_PREFIX/etc/hadoop"

export ZOOKEEPER_HOME=/usr/lib/zookeeper
export ACCUMULO_LOG_DIR=/var/log/accumulo
if [ -f ${ACCUMULO_CONF_DIR}/accumulo.policy ]
then
   POLICY="-Djava.security.manager -Djava.security.policy=${ACCUMULO_CONF_DIR}/accumulo.policy"
fi

# Should the monitor bind to all network interfaces -- default: false
# export ACCUMULO_MONITOR_BIND_ALL="true"
