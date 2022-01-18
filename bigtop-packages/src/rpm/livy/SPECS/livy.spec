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

%define lib_livy /usr/lib/%{name}
%define etc_livy %{_sysconfdir}/%{name}
%define config_livy %{etc_livy}/conf
%define livy_services server
%define var_lib_livy /var/lib/%{name}
%define var_run_livy /var/run/%{name}
%define var_log_livy /var/log/%{name}

Name: livy
Version: %{livy_version}
Release: %{livy_release}
BuildArch: noarch
Summary: Livy Server
URL: http://livy.incubator.apache.org/
Group: Development/Libraries
License: ASL 2.0
Source0: %{name}-%{livy_base_version}.zip
Source1: do-component-build
Source2: install_%{name}.sh
Source3: livy-server.svc
Source4: bigtop.bom
Source6: init.d.tmpl
Requires: bigtop-utils >= 0.7
Requires(preun): /sbin/service
#BIGTOP_PATCH_FILES

%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif

%description
Apache Livy is an open source REST interface for interacting with Apache Spark from anywhere.
It supports executing snippets of code or programs in a Spark context that runs locally or in Apache Hadoop YARN.

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

bash -x %{SOURCE2} --prefix=$RPM_BUILD_ROOT --build-dir=build

for service in %{livy_services}
do
  # Install init script
  initd_script=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
  bash %{SOURCE6} $RPM_SOURCE_DIR/%{name}-${service}.svc rpm $initd_script
done

%preun
for service in %{livy_services}; do
  /sbin/service %{name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{name}-${service} stop > /dev/null 2>&1
  fi
  chkconfig --del %{name}-${service}
done

%pre
getent group livy >/dev/null || groupadd -r livy
getent passwd livy >/dev/null || useradd -c "Livy" -s /sbin/nologin -g livy -r -d %{var_lib_livy} livy 2> /dev/null || :

%post
install --owner livy --group livy --directory --mode=0755 %{var_log_livy}
%{alternatives_cmd} --install %{config_livy} %{name}-conf %{config_livy}.dist 30
for service in %{livy_services}; do
  chkconfig --add %{name}-${service}
done

%postun
for service in %{livy_services}; do
  if [ $1 -ge 1 ]; then
    service %{name}-${service} condrestart >/dev/null 2>&1
  fi
done

%{alternatives_cmd} --remove %{name}-conf %{config_livy}.dist

%files
%defattr(-,root,root)
%config(noreplace) %{config_livy}.dist
%{lib_livy}
%attr(0755,livy,livy) %{var_lib_livy}
%attr(0755,livy,livy) %{var_run_livy}
%attr(0755,livy,livy) %{var_log_livy}
%attr(0755,root,root) %{initd_dir}/%{name}*
