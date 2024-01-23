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

%define dinky_name dinky
%define dinky_pkg_name dinky%{pkg_name_suffix}
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

%define etc_default %{parent_dir}/etc/default
%define usr_lib_dinky %{parent_dir}/usr/lib/%{dinky_name}
%define etc_dinky %{parent_dir}/etc/%{dinky_name}
%define bin_dir %{parent_dir}/%{_bindir}

%define np_var_lib_dinky_data /var/lib/%{dinky_name}/data
%define np_var_run_dinky /var/run/%{dinky_name}
%define np_var_log_dinky /var/log/%{dinky_name}
%define np_etc_dinky /etc/%{dinky_name}

%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif


# disable repacking jars
%define __os_install_post %{nil}

Name: %{dinky_pkg_name}
Version: %{dinky_version}
Release: %{dinky_release}
Summary: Apache dinky
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %{_topdir}/INSTALL/%{dinky_name}-%{version}
License: ASL 2.0
Source0: %{dinky_name}-%{dinky_base_version}.tar.gz
Source1: do-component-build
Source2: install_dinky.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n %{dinky_name}-%{dinky_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
bash -x %{SOURCE2} \
    --prefix=$RPM_BUILD_ROOT \
    --etc-dinky=%{etc_dinky} \
    --lib-dir=%{usr_lib_dinky} \
    --bin-dir=%{bin_dir} \
    --build-dir=`pwd`




%pre
getent group dinky >/dev/null || groupadd -r dinky
getent passwd dinky >/dev/null || useradd -c "dinky" -s /sbin/nologin -g dinky -r -d %{var_lib_kafka} dinky 2> /dev/null || :


%post
%{alternatives_cmd} --install %{np_etc_dinky}/conf %{dinky_name}-conf %{etc_dinky}/conf.dist 30


#######################
#### FILES SECTION ####
#######################

%files
%defattr(-,root,root)
%attr(0755,dinky,dinky) %config(noreplace) %{np_etc_dinky}
%config(noreplace) %{etc_dinky}/conf.dist
%dir %{_sysconfdir}/%{dinky_name}

%attr(0755,dinky,dinky) %{np_var_log_dinky}
%attr(0755,dinky,dinky) %{np_var_run_dinky}

%{usr_lib_dinky}/extends
%{usr_lib_dinky}/jar
%{usr_lib_dinky}/sql
%{usr_lib_dinky}/lib
%{usr_lib_dinky}/dink-loader
%{usr_lib_dinky}/config
%{usr_lib_dinky}/logs
%{usr_lib_dinky}/run
%{usr_lib_dinky}/auto.sh
