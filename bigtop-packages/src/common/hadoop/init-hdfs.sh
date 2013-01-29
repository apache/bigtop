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
# 2. Format namenode (sudo -u hdfs hdfs namenode -format).
# 3. Start the namenode and datanode services on appropriate nodes.
sudo -u hdfs hadoop fs -mkdir /tmp
sudo -u hdfs hadoop fs -chmod -R 1777 /tmp
sudo -u hdfs hadoop fs -mkdir /var
sudo -u hdfs hadoop fs -mkdir /var/log
sudo -u hdfs hadoop fs -chmod -R 1775 /var/log
sudo -u hdfs hadoop fs -chown yarn:mapred /var/log
sudo -u hdfs hadoop fs -mkdir /hbase
sudo -u hdfs hadoop fs -chown hbase /hbase
sudo -u hdfs hadoop fs -mkdir /benchmarks
sudo -u hdfs hadoop fs -chmod -R 777 /benchmarks
sudo -u hdfs hadoop fs -mkdir /user
sudo -u hdfs hadoop fs -mkdir /user/history
sudo -u hdfs hadoop fs -chown mapred /user/history
sudo -u hdfs hadoop fs -mkdir /user/jenkins
sudo -u hdfs hadoop fs -chmod -R 777 /user/jenkins
sudo -u hdfs hadoop fs -chown jenkins /user/jenkins
sudo -u hdfs hadoop fs -mkdir /user/hive
sudo -u hdfs hadoop fs -chmod -R 777 /user/hive
sudo -u hdfs hadoop fs -chown hive /user/hive
sudo -u hdfs hadoop fs -mkdir /user/root
sudo -u hdfs hadoop fs -chmod -R 777 /user/root
sudo -u hdfs hadoop fs -chown root /user/root
sudo -u hdfs hadoop fs -mkdir /user/hue
sudo -u hdfs hadoop fs -chmod -R 777 /user/hue
sudo -u hdfs hadoop fs -chown hue /user/hue
sudo -u hdfs hadoop fs -mkdir /user/oozie
# Do more setup for oozie
sudo -u hdfs hadoop fs -mkdir /user/oozie/share
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib/hive
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib/mapreduce-streaming
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib/distcp
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib/pig
sudo -u hdfs hadoop fs -mkdir /user/oozie/share/lib/sqoop
# Copy over files from local filesystem to HDFS that oozie might need
if ls /usr/lib/hive/lib/*.jar &> /dev/null; then
  sudo -u hdfs hadoop fs -put /usr/lib/hive/lib/*.jar /user/oozie/share/lib/hive
fi

if ls /usr/lib/hadoop-mapreduce/hadoop-streaming*.jar &> /dev/null; then
  sudo -u hdfs hadoop fs -put /usr/lib/hadoop-mapreduce/hadoop-streaming*.jar /user/oozie/share/lib/mapreduce-streaming
fi

if ls /usr/lib/hadoop-mapreduce/hadoop-distcp*.jar &> /dev/null; then
  sudo -u hdfs hadoop fs -put /usr/lib/hadoop-mapreduce/hadoop-distcp*.jar /user/oozie/share/lib/distcp
fi

if ls /usr/lib/pig/{lib/,}*.jar &> /dev/null; then
  sudo -u hdfs hadoop fs -put /usr/lib/pig/{lib/,}*.jar /user/oozie/share/lib/pig
fi

if ls /usr/lib/sqoop/{lib/,}*.jar &> /dev/null; then
  sudo -u hdfs hadoop fs -put /usr/lib/sqoop/{lib/,}*.jar /user/share/lib/sqoop
fi

sudo -u hdfs hadoop fs -chmod -R 777 /user/oozie
sudo -u hdfs hadoop fs -chown -R oozie /user/oozie
