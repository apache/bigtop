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
%define etc_zookeeper /etc/%{name}
%define bin_zookeeper %{_bindir}
%define lib_zookeeper /usr/lib/%{name}
%define log_zookeeper /var/log/%{name}
%define run_zookeeper /var/run/%{name}
%define vlb_zookeeper /var/lib/%{name}
%define svc_zookeeper %{name}-server
%define man_dir %{_mandir}

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict anc check all symlinks point to valid path
# But we do point to a hadoop jar which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}


%define doc_zookeeper %{_docdir}/%{name}
%define alternatives_cmd update-alternatives
%define alternatives_dep update-alternatives
%define chkconfig_dep    aaa_base
%define service_dep      aaa_base
%global initd_dir %{_sysconfdir}/rc.d

%else

%define doc_zookeeper %{_docdir}/%{name}-%{zookeeper_version}
%define alternatives_cmd alternatives
%define alternatives_dep chkconfig 
%define chkconfig_dep    chkconfig
%define service_dep      initscripts
%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif



Name: zookeeper
Version: %{zookeeper_version}
Release: %{zookeeper_release}
Summary: A high-performance coordination service for distributed applications.
URL: http://zookeeper.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: %{name}-%{zookeeper_base_version}.tar.gz
Source1: do-component-build
Source2: install_zookeeper.sh
Source3: zookeeper.1
Source4: zoo.cfg
Source5: zookeeper.default
Source6: zookeeper.service

#BIGTOP_PATCH_FILES
BuildRequires: autoconf, automake, cppunit-devel
Requires(pre): coreutils, /usr/sbin/groupadd, /usr/sbin/useradd
Requires(post): %{alternatives_dep}
Requires(preun): %{alternatives_dep}
#Requires: bigtop-utils >= 0.7

%description 
ZooKeeper is a centralized service for maintaining configuration information, 
naming, providing distributed synchronization, and providing group services. 
All of these kinds of services are used in some form or another by distributed 
applications. Each time they are implemented there is a lot of work that goes 
into fixing the bugs and race conditions that are inevitable. Because of the 
difficulty of implementing these kinds of services, applications initially 
usually skimp on them ,which make them brittle in the presence of change and 
difficult to manage. Even when done correctly, different implementations of these services lead to management complexity when the applications are deployed.  

%package native
Summary: C bindings for ZooKeeper clients
Group: Development/Libraries

%description native
Provides native libraries and development headers for C / C++ ZooKeeper clients. Consists of both single-threaded and multi-threaded implementations.

%prep
%setup -n %{name}-%{zookeeper_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
cp $RPM_SOURCE_DIR/zookeeper.1 $RPM_SOURCE_DIR/zoo.cfg $RPM_SOURCE_DIR/zookeeper.default .
bash %{SOURCE2} \
          --build-dir=build/%{name}-%{zookeeper_base_version} \
          --doc-dir=%{doc_zookeeper} \
          --prefix=$RPM_BUILD_ROOT \
          --system-include-dir=%{_includedir} \
          --system-lib-dir=%{_libdir}
cp -R  %{SOURCE6} $RPM_BUILD_ROOT/usr/lib/systemd/system/

%pre
getent group zookeeper >/dev/null || groupadd -r zookeeper
getent passwd zookeeper > /dev/null || useradd -c "ZooKeeper" -s /sbin/nologin -g zookeeper -r -d %{vlb_zookeeper} zookeeper 2> /dev/null || :

%__install -d -o zookeeper -g zookeeper -m 0755 %{run_zookeeper}
%__install -d -o zookeeper -g zookeeper -m 0755 %{log_zookeeper}

# Manage configuration symlink
%post
%{alternatives_cmd} --install %{etc_zookeeper}/conf %{name}-conf %{etc_zookeeper}/conf.dist 30
%__install -d -o zookeeper -g zookeeper -m 0755 %{vlb_zookeeper}
systemctl daemon-reload

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{etc_zookeeper}/conf.dist || :
fi


#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root)
%config(noreplace) %{etc_zookeeper}/conf.dist
%config(noreplace) /etc/default/zookeeper
%{lib_zookeeper}
%{bin_zookeeper}/zookeeper-server
%{bin_zookeeper}/zookeeper-server-initialize
%{bin_zookeeper}/zookeeper-client
%{bin_zookeeper}/zookeeper-server-cleanup
%doc %{doc_zookeeper}
%{man_dir}/man1/zookeeper.1.*
%attr(0755,root,root) /usr/lib/systemd/system/

%files native
%defattr(-,root,root)
%{lib_zookeeper}-native
%{bin_zookeeper}/cli_*
%{bin_zookeeper}/load_gen*
%{_includedir}/zookeeper
%{_libdir}/*

