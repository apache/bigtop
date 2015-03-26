#!/bin/bash
HCFS_USER="hdfs"
SMOKE_TESTS=${1:-mapreduce,pig}

export HADOOP_CONF_DIR=/etc/hadoop/conf/
export BIGTOP_HOME=/bigtop-home/
export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce/
export HIVE_HOME=/usr/lib/hive/
export PIG_HOME=/usr/lib/pig/
export FLUME_HOME=/usr/lib/flume/
export SQOOP_HOME=/usr/lib/sqoop/
export HIVE_CONF_DIR=/etc/hive/conf/
export JAVA_HOME="/usr/lib/jvm/java-openjdk/"
export MAHOUT_HOME="/usr/lib/mahout"
export ITEST="0.7.0"

su -s /bin/bash $HCFS_USER -c '/usr/bin/hadoop fs -mkdir /user/vagrant /user/root'
su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/vagrant'
su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/root'

if [ -f /etc/debian_version ] ; then
    apt-get -y install pig hive flume mahout sqoop
else
    yum install -y pig hive flume mahout sqoop
fi
cd /bigtop-home/bigtop-tests/smoke-tests && ./gradlew clean compileGroovy test -Dsmoke.tests=$SMOKE_TESTS --info
