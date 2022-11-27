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

%define hbase_name hbase
%define hbase_pkg_name hbase%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_hbase %{parent_dir}/usr/lib/%{hbase_name}
%define var_lib_hbase %{parent_dir}/var/lib/%{hbase_name}
%define etc_hbase %{parent_dir}/etc/%{hbase_name}

%define usr_lib_hadoop %{parent_dir}/usr/lib/hadoop
%define usr_lib_zookeeper %{parent_dir}/usr/lib/zookeeper

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_log_hbase /var/log/%{hbase_name}
%define np_var_run_hbase /var/run/%{hbase_name}
%define np_etc_hbase /etc/%{hbase_name}

%define hbase_username hbase
%define hbase_services master regionserver thrift thrift2 rest


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

%define doc_hbase %{doc_dir}/%{hbase_name}
%global initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%else

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?mgaversion:1}0

# FIXME: brp-repack-jars uses unzip to expand jar files
# Unfortunately guice-2.0.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See BIGTOP-294
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}
%endif


%define doc_hbase %{doc_dir}/%{hbase_name}-%{hbase_version}
%global initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif

# Disable debuginfo package
%define debug_package %{nil}

Name: %{hbase_pkg_name}
Version: %{hbase_version}
Release: %{hbase_release}
Summary: HBase is the Hadoop database. Use it when you need random, realtime read/write access to your Big Data. This project's goal is the hosting of very large tables -- billions of rows X millions of columns -- atop clusters of commodity hardware. 
URL: http://hbase.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: %{hbase_name}-%{hbase_base_version}.tar.gz
Source1: do-component-build
Source2: install_hbase.sh
Source3: hbase.svc
Source4: init.d.tmpl
Source6: hbase.nofiles.conf
Source7: regionserver-init.d.tpl
#BIGTOP_PATCH_FILES
Requires: coreutils, /usr/sbin/useradd, /sbin/chkconfig, /sbin/service
Requires: hadoop-client, zookeeper >= 3.3.1, bigtop-utils >= 0.7

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: coreutils
%endif

%if %{?el7}0
AutoReq: no
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
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif

%description master
HMaster is the "master server" for a HBase. There is only one HMaster for a single HBase deployment.

%package regionserver
Summary: The Hadoop HBase RegionServer server.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif


%description regionserver 
HRegionServer makes a set of HRegions available to clients. It checks in with the HMaster. There are many HRegionServers in a single HBase deployment.

%package thrift
Summary: The Hadoop HBase Thrift Interface
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif


%description thrift
ThriftServer - this class starts up a Thrift server which implements the Hbase API specified in the Hbase.thrift IDL file.
"Thrift is a software framework for scalable cross-language services development. It combines a powerful software stack with a code generation engine to build services that work efficiently and seamlessly between C++, Java, Python, PHP, and Ruby. Thrift was developed at Facebook, and we are now releasing it as open source." For additional information, see http://developers.facebook.com/thrift/. Facebook has announced their intent to migrate Thrift into Apache Incubator.

%package thrift2
Summary: The Hadoop HBase Thrift2 Interface
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif


%description thrift2
Thrift2 Server to supersede original Thrift Server.
Still under development. https://issues.apache.org/jira/browse/HBASE-8818

%package doc
Summary: Hbase Documentation
Group: Documentation
BuildArch: noarch
Obsoletes: %{name}-docs

%description doc
Documentation for Hbase

%package rest
Summary: The Apache HBase REST gateway
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif


%description rest
The Apache HBase REST gateway

%prep
%setup -n %{hbase_name}-%{hbase_base_version}

#BIGTOP_PATCH_COMMANDS

