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
%define lib_whirr /usr/lib/whirr
%define man_dir /usr/share/man

%if  %{?suse_version:1}0
  %define doc_whirr %{_docdir}/whirr
%else
  %define doc_whirr %{_docdir}/whirr-%{whirr_version}
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: whirr
Version: %{whirr_version}
Release: %{whirr_release}
Summary: Scripts and libraries for running software services on cloud infrastructure.
URL: http://whirr.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{name}-%{whirr_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: whirr.1
Requires: bigtop-utils

%description 
Whirr provides

* A cloud-neutral way to run services. You don't have to worry about the
  idiosyncrasies of each provider.
* A common service API. The details of provisioning are particular to the
  service.
* Smart defaults for services. You can get a properly configured system
  running quickly, while still being able to override settings as needed.
    

%prep
%setup -n %{name}-%{whirr_base_version}-src

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
cp $RPM_SOURCE_DIR/whirr.1 .
bash %{SOURCE2} \
          --build-dir=build \
	  --doc-dir=$RPM_BUILD_ROOT%{doc_whirr} \
          --prefix=$RPM_BUILD_ROOT

%files 
%defattr(-,root,root)
%attr(0755,root,root) %{lib_whirr}
%attr(0755,root,root) %{_bindir}/%{name}
%doc %attr(0644,root,root) %{man_dir}/man1/whirr.1.*
%doc %{doc_whirr}
