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

%define kyuubi_name kyuubi
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0


# disable repacking jars
%define __os_install_post %{nil}

Name: kyuubi
Version: %{kyuubi_version}
Release: %{kyuubi_release}
Summary: Apache Kyuubi
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %{_topdir}/INSTALL/%{kyuubi_name}-%{version}
License: ASL 2.0
Source0: %{kyuubi_name}-%{kyuubi_base_version}.tar.gz
Source1: do-component-build 
Source2: install_kyuubi.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n %{kyuubi_name}-%{kyuubi_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
AMBARI_VERSION=%{ambari_version} bash $RPM_SOURCE_DIR/install_kyuubi.sh \
          --build-dir=`pwd` \
          --distro-dir=$RPM_SOURCE_DIR \
          --source-dir=`pwd` \
          --prefix=$RPM_BUILD_ROOT


%package kyuubi
Summary: Apache Kyuubi
Group: Development/Libraries
AutoProv: no
AutoReqProv: no
%description kyuubi
Apache Kyuubi





#######################
#### FILES SECTION ####
#######################

%files kyuubi
%defattr(644,root,root,755)
/usr/lib/kyuubi


