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

%define celeborn_name celeborn
%define celeborn_pkg_name celeborn%{pkg_name_suffix}
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

%define etc_default %{parent_dir}/etc/default
%define usr_lib_celeborn %{parent_dir}/usr/lib/%{celeborn_name}
%define etc_celeborn %{parent_dir}/etc/%{celeborn_name}
%define bin_dir %{parent_dir}/%{_bindir}

%define np_var_lib_celeborn_data /var/lib/%{celeborn_name}/data
%define np_var_run_celeborn /var/run/%{celeborn_name}
%define np_var_log_celeborn /var/log/%{celeborn_name}
%define np_etc_celeborn /etc/%{celeborn_name}

%define strip_v() %{lua:print(string.gsub(rpm.expand('%{1}'), '^v', ''))}


%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif


# disable repacking jars
%define __os_install_post %{nil}

Name: %{celeborn_pkg_name}
Version: %{celeborn_version}
Release: %{celeborn_release}
Summary: Apache celeborn
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %{_topdir}/INSTALL/%{celeborn_name}-%{version}
License: ASL 2.0
Source0: %{celeborn_alias_version}.tar.gz
Source1: do-component-build
Source2: install_celeborn.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n incubator-%{celeborn_name}-%{strip_v %{celeborn_alias_version}}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
bash -x %{SOURCE2} \
    --prefix=$RPM_BUILD_ROOT \
    --etc-celeborn=%{etc_celeborn} \
    --lib-dir=%{usr_lib_celeborn} \
    --bin-dir=%{bin_dir} \
    --build-dir=`pwd`




%pre
getent group celeborn >/dev/null || groupadd -r celeborn
getent passwd celeborn >/dev/null || useradd -c "celeborn" -s /sbin/nologin -g celeborn -r -d %{var_lib_kafka} celeborn 2> /dev/null || :


%post
%{alternatives_cmd} --install %{np_etc_celeborn}/conf %{celeborn_name}-conf %{etc_celeborn}/conf.dist 30


%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{celeborn_name}-conf %{etc_celeborn}/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################

%files
%defattr(-,root,root)
%attr(0755,celeborn,celeborn) %config(noreplace) %{np_etc_celeborn}
%config(noreplace) %{etc_celeborn}/conf.dist
%dir %{_sysconfdir}/%{celeborn_name}

%attr(0755,celeborn,celeborn) %{np_var_log_celeborn}
%attr(0755,celeborn,celeborn) %{np_var_run_celeborn}

%{usr_lib_celeborn}/bin
%{usr_lib_celeborn}/conf
%{usr_lib_celeborn}/charts
%{usr_lib_celeborn}/docker
%{usr_lib_celeborn}/flink
%{usr_lib_celeborn}/jars
%{usr_lib_celeborn}/master-jars
%{usr_lib_celeborn}/mr
%{usr_lib_celeborn}/sbin
%{usr_lib_celeborn}/spark
%{usr_lib_celeborn}/worker-jars
%{usr_lib_celeborn}/logs
%{usr_lib_celeborn}/run