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
#
# Hadoop RPM spec file
#

# FIXME: we need to disable a more strict checks on native files for now,
# since Hadoop build system makes it difficult to pass the kind of flags
# that would make newer RPM debuginfo generation scripts happy.
%undefine _missing_build_ids_terminate_build
%undefine _auto_set_build_flags

%define hadoop_name hadoop
%define hadoop_pkg_name hadoop%{pkg_name_suffix}
%define zookeeper_pkg_name zookeeper%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_hadoop %{parent_dir}/usr/lib/%{hadoop_name}
%define usr_lib_hdfs %{parent_dir}/usr/lib/%{hadoop_name}-hdfs
%define usr_lib_yarn %{parent_dir}/usr/lib/%{hadoop_name}-yarn
%define usr_lib_mapreduce %{parent_dir}/usr/lib/%{hadoop_name}-mapreduce
%define var_lib_yarn %{parent_dir}/var/lib/%{hadoop_name}-yarn
%define var_lib_hdfs %{parent_dir}/var/lib/%{hadoop_name}-hdfs
%define var_lib_mapreduce %{parent_dir}/var/lib/%{hadoop_name}-mapreduce
%define var_lib_httpfs %{parent_dir}/var/lib/%{hadoop_name}-httpfs
%define var_lib_kms %{parent_dir}/var/lib/%{hadoop_name}-kms
%define etc_hadoop %{parent_dir}/etc/%{hadoop_name}

%define usr_lib_zookeeper %{parent_dir}/usr/lib/zookeeper

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}
%define include_dir %{parent_dir}/%{_includedir}
%define lib_dir %{parent_dir}/%{_libdir}
%define doc_hadoop %{doc_dir}/%{hadoop_name}-%{hadoop_version}

# No prefix directory
%define np_var_log_yarn /var/log/%{hadoop_name}-yarn
%define np_var_log_hdfs /var/log/%{hadoop_name}-hdfs
%define np_var_log_httpfs /var/log/%{hadoop_name}-httpfs
%define np_var_log_kms /var/log/%{hadoop_name}-kms
%define np_var_log_mapreduce /var/log/%{hadoop_name}-mapreduce
%define np_var_run_yarn /var/run/%{hadoop_name}-yarn
%define np_var_run_hdfs /var/run/%{hadoop_name}-hdfs
%define np_var_run_httpfs /var/run/%{hadoop_name}-httpfs
%define np_var_run_kms /var/run/%{hadoop_name}-kms
%define np_var_run_mapreduce /var/run/%{hadoop_name}-mapreduce
%define np_etc_hadoop /etc/%{hadoop_name}

%define httpfs_services httpfs
%define kms_services kms
%define mapreduce_services mapreduce-historyserver
%define hdfs_services hdfs-namenode hdfs-secondarynamenode hdfs-datanode hdfs-zkfc hdfs-journalnode hdfs-dfsrouter
%define yarn_services yarn-resourcemanager yarn-nodemanager yarn-proxyserver yarn-timelineserver yarn-router
%define hadoop_services %{hdfs_services} %{mapreduce_services} %{yarn_services} %{httpfs_services} %{kms_services}
# Hadoop outputs built binaries into %{hadoop_build}
%define hadoop_build_path build
%define static_images_dir src/webapps/static/images

%ifarch i386
%global hadoop_arch Linux-i386-32
%endif
%ifarch amd64 x86_64
%global hadoop_arch Linux-amd64-64
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

