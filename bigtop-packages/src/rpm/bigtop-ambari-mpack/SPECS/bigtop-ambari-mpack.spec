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

%define pkg_name bigtop-ambari-mpack
%define lib_bigtop_ambari_mpack /usr/lib/%{pkg_name}
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

# disable repacking jars
%define __os_install_post %{nil}

# Disable debuginfo package
%define debug_package %{nil}

Name: bigtop-ambari-mpack
Version: %{bigtop_ambari_mpack_version}
Release: %{bigtop_ambari_mpack_release}
Summary: Bigtop Ambari Management Packages
Group: Application/Internet
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: apache-ambari-%{bigtop_ambari_mpack_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_mpack.sh
Requires: bigtop-utils >= 0.7
Buildarch: noarch
AutoProv: no
AutoReqProv: no

%description
Apache Ambari Management Packs decouples Ambari's core functionality (cluster management and monitoring)
from stack management and definition. Mpack can bundle multiple service definitions, stack definitions,
stack add-on service definitions, view definitions services.

%prep
%setup -n apache-ambari-%{bigtop_ambari_mpack_base_version}-src

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
bash $RPM_SOURCE_DIR/install_mpack.sh \
          --build-dir=`pwd` \
          --prefix=$RPM_BUILD_ROOT \
          --distro-dir=$RPM_SOURCE_DIR

%files
%defattr(-,root,root,755)
%{lib_bigtop_ambari_mpack}
