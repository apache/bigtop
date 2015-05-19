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

%define crunch_name crunch
%define lib_crunch /usr/lib/crunch
%define crunch_folder apache-%{crunch_name}-%{crunch_base_version}-src
%define zookeeper_home /usr/lib/zookeeper
%define hadoop_home /usr/lib/hadoop

%if  %{?suse_version:1}0
%define doc_crunch %{_docdir}/crunch-doc
%else
%define doc_crunch %{_docdir}/crunch-doc-%{crunch_version}
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: crunch
Version: %{crunch_version}
Release: %{crunch_release}
Summary: Simple and Efficient MapReduce Pipelines.
URL: http://crunch.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{crunch_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{crunch_folder}.tar.gz
Source1: do-component-build 
Source2: install_%{crunch_name}.sh
Source3: bigtop.bom
Requires: hadoop-client, bigtop-utils >= 0.7

%description 
Apache Crunch is a Java library for writing, testing, and running
MapReduce pipelines, based on Google's FlumeJava. Its goal is to make 
pipelines that are composed of many user-defined functions simple to write, 
easy to test, and efficient to run.

%package doc
Summary: Apache Crunch documentation
Group: Documentation
%description doc
Apache Crunch documentation

%prep
%setup -n %{crunch_folder}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
bash $RPM_SOURCE_DIR/install_crunch.sh \
          --build-dir=${PWD}/build   \
          --doc-dir=%{doc_crunch}    \
          --prefix=$RPM_BUILD_ROOT

ln -fs %{zookeeper_home}/zookeeper.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/

ln -fs %{hadoop_home}/client/hadoop-annotations.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/
ln -fs %{hadoop_home}/client/hadoop-auth.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/
ln -fs %{hadoop_home}/client/hadoop-common.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/
ln -fs %{hadoop_home}/client/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/
ln -fs %{hadoop_home}/client/hadoop-yarn-api.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/
ln -fs %{hadoop_home}/client/hadoop-yarn-common.jar $RPM_BUILD_ROOT/%{lib_crunch}/lib/

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%{lib_crunch}

%files doc
%defattr(-,root,root,755)
%{doc_crunch}