# FIXME: brp-repack-jars uses unzip to expand jar files
# Unfortunately aspectjtools-1.6.5.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See BIGTOP-294
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define netcat_package nc
%define doc_hadoop %{doc_dir}/%{hadoop_name}-%{hadoop_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif


%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# Deactivating symlinks checks
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%define netcat_package netcat-openbsd
%define doc_hadoop %{doc_dir}/%{hadoop_name}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d
%endif

%if  0%{?mgaversion}
%define netcat_package netcat-openbsd
%define doc_hadoop %{doc_dir}/%{hadoop_name}-%{hadoop_version}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif


# Even though we split the RPM into arch and noarch, it still will build and install
# the entirety of hadoop. Defining this tells RPM not to fail the build
# when it notices that we didn't package most of the installed files.
%define _unpackaged_files_terminate_build 0

# RPM searches perl files for dependancies and this breaks for non packaged perl lib
# like thrift so disable this
%define _use_internal_dependency_generator 0

# BIGTOP-3359
%define _build_id_links none

Name: %{hadoop_pkg_name}
Version: %{hadoop_version}
Release: %{hadoop_release}
Summary: Hadoop is a software platform for processing vast amounts of data
License: ASL 2.0
URL: http://hadoop.apache.org/core/
Group: Development/Libraries
Source0: %{hadoop_name}-%{hadoop_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{hadoop_name}.sh
Source4: hadoop-fuse.default
Source5: httpfs.default
Source6: hadoop.1
Source7: hadoop-fuse-dfs.1
Source8: hdfs.conf
Source9: yarn.conf
Source10: mapreduce.conf
Source11: init.d.tmpl
Source12: hadoop-hdfs-namenode.svc
Source13: hadoop-hdfs-datanode.svc
Source14: hadoop-hdfs-secondarynamenode.svc
Source15: hadoop-mapreduce-historyserver.svc
Source16: hadoop-yarn-resourcemanager.svc
Source17: hadoop-yarn-nodemanager.svc
Source18: hadoop-httpfs.svc
Source19: mapreduce.default
Source20: hdfs.default
Source21: yarn.default
Source22: hadoop-layout.sh
Source23: hadoop-hdfs-zkfc.svc
Source24: hadoop-hdfs-journalnode.svc
Source26: yarn.1
Source27: hdfs.1
Source28: mapred.1
Source29: hadoop-yarn-timelineserver.svc
Source30: hadoop-kms.svc
Source31: kms.default
#BIGTOP_PATCH_FILES
Buildroot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id} -u -n)
BuildRequires: fuse-devel, fuse
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service, bigtop-utils >= 0.7, %{zookeeper_pkg_name} >= 3.4.0
Requires: psmisc, %{netcat_package}
Requires: openssl-devel
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no

%if  %{?suse_version:1}0
BuildRequires: pkg-config, libfuse2, libopenssl-devel, gcc-c++
# Required for init scripts
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
%if 0%{?openEuler}
BuildRequires: pkgconfig, fuse-libs, openEuler-rpm-config, lzo-devel, openssl-devel
%else
BuildRequires: pkgconfig, fuse-libs, redhat-rpm-config, lzo-devel, openssl-devel
%endif
# Required for init scripts
Requires: coreutils, /lib/lsb/init-functions
%endif

%if  0%{?mgaversion}
BuildRequires: pkgconfig, libfuse-devel, libfuse2 , libopenssl-devel, gcc-c++, liblzo-devel, zlib-devel
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif

# Fedora 35: we need initscripts for /etc/init.d/functions and
# initscripts-service for /sbin/service.
%if %{?fc35}0
Requires: initscripts, initscripts-service
%endif

%description
Hadoop is a software platform that lets one easily write and
run applications that process vast amounts of data.

Here's what makes Hadoop especially useful:
* Scalable: Hadoop can reliably store and process petabytes.
* Economical: It distributes the data and processing across clusters
              of commonly available computers. These clusters can number
              into the thousands of nodes.
* Efficient: By distributing the data, Hadoop can process it in parallel
             on the nodes where the data is located. This makes it
             extremely rapid.
* Reliable: Hadoop automatically maintains multiple copies of data and
            automatically redeploys computing tasks based on failures.

Hadoop implements MapReduce, using the Hadoop Distributed File System (HDFS).
MapReduce divides applications into many small blocks of work. HDFS creates
multiple replicas of data blocks for reliability, placing them on compute
nodes around the cluster. MapReduce can then process the data where it is
located.

%package hdfs
Summary: The Hadoop Distributed File System
Group: System/Daemons
Requires: %{name} = %{version}-%{release}, bigtop-groovy, bigtop-jsvc

%description hdfs
Hadoop Distributed File System (HDFS) is the primary storage system used by
Hadoop applications. HDFS creates multiple replicas of data blocks and distributes
them on compute nodes throughout a cluster to enable reliable, extremely rapid
computations.

%package yarn
Summary: The Hadoop NextGen MapReduce (YARN)
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description yarn
YARN (Hadoop NextGen MapReduce) is a general purpose data-computation framework.
The fundamental idea of YARN is to split up the two major functionalities of the
JobTracker, resource management and job scheduling/monitoring, into separate daemons:
ResourceManager and NodeManager.

