#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.format import format

def hbase_service(
  name,
  action = 'start'): # 'start' or 'stop' or 'status'
    
    import params
  
    role = name
    cmd = format("{daemon_script} --config {hbase_conf_dir}")
    pid_file = format("{hbase_pid_dir}/hbase-{hbase_user}-{role}.pid")
    no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")

    if action == 'start':

      daemon_cmd = format("{cmd} start {role}")
      
      Execute ( daemon_cmd,
        not_if = no_op_test,
        user = params.hbase_user
      )
    elif action == 'stop':
      daemon_cmd = format("{cmd} stop {role}")

      Execute ( daemon_cmd,
        user = params.hbase_user,
        # BUGFIX: hbase regionserver sometimes hangs when nn is in safemode
        timeout = params.hbase_regionserver_shutdown_timeout,
        on_timeout = format("{no_op_test} && {sudo} -H -E kill -9 `{sudo} cat {pid_file}`")
      )
      
      File(pid_file,
        action = "delete",
      )
    elif action == 'metricsFIX':
      Execute(format("{sudo} cp /usr/lib/ambari-server/htrace-core-3*.jar /usr/lib/ams-hbase/lib/client-facing-thirdparty/")
      )
      Execute(format("{sudo} rm -f /usr/lib/ambari-metrics-collector/phoenix-core-*.jar")
      )
      Execute(format("{sudo} rm -f /usr/lib/ambari-metrics-collector/protobuf-java-3*.jar")
      )
      Execute(format("{sudo} rm -f /usr/lib/ambari-metrics-collector/jakarta.ws.rs-api*.jar")
      )
      Execute(format("{sudo} rm -f /usr/lib/ambari-metrics-collector/servlet*.jar")
      )
      Execute(format("{sudo} wget http://repo.bigtop.apache.org.s3.amazonaws.com/bigtop-stack-binary/3.2.0/centos-7/x86_64/phoenix-hbase-compat-2.4.1-5.1.2.jar -P /usr/lib/ambari-metrics-collector")
      )
      Execute(format("{sudo} wget http://repo.bigtop.apache.org.s3.amazonaws.com/bigtop-stack-binary/3.2.0/centos-7/x86_64/phoenix-core-5.1.2.jar -P /usr/lib/ambari-metrics-collector")
      )
      Execute(format("{sudo} wget https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/2.6.1/protobuf-java-2.6.1.jar -P /usr/lib/ambari-metrics-collector")
      )
      Execute(format("{sudo} cp /usr/lib/ambari-metrics-collector/phoenix-*.jar /usr/lib/ambari-metrics-collector/omid*.jar /usr/lib/ambari-metrics-collector/joda-time-*.jar /usr/lib/ams-hbase/lib/client-facing-thirdparty/")
      )
      Execute(format("{sudo} cp /usr/lib/ams-hbase/lib/client-facing-thirdparty/*.jar /usr/lib/ams-hbase/lib")
      )
      Execute(format("{sudo} cp /usr/lib/ams-hbase/lib/hbase-*.jar /usr/lib/ams-hbase/lib/hadoop-*.jar /usr/lib/ambari-metrics-collector")
      )
