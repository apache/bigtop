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

%define kyuubi_name kyuubi
%define kyuubi_pkg_name kyuubi%{pkg_name_suffix}
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

%define etc_default %{parent_dir}/etc/default
%define usr_lib_kyuubi %{parent_dir}/usr/lib/%{kyuubi_name}
%define etc_kyuubi %{parent_dir}/etc/%{kyuubi_name}
%define bin_dir %{parent_dir}/%{_bindir}

%define np_var_lib_kyuubi_data /var/lib/%{kyuubi_name}/data
%define np_var_run_kyuubi /var/run/%{kyuubi_name}
%define np_var_log_kyuubi /var/log/%{kyuubi_name}
%define np_etc_kyuubi /etc/%{kyuubi_name}

%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif


# disable repacking jars
%define __os_install_post %{nil}

Name: %{kyuubi_pkg_name}
Version: %{kyuubi_version}
Release: %{kyuubi_release}
Summary: Apache Kyuubi
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %{_topdir}/INSTALL/%{kyuubi_name}-%{version}
License: ASL 2.0
Source0: %{kyuubi_name}-%{kyuubi_base_version}.tar.gz
Source1: do-component-build 
Source2: install_kyuubi.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n %{kyuubi_name}-%{kyuubi_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
bash -x %{SOURCE2} \
    --prefix=$RPM_BUILD_ROOT \
    --etc-kyuubi=%{etc_kyuubi} \
    --lib-dir=%{usr_lib_kyuubi} \
    --bin-dir=%{bin_dir} \
    --build-dir=`pwd`




%pre
getent group kyuubi >/dev/null || groupadd -r kyuubi
getent passwd kyuubi >/dev/null || useradd -c "kyuubi" -s /sbin/nologin -g kyuubi -r -d %{var_lib_kafka} kyuubi 2> /dev/null || :


%post
%{alternatives_cmd} --install %{np_etc_kyuubi}/conf %{kyuubi_name}-conf %{etc_kyuubi}/conf.dist 30


#######################
#### FILES SECTION ####
#######################

%files
%defattr(-,root,root)
%attr(0755,kyuubi,kyuubi) %config(noreplace) %{np_etc_kyuubi}
%config(noreplace) %{etc_kyuubi}/conf.dist
%dir %{_sysconfdir}/%{kyuubi_name}

%attr(0755,kyuubi,kyuubi) %{np_var_log_kyuubi}
%attr(0755,kyuubi,kyuubi) %{np_var_run_kyuubi}

%{usr_lib_kyuubi}/beeline-jars
%{usr_lib_kyuubi}/conf
%{usr_lib_kyuubi}/logs
%{usr_lib_kyuubi}/pids
%{usr_lib_kyuubi}/bin
%{usr_lib_kyuubi}/charts
%{usr_lib_kyuubi}/db-scripts
%{usr_lib_kyuubi}/docker
%{usr_lib_kyuubi}/extension
%{usr_lib_kyuubi}/externals
%{usr_lib_kyuubi}/jars
%{usr_lib_kyuubi}/web-ui
%{usr_lib_kyuubi}/work
