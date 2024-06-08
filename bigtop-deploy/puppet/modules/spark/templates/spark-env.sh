#!/usr/bin/env bash
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

export SPARK_HOME=${SPARK_HOME:-/usr/lib/spark}
export SPARK_LOG_DIR=${SPARK_LOG_DIR:-/var/log/spark}
export SPARK_DAEMON_JAVA_OPTS=<%= @spark_daemon_java_opts %>
export HADOOP_HOME=${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export HIVE_CONF_DIR=${HIVE_CONF_DIR:-/etc/hive/conf}

export STANDALONE_SPARK_MASTER_HOST=<%= @master_host %>
export SPARK_MASTER_PORT=<%= @master_port %>
export SPARK_MASTER_IP=$STANDALONE_SPARK_MASTER_HOST
<% if @master_url -%>
export SPARK_MASTER_URL=<%= @master_url %>
<% else -%>
<% if (@roles & ['spark-master', 'spark-worker']) != [] -%>
export SPARK_MASTER_URL=spark://<%= @master_host %>:<%= @master_port %>
<% else -%>
export SPARK_MASTER_URL=yarn
<% end -%>
<% end -%>
export SPARK_MASTER_WEBUI_PORT=<%= @master_ui_port %>

export SPARK_WORKER_DIR=${SPARK_WORKER_DIR:-/var/run/spark/work}
export SPARK_WORKER_PORT=<%= @worker_port %>
export SPARK_WORKER_WEBUI_PORT=<%= @worker_ui_port %>

export SPARK_DIST_CLASSPATH=$(hadoop classpath)