The ResourceManager is the ultimate authority that arbitrates resources among all
the applications in the system. The NodeManager is a per-node slave managing allocation
of computational resources on a single node. Both work in support of per-application
ApplicationMaster (AM).

An ApplicationMaster is, in effect, a framework specific library and is tasked with
negotiating resources from the ResourceManager and working with the NodeManager(s) to
execute and monitor the tasks.


%package mapreduce
Summary: The Hadoop MapReduce (MRv2)
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}

%description mapreduce
Hadoop MapReduce is a programming model and software framework for writing applications
that rapidly process vast amounts of data in parallel on large clusters of compute nodes.


%package hdfs-namenode
Summary: The Hadoop namenode manages the block locations of HDFS files
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description hdfs-namenode
The Hadoop Distributed Filesystem (HDFS) requires one unique server, the
namenode, which manages the block locations of files on the filesystem.


%package hdfs-secondarynamenode
Summary: Hadoop Secondary namenode
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description hdfs-secondarynamenode
The Secondary Name Node periodically compacts the Name Node EditLog
into a checkpoint.  This compaction ensures that Name Node restarts
do not incur unnecessary downtime.

%package hdfs-zkfc
Summary: Hadoop HDFS failover controller
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description hdfs-zkfc
The Hadoop HDFS failover controller is a ZooKeeper client which also
monitors and manages the state of the NameNode. Each of the machines
which runs a NameNode also runs a ZKFC, and that ZKFC is responsible
for: Health monitoring, ZooKeeper session management, ZooKeeper-based
election.

%package hdfs-journalnode
Summary: Hadoop HDFS JournalNode
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description hdfs-journalnode
The HDFS JournalNode is responsible for persisting NameNode edit logs.
In a typical deployment the JournalNode daemon runs on at least three
separate machines in the cluster.

%package hdfs-datanode
Summary: Hadoop Data Node
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description hdfs-datanode
The Data Nodes in the Hadoop Cluster are responsible for serving up
blocks of data over the network to Hadoop Distributed Filesystem
(HDFS) clients.

%package hdfs-dfsrouter
Summary: HDFS Router Server
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description hdfs-dfsrouter
HDFS Router Server which supports Router Based Federation.

%package httpfs
Summary: HTTPFS for Hadoop
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-hdfs = %{version}-%{release}

%description httpfs
The server providing HTTP REST API support for the complete FileSystem/FileContext
interface in HDFS.

%package kms
Summary: KMS for Hadoop
Group: System/Daemons
Requires: %{name}-client = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description kms
Cryptographic Key Management Server based on Hadoop KeyProvider API.

%package yarn-resourcemanager
Summary: YARN Resource Manager
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-yarn = %{version}-%{release}

%description yarn-resourcemanager
The resource manager manages the global assignment of compute resources to applications

%package yarn-nodemanager
Summary: YARN Node Manager
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-yarn = %{version}-%{release}

%description yarn-nodemanager
The NodeManager is the per-machine framework agent who is responsible for
containers, monitoring their resource usage (cpu, memory, disk, network) and
reporting the same to the ResourceManager/Scheduler.

%package yarn-proxyserver
Summary: YARN Web Proxy
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-yarn = %{version}-%{release}

%description yarn-proxyserver
The web proxy server sits in front of the YARN application master web UI.

%package yarn-timelineserver
Summary: YARN Timeline Server
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-yarn = %{version}-%{release}

%description yarn-timelineserver
Storage and retrieval of applications' current as well as historic information in a generic fashion is solved in YARN through the Timeline Server.

%package mapreduce-historyserver
Summary: MapReduce History Server
Group: System/Daemons
Requires: %{name}-mapreduce = %{version}-%{release}
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-mapreduce = %{version}-%{release}

%package yarn-router
Summary: YARN Router Server
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}
Requires(pre): %{name}-yarn = %{version}-%{release}

%description yarn-router
YARN Router Server which supports YARN Federation.


%description mapreduce-historyserver
The History server keeps records of the different activities being performed on a Apache Hadoop cluster

