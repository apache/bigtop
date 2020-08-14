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

%define qfs_name qfs

%define bin_dir /usr/bin
%define lib_dir /usr/lib/qfs
%define etc_dir %{_sysconfdir}/qfs
%define include_dir /usr/include
%define data_dir /usr/share/qfs
%define var_dir /var
%define qfs_services chunkserver metaserver webui
%define __python /usr/bin/python2

%if %{?!HADOOP_HOME:1}0
%global HADOOP_HOME /usr/lib/hadoop
%endif

# Required for init.d scripts
%global initd_dir %{_sysconfdir}/init.d
%if  %{?suse_version:1}0
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d
%else
Requires: /lib/lsb/init-functions
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

Name: qfs
Summary: Quantcast Filesystem (QFS) Meta Package
Version: %{qfs_version}
Release: %{qfs_release}
URL: https://quantcast.github.io/qfs
Group: Development/Libraries
License: ASL 2.0

BuildRequires: boost-devel >= 1.3.4
BuildRequires: cmake >= 2.4.7
BuildRequires: fuse-devel
BuildRequires: gcc-c++
BuildRequires: krb5-devel
BuildRequires: libuuid-devel
BuildRequires: net-tools
BuildRequires: openssl-devel
BuildRequires: xfsprogs-devel

Requires: boost >= 1.3.4
Requires: bigtop-utils
Requires: openssl
Requires: qfs-chunkserver
Requires: qfs-devel
Requires: qfs-fuse
Requires: qfs-hadoop
Requires: qfs-java
Requires: qfs-metaserver
Requires: qfs-python
Requires: qfs-webui

Source0: %{qfs_name}-%{qfs_base_version}.tar.gz
#BIGTOP_PATCH_FILES

%description
Quantcast File System (QFS) is a high-performance, fault-tolerant, distributed
file system developed to support MapReduce processing, or other applications
reading and writing large files sequentially.

%package client
Group: Applications/System
Summary: Client binaries and libraries used to link against qfs

%description client
This package provides base client binaries and libraries used to interact with a
running qfs cluster and link against qfs APIs to build your own software that
interacts with a running qfs cluster.

%package chunkserver
Group: System Environment/Daemons
Summary: Executables required to run the Quantcast File System chunkserver

%description chunkserver
The QFS chunkserver service hosts the binary contents of the QFS distributed
filesystem.  A metaserver coordinates many data nodes running this service,
replicating data amongst chunkservers in a redundant fashion.


%package devel
Group: Development/Libraries
Summary: Files needed for building Quantcast File System-based applications

%description devel
The QFS devel package contains the headers, static libraries, and developer
tool binaries required to develop applications which build against QFS.


%package fuse
Group: Applications/System
Summary: Support for mounting the Quantcast File System under FUSE

%description fuse
This package contains the qfs_fuse executable which is required when mounting
QFS distributed filesystems under FUSE.


%package hadoop
Group: Development/Libraries
Summary: Quantcast File System plugin JAR for Hadoop

%description hadoop
This package contains a plugin JAR to enable QFS to serve as a drop-in
replacement for HDFS under Hadoop.


%package java
Group: Development/Libraries
Summary: Java libraries for accessing the Quantcast File System

%description java
This package contains a JAR which enables Java applications to access QFS via
its JNI interface.


%package metaserver
Group: System Environment/Daemons
Summary: Executables required to run the Quantcast File System metaserver

%description metaserver
This package contains the executables required to run the Quantcast File System
metaserver service, which tracks the location of data chunks distributed across
QFS chunkservers.


%package python
Group: Development/Libraries
Summary: Python libraries for accessing the Quantcast File System

%description python
This package contains the libraries required to access the Quantcast File
System libraries via Python.


%package webui
Group: System Environment/Daemons
Requires: python2
Summary: Quantcast File System metaserver/chunkserver web frontend

%description webui
This package contains several Python scripts which provide a simple Hadoop-like
Web UI for viewing Quantcast File Server chunkserver and metaserver status.

%prep
%setup -n %{qfs_name}-%{qfs_base_version}

echo $RPM_SOURCE_DIR
echo $RPM_BUILD_ROOT

#BIGTOP_PATCH_COMMANDS
%build
%if 0%{?with_python3}
PYTHON3_PATH=%{__python3}
%endif # with_python3

bash $RPM_SOURCE_DIR/do-component-build \
    --qfs-version=%{qfs_version} \
    --python=%{__python} \
    --python3=$PYTHON3_PATH

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT

sh $RPM_SOURCE_DIR/install_qfs.sh \
    --source-dir=$RPM_SOURCE_DIR \
    --prefix=$RPM_BUILD_ROOT \
    --qfs-version=%{qfs_version} \
    --python=%{__python} \
    --python3=$PYTHON3_PATH \
    --bin-dir=%{bin_dir} \
    --lib-dir=%{lib_dir} \
    --etc-dir=%{etc_dir} \
    --include-dir=%{include_dir} \
    --data-dir=%{data_dir} \
    --var-dir=%{var_dir}

