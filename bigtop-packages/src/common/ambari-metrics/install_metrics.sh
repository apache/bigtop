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

set -ex

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --build-dir=DIR             path to ambari dist.dir
     --prefix=PREFIX             path to install into
     --source-dir=DIR            path to the source code
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'prefix:' \
  -l 'source-dir:' \
  -l 'distro-dir:' \
  -l 'build-dir:' -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --prefix)
        PREFIX=$2 ; shift 2
        ;;
        --build-dir)
        BUILD_DIR=$2 ; shift 2
        ;;
        --source-dir)
        SOURCE_DIR=$2 ; shift 2
        ;;
        --distro-dir)
        DISTRO_DIR=$2 ; shift 2
        ;;
        --)
        shift ; break
        ;;
        *)
        echo "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
done

for var in PREFIX BUILD_DIR SOURCE_DIR ; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

project_version=3.1.0-SNAPSHOT

embedded_dir=$BUILD_DIR/embedded
hadoop_folder=$embedded_dir/hadoop_ambari_metrics
hbase_folder=$embedded_dir/hbase-${HBASE_VERSION}
grafana_folder=$embedded_dir/grafana-9.3.2
hadoop_version=3.3.5

collector_dir=$BUILD_DIR/ambari-metrics-timelineservice
assembly_dir=$BUILD_DIR/ambari-metrics-assembly/target

resmonitor_install_dir=${PREFIX}/usr/lib/python3.9/site-packages/resource_monitoring
monitor_dir=$BUILD_DIR/ambari-metrics-host-monitoring
aggregator_dir=$BUILD_DIR/ambari-metrics-host-aggregator
grafana_dir=$BUILD_DIR/ambari-metrics-grafana

hadoop_sink_dir=$BUILD_DIR/ambari-metrics-hadoop-sink
storm_sink_dir=$BUILD_DIR/ambari-metrics-storm-sink
flume_sink_dir=$BUILD_DIR/ambari-metrics-flume-sink
kafka_sink_dir=$BUILD_DIR/ambari-metrics-kafka-sink


ams_hbase_install_dir=${PREFIX}/usr/lib/ams-hbase/
install -d -m 0755 ${PREFIX}
install -d -m 0755 ${PREFIX}/usr/sbin/

install -d -m 0755 ${ams_hbase_install_dir}
install -d -m 0755 ${ams_hbase_install_dir}/lib/hadoop-native/
install -d -m 0755 ${PREFIX}/etc/ams-hbase/conf
install -d -m 0755 ${PREFIX}/var/run/ams-hbase

ambari_metrics_collector_install_dir=${PREFIX}/usr/lib/ambari-metrics-collector/
install -d -m 0755 ${ambari_metrics_collector_install_dir}
install -d -m 0755 ${ambari_metrics_collector_install_dir}/bin
install -d -m 0755 ${PREFIX}/etc/ambari-metrics-collector/conf
install -d -m 0755 ${PREFIX}/var/lib/ambari-metrics-collector
install -d -m 0755 ${PREFIX}/usr/lib/ambari-metrics-hadoop-sink
install -d -m 0755 ${PREFIX}/usr/lib/flume/lib
install -d -m 0755 ${PREFIX}/usr/lib/storm/lib
install -d -m 0755 ${PREFIX}/usr/lib/ambari-metrics-kafka-sink
install -d -m 0755 ${PREFIX}/usr/lib/ambari-metrics-kafka-sink/lib

install -d -m 0755 ${PREFIX}/usr/lib/ambari-metrics-grafana/
install -d -m 0755 ${PREFIX}/usr/lib/ambari-metrics-grafana/bin
install -d -m 0755 ${PREFIX}/etc/ambari-metrics-grafana/conf
install -d -m 0755 ${PREFIX}/var/lib/ambari-metrics-grafana
install -d -m 0755 ${PREFIX}/var/lib/ambari-metrics-grafana/plugins
install -d -m 0755 ${PREFIX}/var/lib/ambari-metrics-grafana/plugins/ambari-metrics
install -d -m 0755 ${PREFIX}/var/run/ambari-metrics-grafana
install -d -m 0755 ${PREFIX}/var/log/ambari-metrics-grafana

install -d -m 0755 $resmonitor_install_dir
install -d -m 0755 $resmonitor_install_dir/core
install -d -m 0755 $resmonitor_install_dir/psutil
install -d -m 0755 ${PREFIX}/var/lib/ambari-metrics-monitor/lib
install -d -m 0755 ${PREFIX}/etc/ambari-metrics-monitor/conf

############
############ ambari-metrics-collector
############
# First mapping