%package client
Summary: Hadoop client side dependencies
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires: %{name}-hdfs = %{version}-%{release}
Requires: %{name}-yarn = %{version}-%{release}
Requires: %{name}-mapreduce = %{version}-%{release}

%description client
Installation of this package will provide you with all the dependencies for Hadoop clients.

%package conf-pseudo
Summary: Pseudo-distributed Hadoop configuration
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires: %{name}-hdfs-namenode = %{version}-%{release}
Requires: %{name}-hdfs-datanode = %{version}-%{release}
Requires: %{name}-hdfs-secondarynamenode = %{version}-%{release}
Requires: %{name}-yarn-resourcemanager = %{version}-%{release}
Requires: %{name}-yarn-nodemanager = %{version}-%{release}
Requires: %{name}-mapreduce-historyserver = %{version}-%{release}

%description conf-pseudo
Contains configuration files for a "pseudo-distributed" Hadoop deployment.
In this mode, each of the hadoop components runs as a separate Java process,
but all on the same machine.

%package doc
Summary: Hadoop Documentation
Group: Documentation
%description doc
Documentation for Hadoop

%package libhdfs
Summary: Hadoop Filesystem Library
Group: Development/Libraries
Requires: %{name}-hdfs = %{version}-%{release}
# TODO: reconcile libjvm
AutoReq: no

%description libhdfs
Hadoop Filesystem Library

%package libhdfs-devel
Summary: Development support for libhdfs
Group: Development/Libraries
Requires: %{name} = %{version}-%{release}, %{name}-libhdfs = %{version}-%{release}

%description libhdfs-devel
Includes examples and header files for accessing HDFS from C

%package libhdfspp
Summary: Hadoop Filesystem Library for C++
Group: Development/Libraries

%description libhdfs-devel
Includes examples and header files for accessing HDFS from C

%package libhdfspp-devel
Summary: Development support for libhdfspp
Group: Development/Libraries
Requires: %{name}-libhdfspp = %{version}-%{release}

%description libhdfspp
Hadoop Filesystem Library for C++

%description libhdfspp-devel
Includes header files for accessing HDFS from C++

%package hdfs-fuse
Summary: Mountable HDFS
Group: Development/Libraries
Requires: %{name} = %{version}-%{release}
Requires: %{name}-libhdfs = %{version}-%{release}
Requires: %{name}-client = %{version}-%{release}
Requires: fuse
AutoReq: no

%if %{?suse_version:1}0
Requires: libfuse2
%else
Requires: fuse-libs
%endif


%description hdfs-fuse
These projects (enumerated below) allow HDFS to be mounted (on most flavors of Unix) as a standard file system using


%prep
%setup -n %{hadoop_name}-%{hadoop_base_version}-src

#BIGTOP_PATCH_COMMANDS
%build
# This assumes that you installed Java JDK 6 and set JAVA_HOME
# This assumes that you installed Forrest and set FORREST_HOME

env \
  HADOOP_VERSION=%{hadoop_base_version} \
  HADOOP_ARCH=%{hadoop_arch} \
  DO_MAVEN_DEPLOY=%{?do_maven_deploy} \
  MAVEN_DEPLOY_SOURCE=%{?maven_deploy_source} \
  MAVEN_REPO_ID=%{?maven_repo_id} \
  MAVEN_REPO_URI=%{?maven_repo_uri} \
bash %{SOURCE1}

%clean
%__rm -rf $RPM_BUILD_ROOT

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/%{usr_lib_hadoop}

env HADOOP_VERSION=%{hadoop_base_version} bash %{SOURCE2} \
  --distro-dir=$RPM_SOURCE_DIR \
  --build-dir=$PWD/build \
  --prefix=$RPM_BUILD_ROOT \
  --doc-dir=%{doc_hadoop} \
  --bin-dir=%{bin_dir} \
  --man-dir=%{man_dir} \
  --etc-default=%{etc_default} \
  --hadoop-dir=%{usr_lib_hadoop} \
  --hdfs-dir=%{usr_lib_hdfs} \
  --yarn-dir=%{usr_lib_yarn} \
  --mapreduce-dir=%{usr_lib_mapreduce} \
  --var-hdfs=%{var_lib_hdfs} \
  --var-yarn=%{var_lib_yarn} \
  --var-mapreduce=%{var_lib_mapreduce} \
  --var-httpfs=%{var_lib_httpfs} \
  --var-kms=%{var_lib_kms} \
  --system-include-dir=%{include_dir} \
  --system-lib-dir=%{lib_dir} \
  --etc-hadoop=%{etc_hadoop}

