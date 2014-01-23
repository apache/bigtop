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

%define bin_groovy /usr/lib/bigtop-groovy

Name: bigtop-groovy
Version: %{bigtop_groovy_version}
Release: %{bigtop_groovy_release}
Summary: An agile and dynamic language for the Java Virtual Machine
URL: http://groovy.codehaus.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch:  noarch
License: ASL 2.0
Source0: %{name}-%{bigtop_groovy_base_version}.tar.gz
Source1: do-component-build
Source2: install_groovy.sh
Requires: bigtop-utils >= 0.7

%description 
Groovy provides a JVM based runtime environment for function programming and scripting.

%prep
%setup -n %{name}-%{bigtop_groovy_base_version}

%clean
rm -rf $RPM_BUILD_ROOT

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
          --build-dir=.         \
          --bin-dir=%{bin_groovy}/bin \
          --version=%{bigtop_groovy_version} \
          --lib-dir=%{bin_groovy}/lib \
          --conf-dir=%{bin_groovy}/conf \
          --prefix=$RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%{bin_groovy}
%attr(0775,root,root) %{bin_groovy}/bin/*

%changelog

