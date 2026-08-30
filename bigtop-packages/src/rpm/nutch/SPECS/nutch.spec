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

%define nutch_name nutch
%define nutch_pkg_name %{nutch_name}%{?pkg_name_suffix:%{pkg_name_suffix}}
%define hadoop_pkg_name hadoop%{?pkg_name_suffix:%{pkg_name_suffix}}

%define etc_default %{?parent_dir:/%{parent_dir}}/etc/default
%define usr_lib_nutch %{?parent_dir:/%{parent_dir}}/usr/lib/%{nutch_name}
%define etc_nutch %{?parent_dir:/%{parent_dir}}/etc/%{nutch_name}
%define bin_dir %{?parent_dir:/%{parent_dir}}%{_bindir}

%define __os_install_post %{nil}

Name: %{nutch_pkg_name}
Version: %{nutch_version}
Release: %{nutch_release}
Summary: Apache Nutch - extensible, scalable web crawler
URL: https://nutch.apache.org
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0

Source0: apache-nutch-%{nutch_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_nutch.sh
Source3: nutch.default

Requires: bigtop-utils >= 0.7
Requires: %{hadoop_pkg_name}-client

%if %{?suse_version:1}0
%else
BuildRequires: ant
%endif

%description
Apache Nutch is an open source web crawler. It uses Apache Hadoop data
structures and MapReduce for batch processing, and can integrate with
Apache Solr or Elasticsearch for indexing and search.

%prep
%setup -q -n apache-nutch-%{nutch_base_version}

%build
env FULL_VERSION=%{nutch_base_version} HADOOP_VERSION=%{hadoop_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
  --build-dir=%{_builddir}/apache-nutch-%{nutch_base_version} \
  --prefix=$RPM_BUILD_ROOT \
  --distro-dir=$RPM_SOURCE_DIR \
  --bin-dir=$RPM_BUILD_ROOT%{bin_dir} \
  --lib-dir=$RPM_BUILD_ROOT%{usr_lib_nutch} \
  --etc-default=$RPM_BUILD_ROOT%{etc_default} \
  --conf-dir=$RPM_BUILD_ROOT%{etc_nutch}

%files
%defattr(-,root,root,-)
%config(noreplace) %{etc_default}/nutch
%dir %{etc_nutch}
%{etc_nutch}/conf.dist
%{usr_lib_nutch}
%{bin_dir}/nutch