cp -r $collector_dir/target/lib/*.jar ${PREFIX}/usr/lib/ambari-metrics-collector/
rm -f ${PREFIX}/usr/lib/ambari-metrics-collector/{*tests,findbugs*,jdk.tools*,hadoop*,hbase*}.jar
rm -f ${PREFIX}/usr/lib/ambari-metrics-collector/jakarta.ws.rs-api-*.jar
cp $collector_dir/target/ambari-metrics-timelineservice-*.jar ${PREFIX}/usr/lib/ambari-metrics-collector/
cp $hbase_folder/lib/{hadoop*,hbase*}.jar ${PREFIX}/usr/lib/ambari-metrics-collector/

cp -r $hbase_folder/* ${PREFIX}/usr/lib/ams-hbase/
rm -rf ${PREFIX}/usr/lib/ams-hbase/lib/{*tests,findbugs*,jdk.tools*,hadoop*,guava*,commons-beanutils*}.jar
cp -r $hbase_folder/bin/* ${PREFIX}/usr/lib/ams-hbase/bin/
rm -rf ${PREFIX}/usr/lib/ams-hbase/docs/*


# Copy files from 'share/hadoop/common'
cp $hadoop_folder/share/hadoop/common/hadoop-{common,registry}-$hadoop_version.jar ${PREFIX}/usr/lib/ams-hbase/lib/
# Copy files from 'share/hadoop/hdfs'
cp $hadoop_folder/share/hadoop/hdfs/hadoop-{hdfs,hdfs-client}-$hadoop_version.jar ${PREFIX}/usr/lib/ams-hbase/lib/
# Copy files from 'share/hadoop/common/lib'
cp $hadoop_folder/share/hadoop/common/lib/{commons-beanutils-*,hadoop-shaded-*,hadoop-auth-$hadoop_version,hadoop-annotations-$hadoop_version}.jar ${PREFIX}/usr/lib/ams-hbase/lib/
# Copy files from 'share/hadoop/tools/lib'
cp $hadoop_folder/share/hadoop/tools/lib/hadoop-{aws,distcp,client}-$hadoop_version.jar ${PREFIX}/usr/lib/ams-hbase/lib/
# Copy files from 'share/hadoop/yarn'
cp $hadoop_folder/share/hadoop/yarn/hadoop-yarn-{api,client,common,registry,server-applicationhistoryservice,server-common,server-nodemanager,server-resourcemanager,server-web-proxy}-$hadoop_version.jar ${PREFIX}/usr/lib/ams-hbase/lib/
# Copy files from 'share/hadoop/mapreduce'
cp $hadoop_folder/share/hadoop/mapreduce/hadoop-mapreduce-client-{app,common,core,hs,jobclient,shuffle}-$hadoop_version.jar ${PREFIX}/usr/lib/ams-hbase/lib/


# Fifth mapping
cp $collector_dir/target/lib/{phoenix,antlr,re2j,guava-*,stax2-api,woodstox-core,commons-configuration2-}*.jar ${PREFIX}/usr/lib/ams-hbase/lib/

#Sixth mapping
cp -r $hadoop_folder/lib/native/* ${PREFIX}/usr/lib/ams-hbase/lib/hadoop-native/

#seven
cp $collector_dir/conf/unix/ambari-metrics-collector ${PREFIX}/usr/sbin/


#eight
cp $collector_dir/conf/unix/sqlline/* ${PREFIX}/usr/lib/ambari-metrics-collector/bin/


#nine
cp $collector_dir/conf/unix/ams-env.sh ${PREFIX}/etc/ambari-metrics-collector/conf/
cp $collector_dir/conf/unix/ams-site.xml ${PREFIX}/etc/ambari-metrics-collector/conf/
cp $collector_dir/conf/unix/log4j.properties ${PREFIX}/etc/ambari-metrics-collector/conf/
cp $collector_dir/conf/unix/metrics_whitelist ${PREFIX}/etc/ambari-metrics-collector/conf/
cp $collector_dir/conf/unix/amshbase_metrics_whitelist ${PREFIX}/etc/ambari-metrics-collector/conf/
cp $collector_dir/conf/unix/hbase-site.xml ${PREFIX}/etc/ambari-metrics-collector/conf/

# Copy all files from location to directory
cp -r $hbase_folder/conf/* ${PREFIX}/etc/ams-hbase/conf/


############
############ ambari-metrics-hadoop-sink
############
# Create directories
# Copy jars
cp $hadoop_sink_dir/target/ambari-metrics-hadoop-sink-with-common-*.jar ${PREFIX}/usr/lib/ambari-metrics-hadoop-sink/
cp $flume_sink_dir/target/ambari-metrics-flume-sink-with-common-*.jar ${PREFIX}/usr/lib/flume/lib/
cp $storm_sink_dir/target/ambari-metrics-storm-sink-with-common-*.jar ${PREFIX}/usr/lib/storm/lib/
cp $kafka_sink_dir/target/ambari-metrics-kafka-sink-with-common-*.jar ${PREFIX}/usr/lib/ambari-metrics-kafka-sink/
cp -r $kafka_sink_dir/target/lib/* ${PREFIX}/usr/lib/ambari-metrics-kafka-sink/lib/


############
############ ambari-metrics-grafana
############
# Copy files and directories
cp -r $grafana_folder/* ${PREFIX}/usr/lib/ambari-metrics-grafana/
cp -r $grafana_folder/bin/* ${PREFIX}/usr/lib/ambari-metrics-grafana/bin
cp $grafana_dir/conf/unix/ambari-metrics-grafana ${PREFIX}/usr/sbin/
cp $grafana_dir/conf/unix/ams-grafana-env.sh ${PREFIX}/etc/ambari-metrics-grafana/conf/
cp $grafana_dir/conf/unix/ams-grafana.ini ${PREFIX}/etc/ambari-metrics-grafana/conf/
cp -r $grafana_dir/ambari-metrics/* ${PREFIX}/var/lib/ambari-metrics-grafana/plugins/ambari-metrics/

############
############ ambari-metrics-monitor
############

# Copy files and directories
cp $monitor_dir/src/main/python/__init__.py $resmonitor_install_dir/
cp $monitor_dir/src/main/python/main.py $resmonitor_install_dir/
cp -r $monitor_dir/src/main/python/core/* $resmonitor_install_dir/core/
cp -r $monitor_dir/src/main/python/psutil/* $resmonitor_install_dir/psutil/
cp $aggregator_dir/target/ambari-metrics-host-aggregator-*.jar ${PREFIX}/var/lib/ambari-metrics-monitor/lib/
cp $monitor_dir/conf/unix/ambari-metrics-monitor ${PREFIX}/usr/sbin/

# Exclude specified files in psutil directory
rm -rf $resmonitor_install_dir/psutil/build
