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

%define lib_dir              /usr/lib/bigtop-select
%define bin_dir              /usr/bin
%define default_parent_dir   /usr/bigtop/%{bigtop_base_version}
%if "%{parent_dir}" != ""
%define default_parent_dir   %{parent_dir}
%endif

Name: bigtop-select
Version: %{bigtop_select_version}
Release: %{bigtop_select_release}
Summary: Collection of useful tools for Bigtop

Group:      Applications/Engineering
License:    ASL 2.0
URL:        http://bigtop.apache.org/
BuildRoot:  %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
BuildArch:  noarch
Source0:    install_select.sh
Source1:    LICENSE
Source2:    distro-select
Requires:   bash

# "which" command is needed for a lot of projects.
# It is part of the package "util-linux" on suse and "which" everywhere else
%if  %{?suse_version:1}0
Requires:  util-linux
%else
Requires:       which
%endif

%description
This includes a collection of useful tools and files for Bigtop and Ambari

# disable shebang mangling of python scripts
%undefine __brp_mangle_shebangs

%prep
%setup -q -T -c
install -p -m 755 %{SOURCE0} .
install -p -m 755 %{SOURCE1} .
install -p -m 755 %{SOURCE2} .


%build


%install
bash %{SOURCE0} \
  --distro-dir=${RPM_SOURCE_DIR} \
  --build-dir=${PWD} \
  --prefix=${RPM_BUILD_ROOT} \
  --parent-dir=%{default_parent_dir} \
  --bigtop-base-version=%{bigtop_base_version}

%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%doc LICENSE

%{lib_dir}
%{default_parent_dir}

%changelog

