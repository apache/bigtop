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

%define flink_name flink
%define lib_flink /usr/lib/%{flink_name}
%define bin_flink /usr/bin
%define etc_flink /etc/%{flink_name}
%define config_flink %{etc_flink}/conf
%define man_dir %{_mandir}
%define flink_services master worker
%define var_lib_flink /var/lib/%{flink_name}
%define var_run_flink /var/run/%{flink_name}
%define var_log_flink /var/log/%{flink_name}


%if  %{!?suse_version:1}0
%define doc_flink %{_docdir}/%{flink_name}-%{flink_version}
%define alternatives_cmd alternatives
%define build_flink %{_builddir}/%{flink_name}-%{flink_version}/flink-dist/target/%{flink_name}-%{flink_version}-bin/%{flink_name}-%{flink_version}/

%else
%define doc_flink %{_docdir}/%{flink_name}-%{flink_version}
%define alternatives_cmd update-alternatives
%endif

Name: %{flink_name}
Version: %{flink_version}
Release: %{flink_release}
Summary: Apache Flink is an open source platform for distributed stream and batch data processing.
License: ASL 2.0
URL: http://flink.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: flink-%{flink_base_version}.tar.gz
Source1: do-component-build
Source2: install_flink.sh
Source3: init.d.tmpl
Source4: flink-master.svc
Source5: flink-worker.svc
Source6: bigtop.bom
Requires: bigtop-utils >= 0.7
Requires(preun): /sbin/service

%description
Apache Flink is an open source platform for distributed stream and batch data processing.
Flink’s core is a streaming dataflow engine that provides data distribution, communication,
and fault tolerance for distributed computations over data streams.

Flink includes several APIs for creating applications that use the Flink engine:
    * DataStream API for unbounded streams embedded in Java and Scala, and
    * DataSet API for static data embedded in Java, Scala, and Python,
    * Table API with a SQL-like expression language embedded in Java and Scala.

Flink also bundles libraries for domain-specific use cases:
    * Machine Learning library, and
    * Gelly, a graph processing API and library.

Some of the key features of Apache Flink includes.
    * Complete Event Processing (CEP)
    * Fault-tolerance via Lightweight Distributed Snapshots
    * Hadoop-native YARN & HDFS implementation

# Additions for master-worker configuration #

%global initd_dir %{_sysconfdir}/init.d

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d

%else
# Required for init scripts
Requires: /lib/lsb/init-functions
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

##############################################

%prep
%setup -n %{name}-%{flink_base_version} 

%build
bash $RPM_SOURCE_DIR/do-component-build



# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%install
%__rm -rf $RPM_BUILD_ROOT

sh -x %{SOURCE2} --prefix=$RPM_BUILD_ROOT --doc-dir=%{doc_flink} --build-dir=%{build_flink}



for service in %{flink_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{flink_name}-${service}
    bash %{SOURCE3} $RPM_SOURCE_DIR/%{flink_name}-${service}.svc rpm $init_file
done


%preun
for service in %{flink_services}; do
  /sbin/service %{flink_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{flink_name}-${service} stop > /dev/null 2>&1
  fi
done


%post
%{alternatives_cmd} --install %{config_flink} %{flink_name}-conf %{config_flink}.dist 30

###### FILES ###########

%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_flink}.dist

%dir %{_sysconfdir}/%{flink_name}
%config(noreplace) %{initd_dir}/%{flink_name}-master
%config(noreplace) %{initd_dir}/%{flink_name}-worker
%doc %{doc_flink}
%attr(0755,root,root) %{var_lib_flink}
%attr(0755,root,root) %{var_run_flink}
%attr(0755,root,root) %{var_log_flink}
%{lib_flink}
%{bin_flink}/flink
