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
%define man_dir %{_mandir}

%if  %{?suse_version:1}0
%define bin_gpdb /usr/lib/gpdb
%define doc_gpdb %{_docdir}/%{name}
%define autorequire no
%else
%define bin_gpdb /usr/lib/gpdb
%define doc_gpdb %{_docdir}/%{name}-%{gpdb_version}
%define autorequire yes
%endif
%define  debug_package %{nil}

%undefine _auto_set_build_flags

%global __brp_check_rpaths %{nil}

Name: gpdb
Version: %{gpdb_version}
Release: %{gpdb_release}
Summary: Greenplum MPP database enginer
URL: https://github.com/greenplum-db/gpdb
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: gpdb-%{gpdb_base_version}.tar.gz
Source1: do-component-build
Source2: install_gpdb.sh
Source3: do-component-configure
#BIGTOP_PATCH_FILES
AutoReqProv: %{autorequire}

#python2 be compiled manually and install, not installed by rpm in openEuler
%if 0%{?openEuler}
Requires: bigtop-utils >= 0.7, gcc, libffi-devel, make, openssl-devel
%else
Requires: bigtop-utils >= 0.7, gcc, libffi-devel, make, openssl-devel, python3-devel
%endif

%description
gpdb

%prep
%setup -n %{name}-%{gpdb_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE3} %{bin_gpdb}
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} %{_tmppath}
mkdir -p $RPM_BUILD_ROOT%{bin_gpdb}
cp -f -r %{_tmppath}%{bin_gpdb}/* $RPM_BUILD_ROOT/%{bin_gpdb}
%__rm -rf %{_tmppath}%{bin_gpdb}

%files
%defattr(-,root,root)
%{bin_gpdb}

%changelog
