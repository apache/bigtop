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

%global debug_package %{nil}
%define _build_id_links none

%define airflow_name airflow
%define airflow_pkg_name %{airflow_name}%{pkg_name_suffix}
%define lib_dir /usr/lib/airflow
%define var_dir /var/lib/airflow

Name: %{airflow_name}
Version: %{airflow_version}
Release: %{airflow_release}
Summary: Apache Airflow
Group:     Applications/Engineering
License:   ASL 2.0
URL:       https://airflow.apache.org/
BuildRoot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
Source0:   apache_%{airflow_name}-%{airflow_base_version}.tar.gz
Source1:   do-component-build
Source2:   install_airflow.sh
#BIGTOP_PATCH_FILES
Requires:  bash
# "which" command is needed for a lot of projects.
# It is part of the package "util-linux" on suse and "which" everywhere else
%if  %{?suse_version:1}0
Requires: util-linux
%else
Requires: which
%endif

%description
Apache Airflow is a platform to programmatically author, schedule, and monitor workflows.

%prep
%setup -n apache_%{airflow_name}-%{airflow_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
  --distro-dir=${RPM_SOURCE_DIR} \
  --build-dir=build \
  --prefix=${RPM_BUILD_ROOT}

%pre
getent group airflow >/dev/null || groupadd -r airflow
getent passwd airflow > /dev/null || useradd -c "airflow" -s /sbin/nologin -g airflow -r -d %{var_dir} airflow 2> /dev/null || :

%post
AIRFLOW_HOME=%{var_dir} %{lib_dir}/bin/airflow db init
chown -R airflow:airflow %{var_dir}

%files
%defattr(-,root,root)
%{lib_dir}

%attr(0644,root,root) %{_unitdir}/airflow-scheduler.service
%attr(0644,root,root) %{_unitdir}/airflow-webserver.service

%config(noreplace) /etc/default/airflow
%config(noreplace) %{_tmpfilesdir}/airflow.conf
