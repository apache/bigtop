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

%define ycsb_name ycsb
%define lib_ycsb /usr/lib/ycsb

# disable repacking jars
%define __os_install_post %{nil}

Name: ycsb
Version: %{ycsb_version}
Release: %{ycsb_release}
Summary: Yahoo Cloud Serving Benchmark
URL: http://labs.yahoo.com/news/yahoo-cloud-serving-benchmark
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{ycsb_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{ycsb_name}-%{ycsb_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{ycsb_name}.sh
Source3: bigtop.bom
Requires: python

%description 
The Yahoo! Cloud Serving Benchmark (YCSB) is an open-source 
specification and program suite for evaluating retrieval and maintenance 
capabilities of computer programs. It is often used to compare relative 
performance of NoSQL database management systems.

%prep
%setup -n YCSB-%{ycsb_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_ycsb.sh --build-dir=build/dist --prefix=$RPM_BUILD_ROOT

%files 
%defattr(-,root,root,755)
%{lib_ycsb}
