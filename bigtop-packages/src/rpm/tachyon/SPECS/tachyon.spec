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

Name:           tachyon
Version:        %{tachyon_version}
Release:        %{tachyon_release}
Summary:       Reliable file sharing at memory speed across cluster frameworks
License:       ASL 2.0
URL:           http://tachyon-project.org/
Group:         Development/Libraries
BuildArch:     noarch

Source0:       %{name}-%{tachyon_version}.tar.gz
Source1:       do-component-build 
Source2:       install_tachyon.sh
Source3:       init.d.tmpl
%define        tachyon_home /usr/lib/%{name}

# disable repacking jars
%define __arch_install_post %{nil}

%description
Tachyon is a fault tolerant distributed file system
enabling reliable file sharing at memory-speed
across cluster frameworks, such as Spark and MapReduce.
It achieves high performance by leveraging lineage
information and using memory aggressively.
Tachyon caches working set files in memory, and
enables different jobs/queries and frameworks to
access cached files at memory speed. Thus, Tachyon
avoids going to disk to load data-sets that
are frequently read.

%prep
%setup -n %{name}-%{tachyon_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
rm -rf $RPM_BUILD_ROOT

# See /usr/lib/rpm/macros for info on how vars are defined.
# Here we run the tachyon installation script. 
bash %{SOURCE2} \
    --build-dir=%{buildroot} \
    --bin-dir=%{_bindir} \
    --data-dir=%{_datadir} \
    --libexec-dir=%{_libexecdir} \
    --var-dir=%{_var}  \
    --prefix="${RPM_BUILD_ROOT}"


%files  
%defattr(-,root,root,-)
%doc LICENSE README.md
%dir %{_sysconfdir}/%{name}
%config(noreplace) %{_sysconfdir}/%{name}/log4j.properties
%config(noreplace) %{_sysconfdir}/%{name}/slaves
%config(noreplace) %{_sysconfdir}/%{name}/tachyon-env.sh
%config(noreplace) %{tachyon_home}/libexec/tachyon-layout.sh
%{tachyon_home}/tachyon*
%{tachyon_home}/bin/tachyon*
%{tachyon_home}/libexec/tachyon*
%{_datadir}/%{name}
/usr/bin/tachyon

%clean
rm -rf $RPM_BUILD_ROOT

%changelog
