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

%define accumulo_conf %{_sysconfdir}/%{name}/conf
%define accumulo_conf_dist %{accumulo_conf}.dist

%if  %{?suse_version:1}0
%define doc_accumulo %{_docdir}/%{name}
%define initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives
%else
%define doc_accumulo %{_docdir}/%{name}-%{accumulo_version}
%define initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives
%endif

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict and checks all symlinks point to valid path
# But we do point to a hadoop jar which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif

Name: accumulo
Version: %{accumulo_version}
Release: %{accumulo_release}
Summary: The Apache Accumulo sorted, distributed key/value store is a robust, scalable, high performance data storage and retrieval system.
URL: http://accumulo.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{version}-src.tar.gz
Source1: do-component-build
Source2: install_accumulo.sh
Source3: init.d.tmpl
Source4: accumulo-master.svc
Source5: accumulo-tserver.svc
Source6: accumulo-gc.svc
Source7: accumulo-monitor.svc
Source8: accumulo-tracer.svc
Requires: zookeeper, hadoop-client, bigtop-utils >= 0.6
Obsoletes: accumulo-logger

%description
Apache Accumulo is a highly scalable structured store based on Google’s BigTable. Accumulo is written in Java and operates over the Hadoop Distributed File System (HDFS), which is part of the popular Apache Hadoop project. Accumulo supports efficient storage and retrieval of structured data, including queries for ranges, and provides support for using Accumulo tables as input and output for MapReduce jobs.
Accumulo features automatic load-balancing and partitioning, data compression and fine-grained security labels.

%package master
Summary: The Accumulo Master server.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description master
The Accumulo Master is responsible for detecting and responding to TabletServer failure. It tries to balance the load across TabletServer by assigning tablets carefully and instructing TabletServers to unload tablets when necessary. The Master ensures all tablets are assigned to one TabletServer each, and handles table creation, alteration, and deletion requests from clients. The Master also coordinates startup, graceful shutdown and recovery of changes in write-ahead logs when Tablet servers fail.
Multiple masters may be run. The masters will choose among themselves a single master, and the others will become backups if the master should fail.

%package tserver
Summary: The Accumulo TabletServer daemon.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description tserver
The TabletServer manages some subset of all the tablets (partitions of tables). This includes receiving writes from clients, persisting writes to a write-ahead log, sorting new key-value pairs in memory, periodically flushing sorted key-value pairs to new files in HDFS, and responding to reads from clients, forming a merge-sorted view of all keys and values from all the files it has created and the sorted in-memory store.
TabletServers also perform recovery of a tablet that was previously on a server that failed, reapplying any writes found in the write-ahead log to the tablet.

%package gc
Summary: The Accumulo Garbage Collector daemon
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description gc
Accumulo processes will share files stored in HDFS. Periodically, the Garbage
Collector will identify files that are no longer needed by any process, and
delete them.

%package monitor
Summary: The Accumulo Monitor daemon
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description monitor
The Monitor serves as a log aggregation point for the other servers and
provides a web UI for basic monitoring.

%package tracer
Summary: The Accumulo Tracer daemon
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description tracer
The Tracer does distributed profiling of runtime performance. profiling of
low-level internal operations always happens, and users may optionally turn
on more comprehensive profiling on a per-table basis.

%package doc
Summary: The Accumulo Documentation
Group: Documentation

%description doc
Documentation for Accumulo

# use the debug_package macro if needed
%if  %{!?suse_version:1}0
# RedHat does this by default
%else
%debug_package
%endif

%prep
%setup -n %{name}-%{accumulo_base_version}

%build
env ACCUMULO_VERSION=%{version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
env ACCUMULO_VERSION=%{version} bash %{SOURCE2} \
    --build-dir=. \
    --doc-dir=%{doc_accumulo} \
    --conf-dir=%{accumulo_conf} \
    --prefix=$RPM_BUILD_ROOT \
    --extra-dir=$RPM_SOURCE_DIR

# Install init scripts
for service in master tserver gc monitor tracer; do \
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/accumulo-${service}; \
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/accumulo-${service}.svc rpm $init_file; \
done

%pre
getent group accumulo 2>/dev/null >/dev/null || /usr/sbin/groupadd -r accumulo
getent passwd accumulo 2>&1 > /dev/null || /usr/sbin/useradd -c "Accumulo" -s /sbin/nologin -g accumulo -r -d /var/lib/accumulo accumulo 2> /dev/null || :

%post
%{alternatives_cmd} --install %{accumulo_conf} %{name}-conf %{accumulo_conf_dist} 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{accumulo_conf_dist} || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root)
/usr/lib/accumulo
/usr/bin/accumulo
%config(noreplace) %{accumulo_conf_dist}
%config(noreplace) /etc/default/accumulo
%defattr(-,accumulo,accumulo)
/var/log/accumulo
/var/lib/accumulo

%files doc
%defattr(-,root,root)
%{doc_accumulo}

%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{name}-%1 \
%post %1 \
chkconfig --add %{name}-%1 \
\
%preun %1 \
if [ $1 = 0 ] ; then \
        service %{name}-%1 stop > /dev/null 2>&1 \
        chkconfig --del %{name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
        service %{name}-%1 condrestart >/dev/null 2>&1 \
fi
%service_macro master
%service_macro tserver
%service_macro gc
%service_macro monitor
%service_macro tracer
