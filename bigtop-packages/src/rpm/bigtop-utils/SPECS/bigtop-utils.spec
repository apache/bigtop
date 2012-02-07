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

# See BIGTOP-383
%if  %{?suse_version:1}0
%define _libexecdir /usr/lib
%endif

Name: bigtop-utils
Version: %{bigtop_utils_version}
Release: %{bigtop_utils_release}
Summary:	Collection of useful tools for Bigtop

Group:		Applications/Engineering
License:	APL2
URL:		http://incubator.apache.org/bigtop/
Source0:	bigtop-detect-javahome
Source1:	LICENSE
Source2:    bigtop-utils.default

BuildRoot:	%(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)

BuildArch: noarch

%description
This includes a collection of useful tools and files for Bigtop


%prep
%setup -q -T -c
install -p -m 644 %{SOURCE0} .
install -p -m 644 %{SOURCE1} .
install -p -m 644 %{SOURCE2} .

%build


%install
install -d -p -m 755 $RPM_BUILD_ROOT%{_libexecdir}/
install -d -p -m 755 $RPM_BUILD_ROOT/etc/default
install -p -m 755 %{SOURCE0} $RPM_BUILD_ROOT%{_libexecdir}/
install -p -m 644 %{SOURCE2} $RPM_BUILD_ROOT/etc/default/bigtop-utils

%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%doc LICENSE
%config(noreplace) /etc/default/bigtop-utils

%{_libexecdir}/bigtop-detect-javahome

%changelog