for service in %{qfs_services}
do
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{qfs_name}-${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/%{qfs_name}-${service}.svc rpm $init_file
done

# NOTE(fsareshwala): %pre sections copy pasted :(
%pre chunkserver
getent group qfs >/dev/null || groupadd -r qfs
getent passwd qfs >/dev/null || useradd -c "qfs" -s /sbin/nologin -g qfs -r \
           -d %{var_dir}/lib/qfs qfs 2> /dev/null || :

%pre metaserver
getent group qfs >/dev/null || groupadd -r qfs
getent passwd qfs >/dev/null || useradd -c "qfs" -s /sbin/nologin -g qfs -r \
           -d %{var_dir}/lib/qfs qfs 2> /dev/null || :

%pre webui
getent group qfs >/dev/null || groupadd -r qfs
getent passwd qfs >/dev/null || useradd -c "qfs" -s /sbin/nologin -g qfs -r \
           -d %{var_dir}/lib/qfs qfs 2> /dev/null || :

%post chunkserver
/sbin/chkconfig --add qfs-chunkserver

%preun chunkserver
if [ $1 -eq 0 ]; then
    /sbin/service qfs-chunkserver stop &>/dev/null || :
    /sbin/chkconfig --del qfs-chunkserver
fi

%post metaserver
/sbin/chkconfig --add qfs-metaserver

%preun metaserver
if [ $1 -eq 0 ]; then
    /sbin/service qfs-metaserver stop &>/dev/null || :
    /sbin/chkconfig --del qfs-metaserver
fi

%post webui
/sbin/chkconfig --add qfs-webui

%preun webui
if [ $1 -eq 0 ]; then
    /sbin/service qfs-webui stop &>/dev/null || :
    /sbin/chkconfig --del qfs-webui
fi

%clean
rm -rf $RPM_BUILD_ROOT

%files client
%defattr(-,root,root,755)
%{bin_dir}/qfs
%{bin_dir}/qfsadmin
%{bin_dir}/qfscat
%{bin_dir}/qfsdataverify
%{bin_dir}/qfsfileenum
%{bin_dir}/qfsfsck
%{bin_dir}/qfshibernate
%{bin_dir}/qfsping
%{bin_dir}/qfsput
%{bin_dir}/qfsshell
%{bin_dir}/qfsstats
%{bin_dir}/qfstoggleworm
%{bin_dir}/qfssample
%{bin_dir}/qfs_backup
%{bin_dir}/cpfromqfs
%{bin_dir}/cptoqfs

%{lib_dir}/libgf_complete.so
%{lib_dir}/libgf_complete.so.1
%{lib_dir}/libgf_complete.so.1.0.0
%{lib_dir}/libJerasure.so
%{lib_dir}/libJerasure.so.2
%{lib_dir}/libJerasure.so.2.0.0
%{lib_dir}/libqfs_access.so
%{lib_dir}/libqfs_client.so
%{lib_dir}/libqfs_common.so
%{lib_dir}/libqfsc.so
%{lib_dir}/libqfs_io.so
%{lib_dir}/libqfskrb.so
%{lib_dir}/libqfs_qcdio.so
%{lib_dir}/libqfs_qcrs.so

%files metaserver
%defattr(-,root,root,-)
%{bin_dir}/metaserver
%{bin_dir}/filelister
%{bin_dir}/logcompactor
%attr(0755,qfs,qfs) %{var_dir}/log/qfs
%attr(0755,qfs,qfs) %{var_dir}/lib/qfs
%{initd_dir}/qfs-metaserver
%config(noreplace) %{etc_dir}/logrotate.d/qfs-metaserver

%files chunkserver
%defattr(-,root,root,-)
%{bin_dir}/chunkserver
%{bin_dir}/chunkscrubber
%attr(0755,qfs,qfs) %{var_dir}/log/qfs
%attr(0755,qfs,qfs) %{var_dir}/lib/qfs
%{initd_dir}/qfs-chunkserver
%config(noreplace) %{etc_dir}/logrotate.d/qfs-chunkserver

%files devel
%defattr(-,root,root,-)
%{bin_dir}/checksum
%{bin_dir}/dirtree_creator
%{bin_dir}/dtokentest
%{bin_dir}/qfslogger
%{bin_dir}/rand-sfmt
%{bin_dir}/rebalanceexecutor
%{bin_dir}/rebalanceplanner
%{bin_dir}/replicachecker
%{bin_dir}/requestparser
%{bin_dir}/sortedhash
%{bin_dir}/sslfiltertest
%{bin_dir}/stlset

%{include_dir}/**

%{lib_dir}/libJerasure.a
%{lib_dir}/libgf_complete.a
%{lib_dir}/libqfs_client.a
%{lib_dir}/libqfs_common.a
%{lib_dir}/libqfs_emulator.a
%{lib_dir}/libqfs_io.a
%{lib_dir}/libqfs_meta.a
%{lib_dir}/libqfs_qcdio.a
%{lib_dir}/libqfs_qcrs.a
%{lib_dir}/libqfs_tools.a
%{lib_dir}/libqfsc.a
%{lib_dir}/libqfskrb.a

%files fuse
%defattr(-,root,root,-)
%{bin_dir}/qfs_fuse

%files hadoop
%defattr(-,root,root,-)
%{HADOOP_HOME}/lib/hadoop-%{hadoop_version}-qfs-%{qfs_version}.jar

%files java
%defattr(-,root,root,-)
%{data_dir}/java/qfs-access-%{qfs_version}.jar

%files python
%defattr(-,root,root,-)
%{python_sitearch}/**

%files webui
%defattr(-,root,root,-)
%{data_dir}/webui
%attr(0755,qfs,qfs) %{var_dir}/log/qfs
%attr(0755,qfs,qfs) %{var_dir}/lib/qfs
%{initd_dir}/qfs-webui
%config(noreplace) %{etc_dir}/logrotate.d/qfs-webui
