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

%define trino_name trino
%define trino_cli_name trino-cli
%define component_name trino
%define _build_id_links none
%define vdp_dir /usr/%{white_label}/%{vdp_version_with_bn}/
%define vdp_component_dir %{vdp_dir}/%{trino_name}
%define lib_trino %{vdp_component_dir}/lib/
%define var_lib_trino /var/lib/%{component_name}
%define var_run_trino /var/run/%{component_name}
%define var_log_trino /var/log/%{component_name}
%define bin_trino %{vdp_component_dir}/bin/
%define etc_trino %{vdp_dir}/etc/%{trino_name}
%define config_trino %{etc_trino}/conf
%define bin %{vdp_component_dir}/bin/
%define man_dir %{vdp_dir}%{component_name}/man/man1
%define trino_services server
%define trino_current_version 345
%define initd_dir %{vdp_dir}/etc/rc.d/init.d
%define _binaries_in_noarch_packages_terminate_build   0
%define distroselect %{distro_select}

%if  %{?suse_version:1}0
%define doc_trino %{_docdir}/trino
%define alternatives_cmd update-alternatives
%else
%define doc_trino %{_docdir}/trino-%{trino_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: %{trino_name}_%{vdp_version_as_name}
Version: %{trino_version}
Release: %{trino_release}
Summary: Distributed SQL Query Engine for Big Data
URL: https://trinodb.io/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %{_topdir}/INSTALL/%{component_name}-%{version}
License: ASL 2.0
Source0: %{trino_name}-%{trino_current_version}-src.tar.gz
Source1: do-component-build
Source2: install_%{trino_name}.sh
Source4: trino-server.svc
Source5: init.d.tmpl
Source6: bigtop.bom
Source7: trino.conf
Requires: bigtop-utils >= 0.7, python
Requires(preun): /sbin/service

%define initd_link /etc/rc.d

%if  %{?suse_version:1}0 && %{!?mgaversion:1}0 && %{!?amzn2:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

# if amazonlinux2
%if %{?amzn2:1}0
Requires: sh-utils, system-lsb
%define initd_link /etc/rc.d

%else
# Required for init scripts
Requires: /lib/lsb/init-functions
%define initd_link /etc/rc.d

%endif

%description
trino is an open source distributed SQL query engine for running
interactive analytic queries against data sources of all sizes ranging
from gigabytes to petabytes.

%package server
Summary: trino Server
Group: Development/Libraries
BuildArch: noarch
Requires: trino = %{version}-%{release}

%description server
Server for trino

%package cli
Summary: trino CLI
Group: Development/Libraries
BuildArch: noarch
Requires: trino = %{version}-%{release}

%description cli
CLI for trino

%prep
%setup -n %{trino_name}-%{trino_current_version}.%{vdp_version_with_bn}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

bash $RPM_SOURCE_DIR/install_trino.sh \
          --vdp-dir=%{vdp_dir} \
          --build-dir=build/%{trino_name}-%{trino_jar_version} \
          --cli-dir=build/%{trino_cli_name}-%{trino_jar_version} \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT

%post
/usr/bin/vdp/%{distroselect} --rpm-mode set trino-cli %{vdp_version_with_bn}
/usr/bin/vdp/%{distroselect} --rpm-mode set trino-worker %{vdp_version_with_bn}
/usr/bin/vdp/%{distroselect} --rpm-mode set trino-coordinator %{vdp_version_with_bn}

%files
%defattr(-,root,root,755)
%{vdp_component_dir}/validation.txt
%{vdp_component_dir}/README.txt
%{vdp_component_dir}/NOTICE
%{vdp_component_dir}/bin
%{vdp_component_dir}/lib
%{vdp_component_dir}/plugin
%{vdp_component_dir}/etc

%files cli
%{vdp_component_dir}/trino
