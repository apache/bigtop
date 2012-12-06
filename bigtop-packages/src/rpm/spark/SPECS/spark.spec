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

%define spark_name spark
%define lib_spark /usr/lib/%{spark_name}
%define etc_spark /etc/%{spark_name}
%define config_spark %{etc_spark}/conf
%define log_spark /var/log/%{spark_name}
%define bin_spark /usr/bin
%define man_dir /usr/share/man

%if  %{?suse_version:1}0
%define doc_spark %{_docdir}/spark
%define alternatives_cmd update-alternatives
%else
%define doc_spark %{_docdir}/spark-%{spark_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: spark
Version: %{spark_version}
Release: %{spark_release}
Summary: Lightning-Fast Cluster Computing
URL: http://www.spark-project.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{name}-%{spark_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{name}.sh
Requires: bigtop-utils


%description 
Spark is a MapReduce-like cluster computing framework designed to support
low-latency iterative jobs and interactive use from an interpreter. It is
written in Scala, a high-level language for the JVM, and exposes a clean
language-integrated syntax that makes it easy to write parallel jobs.
Spark runs on top of the Apache Mesos cluster manager.
    
%prep
#%setup -n %{name}-%{spark_base_version}
%setup -n mesos-spark-0472cf8

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_spark.sh \
          --build-dir=`pwd`         \
          --prefix=$RPM_BUILD_ROOT  \
          --doc-dir=%{doc_spark} 

%post
%{alternatives_cmd} --install %{config_spark} %{spark_name}-conf %{config_spark}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{spark_name}-conf %{config_spark}.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_spark}.dist
%doc %{doc_spark}
%{lib_spark}
%{bin_spark}/spark-shell
%{bin_spark}/spark-executor
