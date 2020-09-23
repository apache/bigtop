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
%define lib_tomcat %{_usr}/lib/%{name}

%if  %{?suse_version:1}0
  %define doc_tomcat %{_docdir}/%{name}
%else
  %define doc_tomcat %{_docdir}/%{name}-%{version}
%endif

Name: bigtop-tomcat
Version: %{bigtop_tomcat_version}
Release: %{bigtop_tomcat_release}
Summary: Apache Tomcat
URL: http://tomcat.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: apache-tomcat-%{bigtop_tomcat_base_version}.tar.gz
Source1: do-component-build
Source2: install_tomcat.sh
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7

%description 
Apache Tomcat is an open source software implementation of the
Java Servlet and JavaServer Pages technologies.

%prep
%setup -n apache-tomcat-%{bigtop_tomcat_base_version}-src

#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
          --build-dir=build \
	  --doc-dir=%{doc_tomcat} \
          --prefix=$RPM_BUILD_ROOT

%files 
%defattr(-,root,root)
%attr(0755,root,root) %{lib_tomcat}
%doc %{doc_tomcat}

%changelog

