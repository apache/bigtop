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

%define kite_name kite 
%define lib_kite /usr/lib/kite
%define lib_zookeeper /usr/lib/zookeeper
%define lib_hadoop /usr/lib/hadoop

%if  %{?suse_version:1}0
%define doc_kite %{_docdir}/kite-doc
%else
%define doc_kite %{_docdir}/kite-doc-%{kite_version}
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: kite
Version: %{kite_version}
Release: %{kite_release}
Summary: Kite Software Development Kit.
URL: http://kitesdk.org
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{kite_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{kite_name}-%{kite_version}.tar.gz
Source1: do-component-build 
Source2: install_%{kite_name}.sh
Source3: bigtop.bom
Requires: hadoop-client, hbase, hive, zookeeper, bigtop-utils >= 0.7 

%description 
The Kite Software Development Kit is a set of libraries, tools, examples, and
documentation focused on making it easier to build systems on top of the
Hadoop ecosystem.

%prep
%setup -n %{kite_name}-release-%{kite_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
KITE_VERSION=%{kite_version} bash $RPM_SOURCE_DIR/install_kite.sh \
          --build-dir=build/kite-%{kite_base_version} \
          --prefix=$RPM_BUILD_ROOT
          
%__rm -f $RPM_BUILD_ROOT/%{lib_kite}/lib/zookeeper*.jar
%__rm -f $RPM_BUILD_ROOT/%{lib_kite}/lib/hadoop*.jar

%__ln_s %{lib_zookeeper}/zookeeper.jar $RPM_BUILD_ROOT/%{lib_kite}/lib/
%__ln_s %{lib_hadoop}/client/hadoop-annotations.jar $RPM_BUILD_ROOT/%{lib_kite}/lib/
%__ln_s %{lib_hadoop}/client/hadoop-auth.jar $RPM_BUILD_ROOT/%{lib_kite}/lib/
%__ln_s %{lib_hadoop}/client/hadoop-common.jar $RPM_BUILD_ROOT/%{lib_kite}/lib/
%__ln_s %{lib_hadoop}/client/hadoop-hdfs.jar $RPM_BUILD_ROOT/%{lib_kite}/lib/

%files 
%defattr(-,root,root,755)
/usr/bin/kite-dataset
%{lib_kite}
%{lib_kite}/bin
%{lib_kite}/lib