# Forcing Zookeeper dependency to be on the packaged jar
%__ln_s -f %{usr_lib_zookeeper}/zookeeper.jar $RPM_BUILD_ROOT/%{usr_lib_hadoop}/lib/zookeeper-[[:digit:]]*.jar
# Workaround for BIGTOP-583
%__rm -f $RPM_BUILD_ROOT/%{usr_lib_hadoop}-*/lib/slf4j-log4j12-*.jar

# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

# Install top level /etc/default files
# %__install -d -m 0755 $RPM_BUILD_ROOT/%{etc_default}
%__cp $RPM_SOURCE_DIR/%{hadoop_name}-fuse.default $RPM_BUILD_ROOT/%{etc_default}/%{hadoop_name}-fuse

# Generate the init.d scripts
for service in %{hadoop_services}
do
       bash %{SOURCE11} $RPM_SOURCE_DIR/%{hadoop_name}-${service}.svc rpm $RPM_BUILD_ROOT/%{initd_dir}/%{hadoop_name}-${service}
       cp $RPM_SOURCE_DIR/${service/-*/}.default $RPM_BUILD_ROOT/%{etc_default}/%{hadoop_name}-${service}
       chmod 644 $RPM_BUILD_ROOT/%{etc_default}/%{hadoop_name}-${service}
done

# Install security limits
%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE8} $RPM_BUILD_ROOT/etc/security/limits.d/hdfs.conf
%__install -m 0644 %{SOURCE9} $RPM_BUILD_ROOT/etc/security/limits.d/yarn.conf
%__install -m 0644 %{SOURCE10} $RPM_BUILD_ROOT/etc/security/limits.d/mapreduce.conf

# Install fuse default file
%__install -d -m 0755 $RPM_BUILD_ROOT/%{etc_default}
%__cp %{SOURCE4} $RPM_BUILD_ROOT/%{etc_default}/hadoop-fuse

# /var/lib/*/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{var_lib_yarn}/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{var_lib_hdfs}/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{var_lib_mapreduce}/cache
# /var/log/*
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_yarn}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_hdfs}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_mapreduce}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_httpfs}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_kms}
# /var/run/*
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_yarn}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_hdfs}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_mapreduce}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_httpfs}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_kms}

%__install -d -m 1777 $RPM_BUILD_ROOT/%{usr_lib_hadoop}/logs

%pre
getent group hadoop >/dev/null || groupadd -r hadoop

%pre hdfs
getent group hdfs >/dev/null  || groupadd -r hdfs
getent passwd hdfs >/dev/null || /usr/sbin/useradd --comment "Hadoop HDFS" --shell /bin/bash -M -r -g hdfs -G hadoop --home %{var_lib_hdfs} hdfs

%pre httpfs
getent group httpfs >/dev/null  || groupadd -r httpfs
getent passwd httpfs >/dev/null || /usr/sbin/useradd --comment "Hadoop HTTPFS" --shell /bin/bash -M -r -g httpfs -G httpfs --home %{var_lib_httpfs} httpfs

%pre kms
getent group kms >/dev/null  || groupadd -r kms
getent passwd kms >/dev/null || /usr/sbin/useradd --comment "Hadoop KMS" --shell /bin/bash -M -r -g kms -G kms --home %{var_lib_kms} kms

%pre yarn
getent group yarn >/dev/null  || groupadd -r yarn
getent passwd yarn >/dev/null || /usr/sbin/useradd --comment "Hadoop Yarn" --shell /bin/bash -M -r -g yarn -G hadoop --home %{var_lib_yarn} yarn

%pre mapreduce
getent group mapred >/dev/null  || groupadd -r mapred
getent passwd mapred >/dev/null || /usr/sbin/useradd --comment "Hadoop MapReduce" --shell /bin/bash -M -r -g mapred -G hadoop --home %{var_lib_mapreduce} mapred

%post
%{alternatives_cmd} --install %{np_etc_hadoop}/conf %{hadoop_name}-conf %{etc_hadoop}/conf.empty 10

%post httpfs
chkconfig --add %{hadoop_name}-httpfs

