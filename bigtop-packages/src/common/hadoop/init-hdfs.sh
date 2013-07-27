#!/bin/bash -ex
#
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

# Use this script to initialize HDFS directory structure for various components to run. This script can be run from any node in the Hadoop cluster but should only be run once by one node only. If you are planning on using oozie, we recommend that you run this script from a node that has hive, pig, sqoop, etc. installed. Unless you are using psuedo distributed cluster, this node is most likely NOT your namenode
# Steps to be performed before running this script:
# 1. Stop the namenode and datanode services if running.
# 2. Format namenode (su -s /bin/bash hdfs hdfs namenode -format).
# 3. Start the namenode and datanode services on appropriate nodes.
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /tmp'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 1777 /tmp'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /var'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /var/log'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 1775 /var/log'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown yarn:mapred /var/log'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /tmp/hadoop-yarn'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown -R mapred:mapred /tmp/hadoop-yarn'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /tmp/hadoop-yarn'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir -p /var/log/hadoop-yarn/apps'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 1777 /var/log/hadoop-yarn/apps'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown yarn:mapred /var/log/hadoop-yarn/apps'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /hbase'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown hbase:hbase /hbase'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /benchmarks'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /benchmarks'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod 755 /user'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown hdfs  /user'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/history'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown mapred:mapred /user/history'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod 755 /user/history'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/jenkins'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/jenkins'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown jenkins /user/jenkins'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/hive'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/hive'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown hive /user/hive'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/root'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/root'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown root /user/root'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/hue'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/hue'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown hue /user/hue'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/sqoop'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/sqoop'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown sqoop /user/sqoop'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chmod -R 777 /user/oozie'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -chown -R oozie /user/oozie'
# Do more setup for oozie
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share/lib'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share/lib/hive'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share/lib/mapreduce-streaming'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share/lib/distcp'
su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -mkdir /user/oozie/share/lib/pig'
# Copy over files from local filesystem to HDFS that oozie might need
if ls /usr/lib/hive/lib/*.jar &> /dev/null; then
  su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -put /usr/lib/hive/lib/*.jar /user/oozie/share/lib/hive'
fi

if ls /usr/lib/hadoop-mapreduce/hadoop-streaming*.jar &> /dev/null; then
  su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -put /usr/lib/hadoop-mapreduce/hadoop-streaming*.jar /user/oozie/share/lib/mapreduce-streaming'
fi

if ls /usr/lib/hadoop-mapreduce/hadoop-distcp*.jar &> /dev/null; then
  su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -put /usr/lib/hadoop-mapreduce/hadoop-distcp*.jar /user/oozie/share/lib/distcp'
fi

if ls /usr/lib/pig/{lib/,}*.jar &> /dev/null; then
  su -s /bin/bash hdfs -c '/usr/bin/hadoop fs -put /usr/lib/pig/{lib/,}*.jar /user/oozie/share/lib/pig'
fi

# Create home directory for the current user if it does not exist
if [ "$1" = "-u" ] ; then
  USER="$2"
  USER=${USER:-$(id -un)}
  EXIST=$(su -s /bin/bash hdfs -c "/usr/bin/hadoop fs -ls /user/${USER}" &> /dev/null; echo $?)
  if [ ! $EXIST -eq 0 ]; then
    su -s /bin/bash hdfs -c "/usr/bin/hadoop fs -mkdir /user/${USER}"
    su -s /bin/bash hdfs -c "/usr/bin/hadoop fs -chmod -R 755 /user/${USER}"
    su -s /bin/bash hdfs -c "/usr/bin/hadoop fs -chown ${USER} /user/${USER}"
  fi
fi