%build
env HBASE_VERSION=%{version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
	--build-dir=build \
    --man-dir=%{man_dir} \
    --bin-dir=%{bin_dir} \
    --doc-dir=%{doc_hbase} \
    --lib-dir=%{usr_lib_hbase} \
    --etc-default=%{etc_default} \
    --etc-hbase=%{etc_hbase} \
    --lib-zookeeper-dir=%{usr_lib_zookeeper} \
	--prefix=$RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%__install -d -m 0755 $RPM_BUILD_ROOT/%{etc_default}/

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/security/limits.d/%{hbase_name}.nofiles.conf

%__install -d  -m 0755  %{buildroot}/%{np_var_log_hbase}
ln -s %{np_var_log_hbase} %{buildroot}/%{usr_lib_hbase}/logs

%__install -d  -m 0755  %{buildroot}/%{np_var_run_hbase}
ln -s %{np_var_run_hbase} %{buildroot}/%{usr_lib_hbase}/pids

%__install -d  -m 0755  %{buildroot}/%{var_lib_hbase}

for service in %{hbase_services}
do
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{hbase_name}-${service}
    if [[ "$service" = "regionserver" ]] ; then
        # Region servers start from a different template that allows
        # them to run multiple concurrent instances of the daemon
        %__cp %{SOURCE7} $init_file
        %__sed -i -e "s|@INIT_DEFAULT_START@|3 4 5|" $init_file
        %__sed -i -e "s|@INIT_DEFAULT_STOP@|0 1 2 6|" $init_file
        %__sed -i -e "s|@CHKCONFIG@|345 87 13|" $init_file
        %__sed -i -e "s|@HBASE_DAEMON@|${service}|" $init_file
    else
        %__sed -e "s|@HBASE_DAEMON@|${service}|" %{SOURCE3} > ${RPM_SOURCE_DIR}/hbase-${service}.node
        bash %{SOURCE4} ${RPM_SOURCE_DIR}/hbase-${service}.node rpm $init_file
    fi

    chmod 755 $init_file
done

%__install -d -m 0755 $RPM_BUILD_ROOT/%{bin_dir}

# Pull zookeeper and hadoop from their packages
rm -f $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib/{hadoop,zookeeper,slf4j-log4j12-}*.jar
ln -f -s %{usr_lib_zookeeper}/zookeeper.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib

ln -f -s %{usr_lib_hadoop}/client/hadoop-annotations.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-auth.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-common.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-hdfs.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-mapreduce-client-app.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-mapreduce-client-common.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-mapreduce-client-jobclient.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-mapreduce-client-shuffle.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-yarn-api.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-yarn-client.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-yarn-common.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib
ln -f -s %{usr_lib_hadoop}/client/hadoop-yarn-server-common.jar $RPM_BUILD_ROOT/%{usr_lib_hbase}/lib

%pre
getent group hbase 2>/dev/null >/dev/null || /usr/sbin/groupadd -r hbase
getent passwd hbase 2>&1 > /dev/null || /usr/sbin/useradd -c "HBase" -s /sbin/nologin -g hbase -r -d /var/lib/hbase hbase 2> /dev/null || :

%post
%{alternatives_cmd} --install %{np_etc_hbase}/conf %{hbase_name}-conf %{etc_hbase}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{hbase_name}-conf %{etc_hbase}/conf.dist || :
fi


#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,hbase,hbase)
%{usr_lib_hbase}/logs
%{usr_lib_hbase}/pids
%dir %{np_var_log_hbase}
%dir %{np_var_run_hbase}
%dir %{var_lib_hbase}
%dir %{np_etc_hbase}

%defattr(-,root,root)
%config(noreplace) %{etc_default}/hbase
%config(noreplace) /etc/security/limits.d/hbase.nofiles.conf
%{usr_lib_hbase}
%{usr_lib_hbase}/hbase-*.jar
%{usr_lib_hbase}/hbase-webapps
%{bin_dir}/hbase
%config(noreplace) %{etc_hbase}/conf.dist

%files doc
%defattr(-,root,root)
%doc %{doc_hbase}/


%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{hbase_name}-%1 \
%post %1 \
chkconfig --add %{hbase_name}-%1 \
\
%preun %1 \
if [ $1 = 0 ] ; then \
        service %{hbase_name}-%1 stop > /dev/null 2>&1 \
        chkconfig --del %{hbase_name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
        service %{hbase_name}-%1 condrestart >/dev/null 2>&1 \
fi
%service_macro master
%service_macro thrift
%service_macro thrift2
%service_macro regionserver
%service_macro rest
