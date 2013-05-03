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

%define lib_sqoop /usr/lib/sqoop
%define conf_sqoop %{_sysconfdir}/sqoop/conf
%define conf_sqoop_dist %{conf_sqoop}.dist
%define run_sqoop /var/run/sqoop

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

%define doc_sqoop %{_docdir}/sqoop
%define initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%else

%define doc_sqoop %{_docdir}/sqoop-%{sqoop_version}
%define initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif

Name: sqoop
Version: %{sqoop_version}
Release: %{sqoop_release}
Summary:  Tool for easy imports and exports of data sets between databases and the Hadoop ecosystem
URL: http://sqoop.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{sqoop_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: sqoop.sh
Source4: sqoop.properties
Source5: catalina.properties
Source6: setenv.sh
Source7: sqoop.default
Source8: init.d.tmpl
Source9: sqoop-server.svc
Source10: sqoop-server.sh
Buildarch: noarch
BuildRequires: asciidoc
Requires: hadoop-client, bigtop-utils >= 0.6, bigtop-tomcat, %{name}-client = %{version}-%{release}

%description
Sqoop is a tool that provides the ability to import and export data sets between the Hadoop Distributed File System (HDFS) and relational databases.

%package client
Summary: Client for Sqoop.
URL: http://sqoop.apache.org
Group: System/Daemons

%package server
Summary: Server for Sqoop.
URL: http://sqoop.apache.org
Group: System/Daemons
Requires: sqoop = %{version}-%{release}

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
Lightweight client for Sqoop.

%description server
Centralized server for Sqoop.

%prep
%setup -n sqoop-%{sqoop_base_version}

%build
# No easy way to disable the default RAT run which fails the build because of some fails in the debian/ directory
rm -rf bigtop-empty
mkdir -p bigtop-empty
# I could not find a way to add debian/ to RAT exclude list through cmd line
# or to unbind rat:check goal
# So I am redirecting its attention with a decoy
env FULL_VERSION=%{sqoop_base_version} bash %{SOURCE1} -Drat.basedir=${PWD}/bigtop-empty

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
          --build-dir=build/sqoop-%{sqoop_version} \
          --conf-dir=%{conf_sqoop_dist} \
          --doc-dir=%{doc_sqoop} \
          --prefix=$RPM_BUILD_ROOT \
          --extra-dir=$RPM_SOURCE_DIR \
          --initd-dir=%{initd_dir}

# Install init script
init_file=$RPM_BUILD_ROOT/%{initd_dir}/sqoop-server
bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/sqoop-server.svc rpm $init_file

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%pre
getent group sqoop >/dev/null || groupadd -r sqoop
getent passwd sqoop >/dev/null || useradd -c "Sqoop User" -s /sbin/nologin -g sqoop -r -d %{run_sqoop} sqoop 2> /dev/null || :
%__install -d -o sqoop -g sqoop -m 0755 /var/lib/sqoop
%__install -d -o sqoop -g sqoop -m 0755 /var/log/sqoop
%__install -d -o sqoop -g sqoop -m 0755 /var/tmp/sqoop
%__install -d -o sqoop -g sqoop -m 0755 /var/run/sqoop

%post
%{alternatives_cmd} --install %{conf_sqoop} sqoop-conf %{conf_sqoop_dist} 30

%post server
chkconfig --add sqoop-server

%preun
if [ "$1" = "0" ] ; then
  %{alternatives_cmd} --remove sqoop-conf %{conf_sqoop_dist} || :
fi

%preun server
if [ "$1" = "0" ] ; then
  service sqoop-server stop > /dev/null 2>&1
  chkconfig --del sqoop-server
fi

%postun server
if [ $1 -ge 1 ]; then
  service sqoop-server condrestart > /dev/null 2>&1
fi

%files
%defattr(0755,root,root)
/usr/bin/sqoop-server
%config(noreplace) /etc/sqoop/conf.dist
%config(noreplace) /etc/default/sqoop-server
%{lib_sqoop}/sqoop-server
%{lib_sqoop}/webapps
%{lib_sqoop}/bin/setenv.sh
/var/lib/sqoop

%files client
%attr(0755,root,root)
/usr/bin/sqoop
%{lib_sqoop}/bin/sqoop.sh
%{lib_sqoop}/client-lib

%files server
%attr(0755,root,root) %{initd_dir}/sqoop-server

