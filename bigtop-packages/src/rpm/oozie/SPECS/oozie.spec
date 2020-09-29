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
%define tomcat_conf_oozie %{_sysconfdir}/%{name}/tomcat-conf
%define data_oozie /var/lib/oozie
%define lib_hadoop /usr/lib/hadoop

%if  %{!?suse_version:1}0
  %define doc_oozie %{_docdir}/oozie-%{oozie_version}
  %define initd_dir %{_sysconfdir}/rc.d/init.d
  %define alternatives_cmd alternatives
%else

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict anc check all symlinks point to valid path
# But we do point to a hadoop jar which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

  %define doc_oozie %{_docdir}/oozie
  %define initd_dir %{_sysconfdir}/rc.d
  %define alternatives_cmd update-alternatives
%endif

Name: oozie
Version: %{oozie_version}
Release: %{oozie_release}
Summary:  Oozie is a system that runs workflows of Hadoop jobs.
URL: http://oozie.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: %{name}-%{oozie_base_version}.tar.gz
Source1: do-component-build
Source2: install_oozie.sh
Source3: oozie.1
Source4: oozie-env.sh
Source5: oozie.init
Source6: catalina.properties
Source7: context.xml
Source8: hive.xml
Source9: tomcat-deployment.sh
Source10: oozie-site.xml
Source11: bigtop.bom
#BIGTOP_PATCH_FILES
Requires(pre): /usr/sbin/groupadd, /usr/sbin/useradd
Requires(post): /sbin/chkconfig
Requires(preun): /sbin/chkconfig, /sbin/service
Requires: oozie-client = %{version}, hadoop-client, bigtop-tomcat
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
 as ${VARIABLE NAME} variables.

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
URL: http://oozie.apache.org/
Group: Development/Libraries
License: ASL 2.0
BuildArch: noarch
Requires: bigtop-utils >= 0.7


%description client
 Oozie client is a command line client utility that allows remote
 administration and monitoring of worflows. Using this client utility
 you can submit worflows, start/suspend/resume/kill workflows and
 find out their status at any instance. Apart from such operations,
 you can also change the status of the entire system, get vesion
 information. This client utility also allows you to validate
 any worflows before they are deployed to the Oozie server.


%prep
%setup -n oozie-%{oozie_base_version}

#BIGTOP_PATCH_COMMANDS

%build
    mkdir -p distro/downloads
    env DO_MAVEN_DEPLOY="" FULL_VERSION=%{oozie_base_version} bash -x %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
    sh %{SOURCE2} --extra-dir=$RPM_SOURCE_DIR --build-dir=$PWD --server-dir=$RPM_BUILD_ROOT --client-dir=$RPM_BUILD_ROOT --docs-dir=$RPM_BUILD_ROOT%{doc_oozie} --initd-dir=$RPM_BUILD_ROOT%{initd_dir} --conf-dir=$RPM_BUILD_ROOT%{conf_oozie_dist}

%__ln_s -f %{data_oozie}/ext-2.2 $RPM_BUILD_ROOT/%{lib_oozie}/webapps/oozie/ext-2.2
%__rm  -rf              $RPM_BUILD_ROOT/%{lib_oozie}/webapps/oozie/docs
%__ln_s -f %{doc_oozie} $RPM_BUILD_ROOT/%{lib_oozie}/webapps/oozie/docs

# Oozie server
%__rm  -rf $RPM_BUILD_ROOT/%{lib_oozie}/lib/hadoop-*.jar
%__ln_s -f %{lib_hadoop}/client/hadoop-annotations.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-auth.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-common.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-hdfs-client.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-mapreduce-client-app.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-mapreduce-client-common.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-mapreduce-client-jobclient.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-mapreduce-client-shuffle.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-yarn-api.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-yarn-client.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-yarn-common.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/
%__ln_s -f %{lib_hadoop}/client/hadoop-yarn-server-common.jar $RPM_BUILD_ROOT/%{lib_oozie}/lib/

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/log/oozie
%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/run/oozie

%pre
getent group oozie >/dev/null || /usr/sbin/groupadd -r oozie >/dev/null
getent passwd oozie >/dev/null || /usr/sbin/useradd --comment "Oozie User" --shell /bin/false -M -r -g oozie --home %{data_oozie} oozie >/dev/null

%post
%{alternatives_cmd} --install %{conf_oozie} %{name}-conf %{conf_oozie_dist} 30
%{alternatives_cmd} --install %{tomcat_conf_oozie} %{name}-tomcat-conf %{tomcat_conf_oozie}.http 30
%{alternatives_cmd} --install %{tomcat_conf_oozie} %{name}-tomcat-conf %{tomcat_conf_oozie}.https 20

/sbin/chkconfig --add oozie

%preun
if [ "$1" = 0 ]; then
  rm -r /etc/oozie/conf/tomcat-conf
  /sbin/service oozie stop > /dev/null
  /sbin/chkconfig --del oozie
  %{alternatives_cmd} --remove %{name}-tomcat-conf %{tomcat_conf_oozie}.http || :
  %{alternatives_cmd} --remove %{name}-tomcat-conf %{tomcat_conf_oozie}.https || :
  %{alternatives_cmd} --remove %{name}-conf %{conf_oozie_dist} || :
fi

%postun
if [ $1 -ge 1 ]; then
  /sbin/service oozie condrestart > /dev/null
fi

%files
%defattr(-,root,root)
%config(noreplace) %{conf_oozie_dist}
%config(noreplace) %{tomcat_conf_oozie}.*
%{usr_bin}/oozie-setup
%{lib_oozie}/bin/oozie-sys.sh
%{lib_oozie}/bin/oozie-env.sh
%{lib_oozie}/bin/oozied.sh
%{lib_oozie}/bin/ooziedb.sh
%{lib_oozie}/bin/oozie-setup.sh
%{lib_oozie}/webapps
%{lib_oozie}/libtools
%{lib_oozie}/lib
%{lib_oozie}/oozie-sharelib.tar.gz
%{lib_oozie}/libext
%{lib_oozie}/tomcat-deployment.sh
%{initd_dir}/oozie
%defattr(-, oozie, oozie)
%dir %{_sysconfdir}/%{name}
%dir %{_localstatedir}/log/oozie
%dir %{_localstatedir}/run/oozie
%attr(0755,oozie,oozie) %{data_oozie}

%files client
%defattr(-,root,root)
%{usr_bin}/oozie
%dir %{lib_oozie}
%{lib_oozie}/bin/oozie
%{lib_oozie}/conf/oozie-client-env.sh
%{lib_oozie}/lib
%{lib_oozie}/oozie-examples.tar.gz
%doc %{doc_oozie}
%{man_dir}/man1/oozie.1.*
