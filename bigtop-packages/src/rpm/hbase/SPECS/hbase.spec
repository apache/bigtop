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
%define etc_hbase /etc/%{name}
%define hbase_home /usr/lib/%{name}
%define bin_hbase %{hbase_home}/bin
%define lib_hbase %{hbase_home}/lib
%define conf_hbase %{hbase_home}/conf
%define logs_hbase %{hbase_home}/logs
%define pids_hbase %{hbase_home}/pids
%define webapps_hbase %{hbase_home}/hbase-webapps
%define man_dir %{_mandir}
%define hbase_username hbase
%define hbase_services master regionserver thrift
%define hadoop_home /usr/lib/hadoop
%define zookeeper_home /usr/lib/zookeeper
%define hbase_jar_deps %{hadoop_home}/hadoop-core.jar %{zookeeper_home}/zookeeper.jar

%if  %{?suse_version:1}0

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

%define doc_hbase %{_docdir}/%{name}
%global initd_dir %{_sysconfdir}/rc.d

%else

%define doc_hbase %{_docdir}/%{name}-%{hbase_version}
%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif


Name: hbase
Version: %{hbase_version}
Release: %{hbase_release}
Summary: HBase is the Hadoop database. Use it when you need random, realtime read/write access to your Big Data. This project's goal is the hosting of very large tables -- billions of rows X millions of columns -- atop clusters of commodity hardware. 
URL: http://hbase.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{hbase_base_version}.tar.gz
Source1: do-component-build
Source2: install_hbase.sh
Source3: hbase.sh
Source4: hbase.sh.suse
Source5: hbase.default
Source6: hbase.nofiles.conf
BuildArch: noarch
Requires: coreutils, /usr/sbin/useradd, /sbin/chkconfig, /sbin/service
Requires: hadoop >= 0.20.2, zookeeper >= 3.3.1, bigtop-utils

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: sh-utils
%endif


%description 
HBase is an open-source, distributed, column-oriented store modeled after Google' Bigtable: A Distributed Storage System for Structured Data by Chang et al. Just as Bigtable leverages the distributed data storage provided by the Google File System, HBase provides Bigtable-like capabilities on top of Hadoop. HBase includes:

    * Convenient base classes for backing Hadoop MapReduce jobs with HBase tables
    * Query predicate push down via server side scan and get filters
    * Optimizations for real time queries
    * A high performance Thrift gateway
    * A REST-ful Web service gateway that supports XML, Protobuf, and binary data encoding options
    * Cascading source and sink modules
    * Extensible jruby-based (JIRB) shell
    * Support for exporting metrics via the Hadoop metrics subsystem to files or Ganglia; or via JMX

%package master
Summary: The Hadoop HBase master Server.
Group: System/Daemons
Provides: %{name}-master
Requires: %{name} = %{version}-%{release}

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

%description master
HMaster is the "master server" for a HBase. There is only one HMaster for a single HBase deployment.

%package regionserver
Summary: The Hadoop HBase RegionServer server.
Group: System/Daemons
Provides: %{name}-regionserver
Requires: %{name} = %{version}-%{release}

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


%description regionserver 
HRegionServer makes a set of HRegions available to clients. It checks in with the HMaster. There are many HRegionServers in a single HBase deployment.

%package thrift
Summary: The Hadoop HBase Thrift Interface
Group: System/Daemons
Provides: %{name}-thrift
Requires: %{name} = %{version}-%{release}

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


%description thrift
ThriftServer - this class starts up a Thrift server which implements the Hbase API specified in the Hbase.thrift IDL file.
"Thrift is a software framework for scalable cross-language services development. It combines a powerful software stack with a code generation engine to build services that work efficiently and seamlessly between C++, Java, Python, PHP, and Ruby. Thrift was developed at Facebook, and we are now releasing it as open source." For additional information, see http://developers.facebook.com/thrift/. Facebook has announced their intent to migrate Thrift into Apache Incubator.

%package doc
Summary: Hbase Documentation
Group: Documentation
BuildArch: noarch
Obsoletes: %{name}-docs

%description doc
Documentation for Hbase


%prep
%setup -n %{name}-%{hbase_base_version}

%build
env HBASE_VERSION=%{version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
	--build-dir=. \
   --doc-dir=%{doc_hbase} \
	--prefix=$RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE5} $RPM_BUILD_ROOT/etc/default/%{name}

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/security/limits.d/%{name}.nofiles.conf

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/log/%{name}
ln -s %{_localstatedir}/log/%{name} %{buildroot}/%{logs_hbase}

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/run/%{name}
ln -s %{_localstatedir}/run/%{name} %{buildroot}/%{pids_hbase}

%if  %{?suse_version:1}0
orig_init_file=%{SOURCE4}
%else
orig_init_file=%{SOURCE3}
%endif

for service in %{hbase_services}
do
	init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
	%__cp $orig_init_file $init_file
	%__sed -i -e "s|@HBASE_DAEMON@|${service}|" $init_file
	chmod 755 $init_file
done

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

# Pull zookeeper and hadoop from their packages
ln -f -s %{hbase_jar_deps} $RPM_BUILD_ROOT/%{lib_hbase}

%pre
getent group hbase 2>/dev/null >/dev/null || /usr/sbin/groupadd -r hbase
getent passwd hbase 2>&1 > /dev/null || /usr/sbin/useradd -c "HBase" -s /sbin/nologin -g hbase -r -d /var/run/hbase hbase 2> /dev/null || :

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,hbase,hbase)
%{logs_hbase}
%{pids_hbase}
%dir %{_localstatedir}/log/hbase
%dir %{_localstatedir}/run/hbase

%defattr(-,root,root)
%config(noreplace) %{_sysconfdir}/default/hbase
%config(noreplace) /etc/security/limits.d/hbase.nofiles.conf
%{hbase_home}
%{hbase_home}/hbase-*.jar
%{webapps_hbase}
/usr/bin/hbase
%config(noreplace) %{etc_hbase}/conf

%files doc
%defattr(-,root,root)
%doc %{doc_hbase}/


%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{name}-%1 \
%post %1 \
chkconfig --add %{name}-%1 \
\
%preun %1 \
if [ $1 = 0 ] ; then \
        service %{name}-%1 stop > /dev/null 2>&1 \
        chkconfig --del %{name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
        service %{name}-%1 condrestart >/dev/null 2>&1 \
fi
%service_macro master
%service_macro thrift
%service_macro regionserver