%post kms
chkconfig --add %{hadoop_name}-kms

%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{hadoop_name}-conf %{etc_hadoop}/conf.empty || :
fi

%preun httpfs
if [ $1 = 0 ]; then
  service %{hadoop_name}-httpfs stop > /dev/null 2>&1
  chkconfig --del %{hadoop_name}-httpfs
fi

%postun httpfs
if [ $1 -ge 1 ]; then
  service %{hadoop_name}-httpfs condrestart >/dev/null 2>&1
fi

%preun kms
if [ $1 = 0 ]; then
  service %{hadoop_name}-kms stop > /dev/null 2>&1
  chkconfig --del %{hadoop_name}-kms
fi

%postun kms
if [ $1 -ge 1 ]; then
  service %{hadoop_name}-kms condrestart >/dev/null 2>&1
fi

%files yarn
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/yarn-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/yarn-site.xml
%config(noreplace) %{etc_hadoop}/conf.empty/capacity-scheduler.xml
%config(noreplace) %{etc_hadoop}/conf.empty/container-executor.cfg
%config(noreplace) /etc/security/limits.d/yarn.conf
%{usr_lib_hadoop}/libexec/yarn-config.sh
%{usr_lib_yarn}
%attr(4754,root,yarn) %{usr_lib_yarn}/bin/container-executor
%{bin_dir}/yarn
%attr(0775,yarn,hadoop) %{np_var_run_yarn}
%attr(0775,yarn,hadoop) %{np_var_log_yarn}
%attr(0755,yarn,hadoop) %{var_lib_yarn}
%attr(1777,yarn,hadoop) %{var_lib_yarn}/cache

%files hdfs
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/hdfs-site.xml
%config(noreplace) /etc/security/limits.d/hdfs.conf
%{usr_lib_hdfs}
%{usr_lib_hadoop}/libexec/hdfs-config.sh
%{bin_dir}/hdfs
%attr(0775,hdfs,hadoop) %{np_var_run_hdfs}
%attr(0775,hdfs,hadoop) %{np_var_log_hdfs}
%attr(0755,hdfs,hadoop) %{var_lib_hdfs}
%attr(1777,hdfs,hadoop) %{var_lib_hdfs}/cache
%{usr_lib_hadoop}/libexec/init-hdfs.sh
%{usr_lib_hadoop}/libexec/init-hcfs.json
%{usr_lib_hadoop}/libexec/init-hcfs.groovy

%files mapreduce
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/mapred-site.xml
%config(noreplace) %{etc_hadoop}/conf.empty/mapred-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/mapred-queues.xml.template
%config(noreplace) /etc/security/limits.d/mapreduce.conf
%{usr_lib_mapreduce}
%{usr_lib_hadoop}/libexec/mapred-config.sh
%{bin_dir}/mapred
%attr(0775,mapred,hadoop) %{np_var_run_mapreduce}
%attr(0775,mapred,hadoop) %{np_var_log_mapreduce}
%attr(0775,mapred,hadoop) %{var_lib_mapreduce}
%attr(1777,mapred,hadoop) %{var_lib_mapreduce}/cache


