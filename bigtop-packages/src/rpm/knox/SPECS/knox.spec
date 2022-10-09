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

%define knox_name knox
%define home_dir %{parent_dir}/usr/lib/%{knox_name}

%define np_var_lib_knox /var/lib/%{knox_name}
%define np_var_run_knox /var/run/%{knox_name}
%define np_var_log_knox /var/log/%{knox_name}
%define np_etc_knox /etc/%{knox_name}

%define knox_services gateway

Name: knox
Version: %{knox_version}
Release: %{knox_release}
BuildArch: noarch
Summary: Knox Gateway
URL: https://knox.apache.org/
Group: Development/Libraries
License: ASL 2.0
Source0: %{name}-%{knox_base_version}.zip
Source1: do-component-build
Source2: install_%{name}.sh
Source3: knox-gateway.svc
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
%setup -n %{name}-%{version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_knox}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_knox}

bash -x %{SOURCE2} --prefix=$RPM_BUILD_ROOT --build-dir=`pwd`

for service in %{knox_services}
do
  # Install init script
  initd_script=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
  bash %{SOURCE6} $RPM_SOURCE_DIR/%{name}-${service}.svc rpm $initd_script
done


%pre
for service in %{knox_services}; do
  /sbin/service %{name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{name}-${service} stop > /dev/null 2>&1
  fi
  chkconfig --del %{name}-${service}
done

getent group knox >/dev/null || groupadd -r knox
getent passwd knox >/dev/null || useradd -c "Knox" -s /sbin/nologin -g knox -r -d %{home_dir} knox 2> /dev/null || :

%post
for service in %{knox_services}; do
  chkconfig --add %{name}-${service}
done

%postun
for service in %{knox_services}; do
  if [ $1 -ge 1 ]; then
    service %{name}-${service} condrestart >/dev/null 2>&1
  fi
done

%files
%defattr(-,root,root)


%{home_dir}
%attr(0755,knox,knox) %{np_etc_knox}
%attr(0755,knox,knox) %{np_var_lib_knox}
%attr(0755,knox,knox) %{np_var_run_knox}
%attr(0755,knox,knox) %{np_var_log_knox}
%attr(0755,root,root) /usr/bin/gateway
%attr(0755,root,root) %{initd_dir}/%{name}*

