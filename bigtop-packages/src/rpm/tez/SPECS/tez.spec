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
%define tez_home /usr/lib/%{name}
%define lib_tez %{tez_home}/lib
%define man_dir %{_mandir}


%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_tez %{_docdir}/tez-%{tez_version}

%endif


%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_tez %{_docdir}/%{name}
%define alternatives_cmd update-alternatives
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%else

%define doc_tez %{_docdir}/%{name}-%{tez_version}
%define alternatives_cmd alternatives

%endif

Name: tez
Version: %{tez_version}
Release: %{tez_release}
Summary:Apache Tez is the Hadoop enhanced Map/Reduce module.
URL: http://tez.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: Apache License v2.0
Source0: apache-%{name}-%{tez_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_tez.sh
Source3: tez.1
Source4: tez-site.xml
Source5: bigtop.bom
Source6: init.d.tmpl
#BIGTOP_PATCH_FILES
BuildArch: noarch
Requires: hadoop hadoop-hdfs hadoop-yarn hadoop-mapreduce

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: coreutils
%endif


%description
The Apache Tez project is aimed at building an application framework
which allows for a complex directed-acyclic-graph of tasks for
processing data. It is currently built atop Apache Hadoop YARN

%prep
%setup -q -n apache-%{name}-%{tez_base_version}-src

#BIGTOP_PATCH_COMMANDS

%build
env TEZ_VERSION=%{version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

cp %{SOURCE3} %{SOURCE4} .
sh %{SOURCE2} \
	--build-dir=. \
        --doc-dir=%{doc_tez} \
        --libexec-dir=%{libexec_tez} \
	--prefix=$RPM_BUILD_ROOT

%__rm -f $RPM_BUILD_ROOT/%{lib_tez}/slf4j-log4j12-*.jar
%__ln_s -f /usr/lib/hadoop/hadoop-annotations.jar $RPM_BUILD_ROOT/%{lib_tez}/hadoop-annotations.jar
%__ln_s -f /usr/lib/hadoop/hadoop-auth.jar $RPM_BUILD_ROOT/%{lib_tez}/hadoop-auth.jar
%__ln_s -f /usr/lib/hadoop-mapreduce/hadoop-mapreduce-client-common.jar $RPM_BUILD_ROOT/%{lib_tez}/hadoop-mapreduce-client-common.jar
%__ln_s -f /usr/lib/hadoop-mapreduce/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{lib_tez}/hadoop-mapreduce-client-core.jar
%__ln_s -f /usr/lib/hadoop-yarn/hadoop-yarn-server-web-proxy.jar $RPM_BUILD_ROOT/%{lib_tez}/hadoop-yarn-server-web-proxy.jar

%pre

# Manage configuration symlink
%post
%{alternatives_cmd} --install /etc/tez/conf %{name}-conf /etc/tez/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf /etc/tez/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root)
%config(noreplace) /etc/tez/conf.dist
%{tez_home}
%doc %{doc_tez}
%{man_dir}/man1/tez.1.*
