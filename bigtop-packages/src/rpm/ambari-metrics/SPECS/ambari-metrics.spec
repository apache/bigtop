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


%global _python_bytecompile_extra 0
%define debug_package %{nil}
%undefine _missing_build_ids_terminate_build
%define ambari_metrics_name ambari-metrics
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

# disable repacking jars
%define __os_install_post %{nil}
%define __jar_repack %{nil}

Name: ambari-metrics
Version: %{ambari_metrics_version}
Release: %{ambari_metrics_release}
Summary: Ambari Metrics
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/apache-%{ambari_metrics_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: apache-%{ambari_metrics_name}-%{ambari_metrics_base_version}.tar.gz
Source1: do-component-build 
Source2: install_metrics.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n apache-%{ambari_metrics_name}-%{ambari_metrics_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
AMBARI_VERSION=%{ambari_version} bash $RPM_SOURCE_DIR/install_metrics.sh \
          --build-dir=`pwd` \
          --distro-dir=$RPM_SOURCE_DIR \
          --source-dir=`pwd` \
          --prefix=$RPM_BUILD_ROOT

%package collector
Summary: Ambari Metrics Collector
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description collector
Ambari Metrics Collector

%post collector
/bin/chmod 755 /usr/sbin/ambari-metrics-collector
/bin/chmod -R 755 /usr/lib/ambari-metrics-collector/bin
/bin/chmod -R 755 /usr/lib/ams-hbase/bin
/bin/chmod -R 755 /usr/lib/ams-hbase/lib/hadoop-native
/bin/chmod 644 /etc/ambari-metrics-collector/conf/*.xml
/bin/chmod 755 /etc/ambari-metrics-collector/conf/*.sh
/bin/chmod 755 /etc/ambari-metrics-collector/conf/amshbase_metrics_whitelist


%package grafana
Summary: Ambari Metrics Grafana
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description grafana
Ambari Metrics Grafana

%post grafana
/bin/chmod  755 /usr/sbin/ambari-metrics-grafana
/bin/chmod -R 755 /etc/ambari-metrics-grafana/conf
/bin/chmod -R 755 /var/lib/ambari-metrics-grafana/plugins/ambari-metrics
/bin/chmod -R 755 /usr/lib/ambari-metrics-grafana/bin


%package hadoop-sink
Summary: Ambari Metrics Hadoop Sink
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description hadoop-sink
Ambari Metrics Hadoop Sink

%pre hadoop-sink
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
# limitations under the License

JAR_FILES_LEGACY_FOLDER="/usr/lib/ambari-metrics-sink-legacy"

HADOOP_SINK_LINK="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar"

HADOOP_LEGACY_LINK_NAME="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink-legacy.jar"

if [ -f ${HADOOP_SINK_LINK} ]
then
    old_jar=$(readlink -f ${HADOOP_SINK_LINK})
    version_part=$(basename ${old_jar} | awk -F"-" '{print $7}')
    IFS=. version=(${version_part})
    unset IFS

    if [[ ${version[0]} -le 2 && ${version[1]} -lt 7 ]] # backup only required on upgrade from version < 2.7
    then
        if [ ! -d "$JAR_FILES_LEGACY_FOLDER" ]
        then
            mkdir -p "$JAR_FILES_LEGACY_FOLDER"
        fi
        echo "Backing up Ambari metrics hadoop sink jar ${old_jar} -> $JAR_FILES_LEGACY_FOLDER/"
        cp "${old_jar}" "${JAR_FILES_LEGACY_FOLDER}/"

        HADOOP_SINK_LEGACY_JAR="$JAR_FILES_LEGACY_FOLDER/$(basename ${old_jar})"
        echo "Creating symlink for backup jar $HADOOP_LEGACY_LINK_NAME -> $HADOOP_SINK_LEGACY_JAR"
        rm -f "${HADOOP_LEGACY_LINK_NAME}" ; ln -s "${HADOOP_SINK_LEGACY_JAR}" "${HADOOP_LEGACY_LINK_NAME}"
    fi
fi

exit 0

%post hadoop-sink
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
# limitations under the License

project_version=3.1.0-SNAPSHOT

/bin/chmod -R 755 /usr/lib/ambari-metrics-kafka-sink/lib
/bin/chmod -R 755 /usr/lib/storm/lib
/bin/chmod -R 755 /usr/lib/ambari-metrics-hadoop-sink

HADOOP_LINK_NAME="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar"
HADOOP_SINK_JAR="/usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink-with-common-${project_version}.jar"

FLUME_LINK_NAME="/usr/lib/flume/lib/ambari-metrics-flume-sink.jar"
FLUME_SINK_JAR="/usr/lib/flume/lib/ambari-metrics-flume-sink-with-common-${project_version}.jar"

KAFKA_LINK_NAME="/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar"
KAFKA_SINK_JAR="/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink-with-common-${project_version}.jar"


JARS=(${HADOOP_SINK_JAR} ${FLUME_SINK_JAR} ${KAFKA_SINK_JAR})
LINKS=(${HADOOP_LINK_NAME} ${FLUME_LINK_NAME} ${KAFKA_LINK_NAME})

for index in ${!LINKS[*]}
do
  rm -f ${LINKS[$index]} ; ln -s ${JARS[$index]} ${LINKS[$index]}
done

%package monitor
Summary: Ambari Metrics Monitor
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description monitor
Ambari Metrics Monitor

%post monitor
/bin/chmod 755 /usr/sbin/ambari-metrics-monitor


#######################
#### FILES SECTION ####
#######################
%files collector
%defattr(644,root,root,755)
/usr/lib/ambari-metrics-collector
/var/lib/ambari-metrics-collector
/etc/ambari-metrics-collector/conf
/usr/lib/ams-hbase
/usr/sbin/ambari-metrics-collector
/etc/ams-hbase/conf
/var/run/ams-hbase


%files grafana
%defattr(644,root,root,755)
/usr/sbin/ambari-metrics-grafana
/usr/lib/ambari-metrics-grafana/
/var/lib/ambari-metrics-grafana
/etc/ambari-metrics-grafana/conf
/var/run/ambari-metrics-grafana
/var/log/ambari-metrics-grafana

%files hadoop-sink
%defattr(644,root,root,755)
/usr/lib/ambari-metrics-hadoop-sink
/usr/lib/ambari-metrics-kafka-sink
/usr/lib/flume/lib
/usr/lib/storm/lib

%files monitor
%defattr(644,root,root,755)
/etc/ambari-metrics-monitor/conf
/usr/lib/python3.9/site-packages/resource_monitoring
/var/lib/ambari-metrics-monitor
/usr/sbin/ambari-metrics-monitor

