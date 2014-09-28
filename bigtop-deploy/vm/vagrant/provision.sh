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
#Taken from : https://cwiki.apache.org/confluence/display/BIGTOP/How+to+install+Hadoop+distribution+from+Bigtop+0.6.0
#A Vagrant recipe for setting up a hadoop box.

#Get the apache yum repo
yum install -y wget java-1.7.0-openjdk-devel.x86_64

wget -O /etc/yum.repos.d/bigtop.repo http://www.apache.org/dist/bigtop/bigtop-0.6.0/repos/fedora18/bigtop.repo

#Now install the base components
yum install -y  hadoop\* mahout\* hive\* pig\*

export JAVA_HOME=`sudo find /usr -name java-* | grep openjdk | grep 64 | grep "jvm/java" | grep -v fc`

/etc/init.d/hadoop-hdfs-namenode init

#Start each datanode
for i in hadoop-hdfs-namenode hadoop-hdfs-datanode ; 
	do service $i start ;
done

/usr/lib/hadoop/libexec/init-hdfs.sh

service hadoop-yarn-resourcemanager start
service hadoop-yarn-nodemanager start

hadoop fs -ls -R /

# Make a directory so that vagrant user has a dir to run jobs inside of. 
su -s /bin/bash hdfs -c 'hadoop fs -mkdir /user/vagrant && hadoop fs -chown vagrant:vagrant /user/vagrant'

su -s /bin/bash vagrant -c 'hadoop jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples*.jar pi 2 2'
