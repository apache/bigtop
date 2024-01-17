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

%define tez_name tez
%define tez_pkg_name tez%{pkg_name_suffix}
%define hadoop_pkg_name hadoop%{pkg_name_suffix}

%define usr_lib_tez %{parent_dir}/usr/lib/%{tez_name}
%define etc_tez %{parent_dir}/etc/%{tez_name}

%define usr_lib_hadoop %{parent_dir}/usr/lib/hadoop

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_log_tez /var/log/%{tez_name}
%define np_var_run_tez /var/run/%{tez_name}
%define np_etc_tez /etc/%{tez_name}

%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_tez %{doc_dir}/tez-%{tez_version}

%endif


%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_tez %{doc_dir}/%{tez_name}
%define alternatives_cmd update-alternatives
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%else

%define doc_tez %{doc_dir}/%{tez_name}-%{tez_version}
%define alternatives_cmd alternatives

%endif

Name: %{tez_pkg_name}
Version: %{tez_version}
Release: %{tez_release}
Summary:Apache Tez is the Hadoop enhanced Map/Reduce module.
URL: http://tez.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: Apache License v2.0
Source0: apache-%{tez_name}-%{tez_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_tez.sh
Source3: tez.1
Source4: tez-site.xml
Source5: bigtop.bom
Source6: init.d.tmpl
#BIGTOP_PATCH_FILES
BuildArch: noarch
Requires: %{hadoop_pkg_name} %{hadoop_pkg_name}-hdfs %{hadoop_pkg_name}-yarn %{hadoop_pkg_name}-mapreduce

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
%setup -q -n apache-%{tez_name}-%{tez_base_version}-src

#BIGTOP_PATCH_COMMANDS

%build
env TEZ_VERSION=%{version} VDP_RELEASE=%{vdp_release} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

cp %{SOURCE3} %{SOURCE4} .
sh %{SOURCE2} \
    --build-dir=. \
    --prefix=$RPM_BUILD_ROOT \
    --man-dir=%{man_dir} \
    --doc-dir=%{doc_tez} \
    --lib-dir=%{usr_lib_tez} \
    --etc-tez=%{etc_tez}

%__rm -f $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/slf4j-log4j12-*.jar
%__ln_s -f %{usr_lib_hadoop}/hadoop-annotations.jar $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/hadoop-annotations.jar
%__ln_s -f %{usr_lib_hadoop}/hadoop-auth.jar $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/hadoop-auth.jar
%__ln_s -f %{usr_lib_hadoop}-mapreduce/hadoop-mapreduce-client-common.jar $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/hadoop-mapreduce-client-common.jar
%__ln_s -f %{usr_lib_hadoop}-mapreduce/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/hadoop-mapreduce-client-core.jar
%__ln_s -f %{usr_lib_hadoop}-yarn/hadoop-yarn-server-web-proxy.jar $RPM_BUILD_ROOT/%{usr_lib_tez}/lib/hadoop-yarn-server-web-proxy.jar

%pre

# Manage configuration symlink
%post
%{alternatives_cmd} --install %{np_etc_tez}/conf %{tez_name}-conf %{etc_tez}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{tez_name}-conf %{etc_tez}/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root)
%config(noreplace) %{etc_tez}/conf.dist
%doc %{doc_tez}
%dir %{np_etc_tez}
%{usr_lib_tez}
%{man_dir}/man1/tez.1.*
