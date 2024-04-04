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

HCFS_USER="hdfs"
SMOKE_TESTS=$1

if [ -z "$SMOKE_TESTS" ]; then
  >&2 echo -e "\nSMOKE_TESTS VARIABLE IS NOT DEFINED. CHECK THE INPUT OF `basename $0` \n"
  exit 2
fi

# Autodetect JAVA_HOME
if [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
else
  >&2 echo -e "\nUNABLE TO DETECT JAVAHOME SINCE bigtop-utils NEEDS TO BE INSTALLED!\n"
  exit 2
fi

echo -e "\n===== EXPORTING VARIABLES =====\n"

export ALLUXIO_HOME=${ALLUXIO_HOME:-/usr/lib/alluxio}
export FLINK_HOME=${FLINK_HOME:-/usr/lib/flink}
export HADOOP_HOME=${HADOOP_HOME:-/usr/lib/hadoop}
export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-/etc/hadoop/conf}
export HADOOP_MAPRED_HOME=${HADOOP_MAPRED_HOME:-/usr/lib/hadoop-mapreduce}
export HBASE_HOME=${HBASE_HOME:-/usr/lib/hbase}
export HBASE_CONF_DIR=${HBASE_CONF_DIR:-/usr/lib/hbase/conf}
export PHOENIX_HOME=${PHOENIX_HOME:-/usr/lib/phoenix}
export HIVE_HOME=${HIVE_HOME:-/usr/lib/hive}
export HIVE_CONF_DIR=${HIVE_CONF_DIR:-/etc/hive/conf}
export HIVE_JDBC_URL=${HIVE_JDBC_URL:-jdbc:hive2://localhost:10000/default}
export KAFKA_HOME=${KAFKA_HOME:-/usr/lib/kafka}
export LIVY_HOME=${LIVY_HOME:-/usr/lib/livy}
export RANGER_URL=${RANGER_URL:-http://localhost:6080}
export SPARK_HOME=${SPARK_HOME:-/usr/lib/spark}
export TEZ_HOME=${TEZ_HOME:-/usr/lib/tez}
export WEBHDFS_URL=${WEBHDFS_URL:-$(hostname):50070/webhdfs/v1}
export ZEPPELIN_HOME=${ZEPPELIN_HOME:-/usr/lib/zeppelin}
export ZOOKEEPER_HOME=${ZOOKEEPER_HOME:-/usr/lib/zookeeper}

echo -e "\n===== START TO RUN SMOKE TESTS: $SMOKE_TESTS =====\n"

su -s /bin/bash $HCFS_USER -c "hadoop fs -mkdir -p /user/root /user/yarn"
su -s /bin/bash $HCFS_USER -c "hadoop fs -chmod 777 /user/root"
su -s /bin/bash $HCFS_USER -c "hadoop fs -chown yarn:yarn /user/yarn"

if [[ $SMOKE_TESTS == *"alluxio"* ]]; then
    su -s /bin/bash $HCFS_USER -c "hadoop fs -mkdir /underFSStorage"
    su -s /bin/bash $HCFS_USER -c "hadoop fs -chmod 777 /underFSStorage"
fi

ALL_SMOKE_TASKS=""
for s in `echo $SMOKE_TESTS | sed -e 's#,# #g'`; do
  ALL_SMOKE_TASKS="$ALL_SMOKE_TASKS bigtop-tests:smoke-tests:$s:test"
done
rm -rf /bigtop-home/.gradle
cd /bigtop-home && ./gradlew clean $ALL_SMOKE_TASKS -Psmoke.tests -Duser.dir=/bigtop-home --info
# BIGTOP-2244 workaround: clean the top level buildSrc/build with the same
# permissions as used for smoke-tests execution
rm -rf buildSrc/build/test-results/binary
rm -rf /bigtop-home/.gradle
