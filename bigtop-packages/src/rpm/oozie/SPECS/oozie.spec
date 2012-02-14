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
%define usr_bin /usr/bin
%define lib_oozie /usr/lib/oozie
%define man_dir /usr/share/man
%define conf_oozie %{_sysconfdir}/%{name}/conf
%define conf_oozie_dist %{conf_oozie}.dist
%define data_oozie /var/lib/oozie

%if  %{!?suse_version:1}0
  %define doc_oozie %{_docdir}/oozie-%{oozie_version}
  %define initd_dir %{_sysconfdir}/rc.d/init.d
  %define alternatives_cmd alternatives
%else
  %define doc_oozie %{_docdir}/oozie
  %define initd_dir %{_sysconfdir}/rc.d
  %define alternatives_cmd update-alternatives
%endif

Name: oozie
Version: %{oozie_version}
Release: %{oozie_release}
Summary:  Oozie is a system that runs workflows of Hadoop jobs.
URL: http://incubator.apache.org/oozie/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{oozie_base_version}.tar.gz
Source1: do-component-build
Source2: install_oozie.sh 
Requires(pre): /usr/sbin/groupadd, /usr/sbin/useradd
Requires(post): /sbin/chkconfig, hadoop
Requires(preun): /sbin/chkconfig, /sbin/service
Requires: zip, unzip, oozie-client = %{version}
BuildArch: noarch

%description 
 Oozie is a system that runs workflows of Hadoop jobs.
 Oozie workflows are actions arranged in a control dependency DAG (Direct
 Acyclic Graph).

 Oozie coordinator functionality allows to start workflows at regular
 frequencies and when data becomes available in HDFS.
 
 An Oozie workflow may contain the following types of actions nodes:
 map-reduce, map-reduce streaming, map-reduce pipes, pig, file-system,
 sub-workflows, java, hive, sqoop and ssh (deprecated).
 
 Flow control operations within the workflow can be done using decision,
 fork and join nodes. Cycles in workflows are not supported.
 
 Actions and decisions can be parameterized with job properties, actions
 output (i.e. Hadoop counters) and HDFS  file information (file exists,
 file size, etc). Formal parameters are expressed in the workflow definition
 as ${VAR} variables.
 
 A Workflow application is an HDFS directory that contains the workflow
 definition (an XML file), all the necessary files to run all the actions:
 JAR files for Map/Reduce jobs, shells for streaming Map/Reduce jobs, native
 libraries, Pig scripts, and other resource files.
 
 Running workflow jobs is done via command line tools, a WebServices API 
 or a Java API.
 
 Monitoring the system and workflow jobs can be done via a web console, the
 command line tools, the WebServices API and the Java API.
 
 Oozie is a transactional system and it has built in automatic and manual
 retry capabilities.
 
 In case of workflow job failure, the workflow job can be rerun skipping
 previously completed actions, the workflow application can be patched before
 being rerun.

 
%package client
Version: %{version}
Release: %{release} 
Summary:  Client for Oozie Workflow Engine
URL: http://incubator.apache.org/oozie/
Group: Development/Libraries
License: APL2
BuildArch: noarch
Requires: bigtop-utils


%description client
 Oozie client is a command line client utility that allows remote
 administration and monitoring of worflows. Using this client utility
 you can submit worflows, start/suspend/resume/kill workflows and
 find out their status at any instance. Apart from such operations,
 you can also change the status of the entire system, get vesion
 information. This client utility also allows you to validate
 any worflows before they are deployed to the Oozie server.


%prep
%setup -n yahoo-oozie-39697f6

%build
    M2_CACHE=`mktemp -d /tmp/oozie.m2.XXXXX`
    mkdir -p distro/downloads
    # curl --retry 5 -# -L -k -o distro/downloads/tomcat.tar.gz http://archive.apache.org/dist/tomcat/tomcat-6/v6.0.29/bin/apache-tomcat-6.0.29.tar.gz    
    (export DO_MAVEN_DEPLOY="";export FULL_VERSION=%{version};cp %{SOURCE1} bin; sh -x bin/do-component-build -Dmaven.repo.local=${HOME}/.m2/repository -DskipTests)
    rm -rf ${M2_CACHE}

%install
%__rm -rf $RPM_BUILD_ROOT
    sh %{SOURCE2} --extra-dir=$RPM_SOURCE_DIR --build-dir=. --server-dir=$RPM_BUILD_ROOT --client-dir=$RPM_BUILD_ROOT --docs-dir=$RPM_BUILD_ROOT%{doc_oozie} --initd-dir=$RPM_BUILD_ROOT%{initd_dir} --conf-dir=$RPM_BUILD_ROOT%{conf_oozie_dist}

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/log/oozie
%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/run/oozie

%pre
getent group oozie >/dev/null || /usr/sbin/groupadd -r oozie >/dev/null
getent passwd oozie >/dev/null || /usr/sbin/useradd --comment "Oozie User" --shell /bin/false -M -r -g oozie --home /var/run/oozie oozie >/dev/null

%post 
%{lib_oozie}/bin/oozie-setup.sh -hadoop 0.20.200 /usr/lib/hadoop
/sbin/chkconfig --add oozie 

%preun
if [ "$1" = 0 ]; then
  /sbin/service oozie stop > /dev/null
  /sbin/chkconfig --del oozie
fi

%postun
if [ $1 -ge 1 ]; then
  /sbin/service oozie condrestart > /dev/null
fi

%post client
%{alternatives_cmd} --install %{conf_oozie} %{name}-conf %{conf_oozie_dist} 30

%preun client
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{name}-conf %{conf_oozie_dist} || :
fi

%files 
%defattr(-,root,root)
%{lib_oozie}/bin/addtowar.sh
%{lib_oozie}/bin/oozie-run.sh
%{lib_oozie}/bin/oozie-setup.sh
%{lib_oozie}/bin/oozie-start.sh
%{lib_oozie}/bin/oozie-stop.sh
%{lib_oozie}/bin/oozie-sys.sh
%{lib_oozie}/bin/oozie-env.sh
%{lib_oozie}/bin/oozied.sh
%{lib_oozie}/oozie.war
%{lib_oozie}/oozie-sharelib.tar.gz
%{lib_oozie}/oozie-server
%{initd_dir}/oozie
%defattr(-, oozie, oozie)
%dir %{_localstatedir}/log/oozie
%dir %{_localstatedir}/run/oozie
%{data_oozie}

%files client
%defattr(-,root,root)
%config(noreplace) %{conf_oozie_dist}
%{usr_bin}/oozie
%dir %{lib_oozie}/bin
%{lib_oozie}/bin/oozie
%{lib_oozie}/bin/oozie-examples.sh
%{lib_oozie}/lib
%doc %{doc_oozie}
%{man_dir}/man1/oozie.1.*