%files
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/core-site.xml
%config(noreplace) %{etc_hadoop}/conf.empty/hadoop-metrics2.properties
%config(noreplace) %{etc_hadoop}/conf.empty/log4j.properties
%config(noreplace) %{etc_hadoop}/conf.empty/workers
%config(noreplace) %{etc_hadoop}/conf.empty/ssl-client.xml.example
%config(noreplace) %{etc_hadoop}/conf.empty/ssl-server.xml.example
%config(noreplace) %{etc_hadoop}/conf.empty/configuration.xsl
%config(noreplace) %{etc_hadoop}/conf.empty/hadoop-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/hadoop-policy.xml
%config(noreplace) %{etc_default}/hadoop
%dir %{np_etc_hadoop}
/etc/bash_completion.d/hadoop
%{usr_lib_hadoop}/*.jar
%{usr_lib_hadoop}/lib
%{usr_lib_hadoop}/sbin
%{usr_lib_hadoop}/bin
%{usr_lib_hadoop}/etc
%{usr_lib_hadoop}/logs
%{usr_lib_hadoop}/tools
%{usr_lib_hadoop}/libexec/hadoop-config.sh
%{usr_lib_hadoop}/libexec/hadoop-layout.sh
%{usr_lib_hadoop}/libexec/hadoop-functions.sh
%{usr_lib_hadoop}/libexec/shellprofile.d
%{usr_lib_hadoop}/libexec/tools
%{bin_dir}/hadoop
%{man_dir}/man1/hadoop.1.*
%{man_dir}/man1/yarn.1.*
%{man_dir}/man1/hdfs.1.*
%{man_dir}/man1/mapred.1.*
%attr(1777,hdfs,hadoop) %{usr_lib_hadoop}/logs

# Shouldn't the following be moved to hadoop-hdfs?
%exclude %{usr_lib_hadoop}/bin/fuse_dfs

%files doc
%defattr(-,root,root)
%doc %{doc_hadoop}

%files httpfs
%defattr(-,root,root)

%config(noreplace) %{etc_default}/%{hadoop_name}-httpfs
%config(noreplace) %{etc_hadoop}/conf.empty/httpfs-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/httpfs-log4j.properties
%config(noreplace) %{etc_hadoop}/conf.empty/httpfs-site.xml
%{initd_dir}/%{hadoop_name}-httpfs
%attr(0775,httpfs,httpfs) %{np_var_run_httpfs}
%attr(0775,httpfs,httpfs) %{np_var_log_httpfs}
%attr(0775,httpfs,httpfs) %{var_lib_httpfs}

%files kms
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/kms-acls.xml
%config(noreplace) %{etc_hadoop}/conf.empty/kms-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/kms-log4j.properties
%config(noreplace) %{etc_hadoop}/conf.empty/kms-site.xml
%config(noreplace) %{etc_default}/%{hadoop_name}-kms
%{initd_dir}/%{hadoop_name}-kms
%attr(0775,kms,kms) %{np_var_run_kms}
%attr(0775,kms,kms) %{np_var_log_kms}
%attr(0775,kms,kms) %{var_lib_kms}

# Service file management RPMs
%define service_macro() \
%files %1 \
%defattr(-,root,root) \
%{initd_dir}/%{hadoop_name}-%1 \
%config(noreplace) %{etc_default}/%{hadoop_name}-%1 \
%post %1 \
chkconfig --add %{hadoop_name}-%1 \
\
%preun %1 \
if [ $1 = 0 ]; then \
  service %{hadoop_name}-%1 stop > /dev/null 2>&1 \
  chkconfig --del %{hadoop_name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
  service %{hadoop_name}-%1 condrestart >/dev/null 2>&1 \
fi

%service_macro hdfs-namenode
%service_macro hdfs-secondarynamenode
%service_macro hdfs-zkfc
%service_macro hdfs-journalnode
%service_macro hdfs-datanode
%service_macro hdfs-dfsrouter
%service_macro yarn-resourcemanager
%service_macro yarn-nodemanager
%service_macro yarn-proxyserver
%service_macro yarn-timelineserver
%service_macro yarn-router
%service_macro mapreduce-historyserver

# Pseudo-distributed Hadoop installation
%post conf-pseudo
%{alternatives_cmd} --install %{np_etc_hadoop}/conf %{hadoop_name}-conf %{etc_hadoop}/conf.pseudo 30

%preun conf-pseudo
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{hadoop_name}-conf %{etc_hadoop}/conf.pseudo
fi

%files conf-pseudo
%defattr(-,root,root)
%config(noreplace) %attr(755,root,root) %{etc_hadoop}/conf.pseudo

%files client
%defattr(-,root,root)
%{usr_lib_hadoop}/client

%files libhdfs
%defattr(-,root,root)
%{lib_dir}/libhdfs.*

%files libhdfs-devel
%{include_dir}/hdfs.h
#%doc %{doc_dir}/libhdfs-%{hadoop_version}

%files libhdfspp
%defattr(-,root,root)
%{lib_dir}/libhdfspp.*

%files libhdfspp-devel
%{include_dir}/hdfspp

%files hdfs-fuse
%defattr(-,root,root)
%attr(0644,root,root) %config(noreplace) %{etc_default}/hadoop-fuse
%attr(0755,root,root) %{usr_lib_hadoop}/bin/fuse_dfs
%attr(0755,root,root) %{bin_dir}/hadoop-fuse-dfs
