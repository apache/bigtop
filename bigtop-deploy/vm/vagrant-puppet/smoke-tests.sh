#!/bin/bash
HCFS_USER="hdfs"
gradle="/usr/local/gradle/bin/gradle"

export HADOOP_CONF_DIR=/etc/hadoop/conf/
export BIGTOP_HOME=/bigtop-home/
export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce/
export HIVE_HOME=/usr/lib/hive/
export PIG_HOME=/usr/lib/pig/
export FLUME_HOME=/usr/lib/flume/
export HIVE_CONF_DIR=/etc/hive/conf/
export JAVA_HOME="/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.65.x86_64/"
export MAHOUT_HOME="/usr/lib/mahout"
export ITEST="0.7.0"

su -s /bin/bash $HCFS_USER -c '/usr/bin/hadoop fs -mkdir /user/vagrant /user/root'
su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/vagrant'
su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/root'

yum install -y pig hive flume mahout
cd /bigtop-home/bigtop-tests/smoke-tests && $gradle compileGroovy test -Dsmoke.tests=pig --info
