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
Summary: Apache flink is unified stream and batch processing
License: ASL 2.0
URL: http://flink.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: flink-%{flink_base_version}.tar.gz
Source1: do-component-build
Source2: install_flink.sh
Source3: bigtop.bom
Requires: hadoop-client, bigtop-utils >= 0.7

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

%prep
%setup -n %{name}-%{flink_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build
#env flink_VERSION=%{flink_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

sh -x %{SOURCE2} --prefix=$RPM_BUILD_ROOT --doc-dir=%{doc_flink} --build-dir=%{build_flink}

%post
%{alternatives_cmd} --install %{config_flink} %{flink_name}-conf %{config_flink}.dist 30

%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_flink}.dist
%doc %{doc_flink}
%{lib_flink}
%{bin_flink}/flink
