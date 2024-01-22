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
%define etc_default %{parent_dir}/etc/default
%define usr_lib_dinky %{parent_dir}/usr/lib/%{dinky_name}
%define etc_dinky %{parent_dir}/etc/%{dinky_name}
%define bin_dir %{parent_dir}/%{_bindir}

%define np_var_lib_dinky_data /var/lib/%{dinky_name}/data
%define np_var_run_dinky /var/run/%{dinky_name}
%define np_var_log_dinky /var/log/%{dinky_name}
%define np_etc_dinky /etc/%{dinky_name}

%define dinky_services gateway

Name: %{dinky_pkg_name}
Version: %{dinky_version}
Release: %{dinky_release}
BuildArch: noarch
Summary: Knox Gateway
URL: https://dinky.apache.org/
Group: Development/Libraries
License: ASL 2.0
Source0: %{dinky_name}-%{dinky_base_version}.zip
Source1: do-component-build
Source2: install_%{dinky_name}.sh
Source3: dinky-gateway.svc
Source4: bigtop.bom
Source6: init.d.tmpl
Requires: bigtop-utils >= 0.7
Requires(preun): /sbin/service
%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
Requires: /lib/lsb/init-functions
Requires: sh-utils, redhat-lsb
%endif

AutoProv: no
AutoReqProv: no
#BIGTOP_PATCH_FILES


%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif

%description
The Apache Knox Gateway is an Application Gateway for interacting with the REST APIs and UIs
of Apache Hadoop deployments.

The Knox Gateway provides a single access point for all REST and HTTP interactions with Apache Hadoop
clusters.

%global        initd_dir %{_sysconfdir}/init.d

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global        initd_dir %{_sysconfdir}/rc.d

%else
# Required for init scripts
Requires: /lib/lsb/init-functions

%global        initd_dir %{_sysconfdir}/init.d

%endif

# disable repacking jars
%define __os_install_post %{nil}
%define __jar_repack %{nil}

%clean
%__rm -rf $RPM_BUILD_ROOT

%prep
%setup -n %{dinky_name}-%{version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

bash -x %{SOURCE2} \
    --prefix=$RPM_BUILD_ROOT \
    --etc-dinky=%{etc_dinky} \
    --lib-dir=%{usr_lib_dinky} \
    --bin-dir=%{bin_dir} \
    --build-dir=`pwd`

for service in %{dinky_services}
do
  # Install init script
  initd_script=$RPM_BUILD_ROOT/%{initd_dir}/%{dinky_name}-${service}
  bash %{SOURCE6} $RPM_SOURCE_DIR/%{dinky_name}-${service}.svc rpm $initd_script
done


%pre
for service in %{dinky_services}; do
  /sbin/service %{dinky_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{dinky_name}-${service} stop > /dev/null 2>&1
  fi
  chkconfig --del %{dinky_name}-${service}
done

getent group dinky >/dev/null || groupadd -r dinky
getent passwd dinky >/dev/null || useradd -c "Knox" -s /sbin/nologin -g dinky -r -d %{usr_lib_dinky} dinky 2> /dev/null || :

%post
for service in %{dinky_services}; do
  chkconfig --add %{dinky_name}-${service}
done
%{alternatives_cmd} --install %{np_etc_dinky}/conf %{dinky_name}-conf %{etc_dinky}/conf.dist 30


%postun
for service in %{dinky_services}; do
  if [ $1 -ge 1 ]; then
    service %{dinky_name}-${service} condrestart >/dev/null 2>&1
  fi
done

%files
%defattr(-,root,root)
%attr(0755,dinky,dinky) %config(noreplace) %{np_etc_dinky}
%config(noreplace) %{etc_dinky}/conf.dist
%attr(0755,dinky,dinky) %config(noreplace) %{initd_dir}/%{dinky_name}-gateway
%dir %{_sysconfdir}/%{dinky_name}

%attr(0755,dinky,dinky) %{np_var_log_dinky}
%attr(0755,dinky,dinky) %{np_var_lib_dinky_data}
%attr(0755,dinky,dinky) %{np_var_run_dinky}

%{usr_lib_dinky}/data
%{usr_lib_dinky}/conf
%{usr_lib_dinky}/logs
%{usr_lib_dinky}/pids
%{usr_lib_dinky}/bin
%{usr_lib_dinky}/dep
%{usr_lib_dinky}/lib
%{usr_lib_dinky}/samples
%{usr_lib_dinky}/templates

%{bin_dir}/gateway