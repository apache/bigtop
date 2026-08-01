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

%define lib_dir /usr/lib/bigtop-utils
%define plugins_dir /var/lib/bigtop

Name: bigtop-utils
Version: %{bigtop_utils_version}
Release: %{bigtop_utils_release}
Summary: Collection of useful tools for Bigtop

Group:      Applications/Engineering
License:    ASL 2.0
URL:        http://bigtop.apache.org/
BuildRoot:  %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
BuildArch:  noarch
Source0:    bigtop-detect-javahome
Source1:    LICENSE
Source2:    bigtop-utils.default
Source3:    bigtop-detect-javalibs
Source4:    bigtop-detect-classpath
Source5:    bigtop-monitor-service
Source6:    bigtop-detect-pythonpath
Requires:   bash

# "which" command is needed for a lot of projects.
# It is part of the package "util-linux" on suse and "which" everywhere else
%if  %{?suse_version:1}0
Requires:  util-linux
%else
Requires:       which
%endif

%description
This includes a collection of useful tools and files for Bigtop


%prep
%setup -q -T -c
install -p -m 644 %{SOURCE0} .
install -p -m 644 %{SOURCE1} .
install -p -m 644 %{SOURCE2} .
install -p -m 644 %{SOURCE3} .
install -p -m 644 %{SOURCE4} .
install -p -m 644 %{SOURCE5} .
install -p -m 644 %{SOURCE6} .

%build


%install
install -d -p -m 755 $RPM_BUILD_ROOT%{plugins_dir}/
install -d -p -m 755 $RPM_BUILD_ROOT%{lib_dir}/
install -d -p -m 755 $RPM_BUILD_ROOT/etc/default
install -p -m 755 %{SOURCE0} $RPM_BUILD_ROOT%{lib_dir}/
install -p -m 755 %{SOURCE3} $RPM_BUILD_ROOT%{lib_dir}/
install -p -m 755 %{SOURCE4} $RPM_BUILD_ROOT%{lib_dir}/
install -p -m 755 %{SOURCE5} $RPM_BUILD_ROOT%{lib_dir}/
install -p -m 755 %{SOURCE6} $RPM_BUILD_ROOT%{lib_dir}/
install -p -m 644 %{SOURCE2} $RPM_BUILD_ROOT/etc/default/bigtop-utils

%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%doc LICENSE
%config(noreplace) /etc/default/bigtop-utils

%{lib_dir}
%{plugins_dir}

%changelog

