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
%define livy_name livy
%define etc_livy /etc/%{livy_name}
%define config_livy %{etc_livy}/conf
%define lib_livy /usr/lib/%{livy_name}
%define log_livy /var/log/%{livy_name}
%define bin_livy /usr/bin
%define livy_config_virtual livy_active_configuration
%define man_dir %{_mandir}
%define hive_home /usr/lib/hive
%define zookeeper_home /usr/lib/zookeeper
%define hbase_home /usr/lib/hbase
#BIGTOP_PATCH_FILES

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

# FIXME: brp-repack-jars uses unzip to expand jar files
# Unfortunately aspectjtools-1.6.5.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See BIGTOP-294
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_livy %{_docdir}/livy-%{livy_version}
%define alternatives_cmd alternatives

%endif


%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_livy %{_docdir}/livy
%define alternatives_cmd update-alternatives
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif


%if  0%{?mgaversion}
%define doc_livy %{_docdir}/livy-%{livy_version}
%define alternatives_cmd update-alternatives
%endif


Name: livy
Version: %{livy_version}
Release: %{livy_release}
Summary: livy is a REST service for Apache Spark
License: ASL 2.0
URL: http://livy.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: livy-%{livy_base_version}.tar.gz
Source1: do-component-build
Source2: install_livy.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7

%description 
Apache Livy is a service that enables easy interaction with a Spark cluster over a REST interface. It enables easy submission of Spark jobs or snippets of Spark code, synchronous or asynchronous result retrieval, as well as Spark Context management, all via a simple REST interface or an RPC client library. Apache Livy also simplifies the interaction between Spark and application servers, thus enabling the use of Spark for interactive web/mobile applications. Additional features include:

Have long running Spark Contexts that can be used for multiple Spark jobs, by multiple clients
Share cached RDDs or Dataframes across multiple jobs and clients
Multiple Spark Contexts can be managed simultaneously, and the Spark Contexts run on the cluster (YARN/Mesos) instead of the Livy Server, for good fault tolerance and concurrency
Jobs can be submitted as precompiled jars, snippets of code or via java/scala client API
Ensure security via secure authenticated communication

%prep
%setup -n %{name}-%{livy_base_version}
#BIGTOP_PATCH_COMMANDS
%build
env livyOD_BASE_VERSION=%{livy_base_version} bash %{SOURCE1}


#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{livy_version}

#######################
#### FILES SECTION ####
#######################
%files 
%config /etc/livy
%doc
/usr/lib/livy
