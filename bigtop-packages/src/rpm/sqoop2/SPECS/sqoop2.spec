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

%define lib_sqoop /usr/lib/sqoop2
%define conf_sqoop %{_sysconfdir}/sqoop2/conf
%define conf_sqoop_dist %{conf_sqoop}.dist
%define tomcat_conf_sqoop %{_sysconfdir}/sqoop2/tomcat-conf
%define tomcat_conf_sqoop_dist %{tomcat_conf_sqoop}.dist
%define run_sqoop /var/run/sqoop2

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. let's update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict anc check all symlinks point to valid path
# But we do point to a conf which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%define doc_sqoop %{_docdir}/sqoop2
%define initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%else

%define doc_sqoop %{_docdir}/sqoop2-%{sqoop2_version}
%define initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif

Name: sqoop2
Version: %{sqoop2_version}
Release: %{sqoop2_release}
Summary:  Tool for easy imports and exports of data sets between databases and the Hadoop ecosystem
URL: http://sqoop.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: sqoop-%{sqoop2_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: sqoop.sh
Source5: catalina.properties
Source7: sqoop2.default
Source8: init.d.tmpl
Source9: sqoop-server.svc
Source10: sqoop-server.sh
Source11: sqoop-tool.sh
Source12: tomcat-deployment.sh
#BIGTOP_PATCH_FILES
Buildarch: noarch
Requires: hadoop-client, bigtop-utils >= 0.7, bigtop-tomcat >= 0.7, %{name}-client = %{version}-%{release}

%description
Sqoop is a tool that provides the ability to import and export data sets between
the Hadoop Distributed File System (HDFS) and relational databases. In Sqoop 2, the tool
consists of a server that is configured to interface with the Hadoop cluster, and a
lightweight client for executing imports and exports on the server.

%package client
Summary: Client for Sqoop 2.
URL: http://sqoop.apache.org
Requires: bigtop-utils >= 0.7
Group: System/Daemons

%package server
Summary: Server for Sqoop 2.
URL: http://sqoop.apache.org
Group: System/Daemons
Requires: sqoop2 = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%endif

%if  0%{?mgaversion}
# Required for init scripts
Requires: initscripts
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description client
Lightweight client for Sqoop 2.

%description server
Centralized server for Sqoop 2.

%prep
%setup -n sqoop-%{sqoop2_base_version}

#BIGTOP_PATCH_COMMANDS

%build
# No easy way to disable the default RAT run which fails the build because of some fails in the debian/ directory
rm -rf bigtop-empty
mkdir -p bigtop-empty
env FULL_VERSION=%{sqoop2_base_version} bash %{SOURCE1} -Drat.basedir=${PWD}/bigtop-empty

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
          --build-dir=build/sqoop2-%{sqoop2_base_version} \
          --conf-dir=%{conf_sqoop_dist} \
          --doc-dir=%{doc_sqoop} \
          --prefix=$RPM_BUILD_ROOT \
          --extra-dir=$RPM_SOURCE_DIR \
          --initd-dir=%{initd_dir} \
          --dist-dir=dist/target/sqoop2-%{sqoop2_base_version}

# Install init script
init_file=$RPM_BUILD_ROOT/%{initd_dir}/sqoop2-server
bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/sqoop-server.svc rpm $init_file

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%pre
getent group sqoop >/dev/null || groupadd -r sqoop
getent passwd sqoop2 >/dev/null || useradd -c "Sqoop 2 User" -s /sbin/nologin -g sqoop -r -d /var/lib/sqoop2 sqoop2 2> /dev/null || :
%__install -d -o sqoop2 -g sqoop -m 0755 /var/lib/sqoop2
%__install -d -o sqoop2 -g sqoop -m 0755 /var/log/sqoop2
%__install -d -o sqoop2 -g sqoop -m 0755 /var/tmp/sqoop2
%__install -d -o sqoop2 -g sqoop -m 0755 /var/run/sqoop2

%post
%{alternatives_cmd} --install %{conf_sqoop} sqoop2-conf %{conf_sqoop_dist} 30
%{alternatives_cmd} --install %{tomcat_conf_sqoop} sqoop2-tomcat-conf %{tomcat_conf_sqoop_dist} 30

%post server
chkconfig --add sqoop2-server

%preun
if [ "$1" = "0" ] ; then
  %{alternatives_cmd} --remove sqoop2-conf %{conf_sqoop_dist} || :
  %{alternatives_cmd} --remove sqoop2-tomcat-conf %{tomcat_conf_sqoop_dist} || :
fi

%preun server
if [ "$1" = "0" ] ; then
  service sqoop2-server stop > /dev/null 2>&1
  chkconfig --del sqoop2-server
fi

%postun server
if [ $1 -ge 1 ]; then
  service sqoop2-server condrestart > /dev/null 2>&1
fi

%files
%defattr(0755,root,root)
/usr/bin/sqoop2-server
/usr/bin/sqoop2-tool
%config(noreplace) /etc/sqoop2/conf.dist
%config(noreplace) /etc/sqoop2/tomcat-conf.dist
%config(noreplace) /etc/default/sqoop2-server
%{lib_sqoop}/webapps
%{lib_sqoop}/bin/setenv.sh
%{lib_sqoop}/bin/sqoop-sys.sh
%{lib_sqoop}/tomcat-deployment.sh
%defattr(0755,sqoop2,sqoop)
/var/lib/sqoop2
/usr/lib/bigtop-tomcat/lib/sqoop-tomcat*.jar

%files client
%defattr(0755,root,root)
/usr/bin/sqoop2
%dir %{lib_sqoop}
%dir %{lib_sqoop}/bin
%{lib_sqoop}/bin/sqoop.sh
%{lib_sqoop}/client-lib
%{lib_sqoop}/LICENSE.txt
%{lib_sqoop}/NOTICE.txt

%files server
%attr(0755,root,root) %{initd_dir}/sqoop2-server

