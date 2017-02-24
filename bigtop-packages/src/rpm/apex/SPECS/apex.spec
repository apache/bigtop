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

%define apex_name apex
%define lib_apex /usr/lib/%{apex_name}
%define bin_apex /usr/bin
%define man_dir %{_mandir}

%if  %{!?suse_version:1}0

%define doc_apex %{_docdir}/%{apex_name}-%{apex_version}
%define alternatives_cmd alternatives

%else

%define doc_apex %{_docdir}/%{name}
%define alternatives_cmd update-alternatives

%endif

Name: %{apex_name}
Version: %{apex_version}
Release: %{apex_release}
Summary: Apache Apex is an enterprise Grade YARN-native platform for unified stream and batch processing
License: ASL 2.0
URL: http://apex.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: apex-%{apex_base_version}.tar.gz
Source1: do-component-build
Source2: install_apex.sh
Source3: apex.1
Source4: bigtop.bom
Requires: hadoop-client, bigtop-utils >= 0.7

%description
Apache Apex is a free, open source implementation for unified stream and batch processing.

Apache Apex includes following key features:
 * Event processing guarantees
 * In-memory performance & scalability
 * Fault tolerance and state management
 * Native rolling and tumbling window support
 * Hadoop-native YARN & HDFS implementation

%prep
%setup -n apache-%{name}-core-%{apex_base_version}

%build
env APEX_VERSION=%{apex_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

cp $RPM_SOURCE_DIR/apex.1 .
sh -x %{SOURCE2} --prefix=$RPM_BUILD_ROOT --doc-dir=$RPM_BUILD_ROOT/%{doc_apex}

%files 
%defattr(-,root,root,755)
%doc %{doc_apex}
%{lib_apex}
%{bin_apex}/apex
%{man_dir}/man1/apex.1.*
