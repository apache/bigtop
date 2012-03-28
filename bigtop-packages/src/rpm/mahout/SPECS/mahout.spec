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

%define mahout_name mahout
%define lib_mahout /usr/lib/%{mahout_name}
%define etc_mahout /etc/%{mahout_name}
%define config_mahout %{etc_mahout}/conf
%define log_mahout /var/log/%{mahout_name}
%define bin_mahout /usr/bin
%define man_dir /usr/share/man

%if  %{?suse_version:1}0
%define doc_mahout %{_docdir}/mahout
%define alternatives_cmd update-alternatives
%else
%define doc_mahout %{_docdir}/mahout-%{mahout_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: mahout
Version: %{mahout_version}
Release: %{mahout_release}
Summary: A set of Java libraries for scalable machine learning.
URL: http://mahout.apache.org
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{name}-distribution-%{mahout_base_version}-src.tar.gz
Source1: do-component-build 
Source2: install_%{name}.sh
Patch0: MAHOUT-822.patch
Requires: hadoop-client, bigtop-utils


%description 
Mahout's goal is to build scalable machine learning libraries. 
With scalable we mean:

Scalable to reasonably large data sets. Our core algorithms for clustering,
classfication and batch based collaborative filtering are implemented on top of
Apache Hadoop using the map/reduce paradigm. However we do not restrict
contributions to Hadoop based implementations: Contributions that run on a
single node or on a non-Hadoop cluster are welcome as well. The core libraries
are highly optimized to allow for good performance also for non-distributed
algorithms.
Scalable to support your business case. Mahout is distributed under a 
commercially friendly Apache Software license.
Scalable community. The goal of Mahout is to build a vibrant, responsive,
diverse community to facilitate discussions not only on the project itself but
also on potential use cases. Come to the mailing lists to find out more.
    
%prep
%setup -n %{name}-distribution-%{mahout_base_version}
%patch0 -p0

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_mahout.sh \
          --build-dir=build \
          --prefix=$RPM_BUILD_ROOT \
          --doc-dir=%{doc_mahout} 
rm -f $RPM_BUILD_ROOT/usr/lib/mahout/lib/hadoop*.jar
ln -s /usr/lib/hadoop/hadoop-core.jar $RPM_BUILD_ROOT/usr/lib/mahout/lib/hadoop-core.jar

%post
%{alternatives_cmd} --install %{config_mahout} %{mahout_name}-conf %{config_mahout}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{mahout_name}-conf %{config_mahout}.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_mahout}.dist
%doc %{doc_mahout}
%{lib_mahout}
%{bin_mahout}/mahout


