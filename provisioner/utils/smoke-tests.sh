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
SMOKE_TESTS=${1:-mapreduce,pig}

# Autodetect JAVA_HOME
if [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
else
  >&2 echo -e "\nUNABLE TO DETECT JAVAHOME SINCE bigtop-utils NEEDS TO BE INSTALLED!\n"
  exit 2
fi

echo -e "\n===== START TO RUN SMOKE TESTS: $SMOKE_TESTS =====\n"

export HADOOP_CONF_DIR=/etc/hadoop/conf/
export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce/
export HIVE_HOME=/usr/lib/hive/
export PIG_HOME=/usr/lib/pig/
export FLUME_HOME=/usr/lib/flume/
export SQOOP_HOME=/usr/lib/sqoop/
export HIVE_CONF_DIR=/etc/hive/conf/
export MAHOUT_HOME="/usr/lib/mahout"

prep() {
    HADOOP_COMMAND=$1
    su -s /bin/bash $HCFS_USER -c "JAVA_LIBRARY_PATH=/usr/lib/qfs $HADOOP_COMMAND fs -mkdir /user/vagrant /user/root"
    su -s /bin/bash $HCFS_USER -c "JAVA_LIBRARY_PATH=/usr/lib/qfs $HADOOP_COMMAND fs -chmod 777 /user/vagrant"
    su -s /bin/bash $HCFS_USER -c "JAVA_LIBRARY_PATH=/usr/lib/qfs $HADOOP_COMMAND fs -chmod 777 /user/root"
}

prep hadoop
if [[ $SMOKE_TESTS == *"qfs"* ]]; then
    prep hadoop-qfs
fi

if [ -f /etc/debian_version ] ; then
    apt-get -y install pig hive flume mahout sqoop
else
    yum install -y pig hive flume mahout sqoop
fi
ALL_SMOKE_TASKS=""
for s in `echo $SMOKE_TESTS | sed -e 's#,# #g'`; do
  ALL_SMOKE_TASKS="$ALL_SMOKE_TASKS bigtop-tests:smoke-tests:$s:test"
done
cd /bigtop-home && ./gradlew clean $ALL_SMOKE_TASKS -Psmoke.tests --info
# BIGTOP-2244 workaround: clean the top level buildSrc/build with the same
# permissions as used for smoke-tests execution
rm -rf buildSrc/build/test-results/binary

