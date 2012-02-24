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

%define hadoop_name hadoop
%define etc_hadoop /etc/%{name}
%define etc_yarn /etc/yarn
%define etc_httpfs /etc/%{name}-httpfs
%define config_hadoop %{etc_hadoop}/conf
%define config_yarn %{etc_yarn}/conf
%define config_httpfs %{etc_httpfs}/conf
%define lib_hadoop_dirname /usr/lib
%define lib_hadoop %{lib_hadoop_dirname}/%{name}
%define lib_httpfs %{lib_hadoop_dirname}/%{name}-httpfs
%define log_hadoop_dirname /var/log
%define log_hadoop %{log_hadoop_dirname}/%{name}
%define log_yarn %{log_hadoop_dirname}/yarn
%define log_hdfs %{log_hadoop_dirname}/hdfs
%define log_httpfs %{log_hadoop_dirname}/%{name}-httpfs
%define log_mapreduce %{log_hadoop_dirname}/mapreduce
%define run_hadoop_dirname /var/run
%define run_hadoop %{run_hadoop_dirname}/hadoop
%define run_yarn %{run_hadoop_dirname}/yarn
%define run_hdfs %{run_hadoop_dirname}/hdfs
%define run_httpfs %{run_hadoop_dirname}/%{name}-httpfs
%define run_mapreduce %{run_hadoop_dirname}/mapreduce
%define state_hadoop_dirname /var/lib
%define state_hadoop %{state_hadoop_dirname}/hadoop
%define state_yarn %{state_hadoop_dirname}/yarn
%define state_hdfs %{state_hadoop_dirname}/hdfs
%define state_mapreduce %{state_hadoop_dirname}/mapreduce
%define bin_hadoop %{_bindir}
%define man_hadoop %{_mandir}
%define doc_hadoop %{_docdir}/%{name}-%{hadoop_version}
%define httpfs_services httpfs
%define mapreduce_services mapreduce-historyserver
%define hdfs_services hdfs-namenode hdfs-secondarynamenode hdfs-datanode
%define yarn_services yarn-resourcemanager yarn-nodemanager yarn-proxyserver
%define hadoop_services %{hdfs_services} %{mapreduce_services} %{yarn_services} %{httpfs_services}
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
    /usr/lib/rpm/redhat/brp-compress ; \
    /usr/lib/rpm/redhat/brp-strip-static-archive %{__strip} ; \
    /usr/lib/rpm/redhat/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}


%define libexecdir %{_libexecdir}
%define doc_hadoop %{_docdir}/%{name}-%{hadoop_version}
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

%define libexecdir %{_libexecdir}
%define doc_hadoop %{_docdir}/%{name}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d
%endif

%if  0%{?mgaversion}
%define libexecdir /usr/libexec/
%define doc_hadoop %{_docdir}/%{name}-%{hadoop_version}
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

Name: %{hadoop_name}
Version: %{hadoop_version}
Release: %{hadoop_release}
Summary: Hadoop is a software platform for processing vast amounts of data
License: Apache License v2.0
URL: http://hadoop.apache.org/core/
Group: Development/Libraries
Source0: %{name}-%{hadoop_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: hadoop.default
Source4: hadoop-fuse.default
Source5: hadoop-httpfs.default
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
Source19: hadoop-mapreduce-historyserver.default
Patch0: MAPREDUCE-3890.patch
Buildroot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id} -u -n)
BuildRequires: python >= 2.4, git, fuse-devel,fuse, automake, autoconf
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service, bigtop-utils
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no
Provides: hadoop

%if  %{?suse_version:1}0
BuildRequires: libfuse2, libopenssl-devel, gcc-c++, ant, ant-nodeps, ant-trax
# Required for init scripts
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
BuildRequires: fuse-libs, libtool, redhat-rpm-config, lzo-devel, openssl-devel
# Required for init scripts
Requires: sh-utils, redhat-lsb
%endif

%if  0%{?mgaversion}
BuildRequires: libfuse-devel, libfuse2 , libopenssl-devel, gcc-c++, ant, libtool, automake, autoconf, liblzo-devel, zlib-devel
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
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
Requires: %{name} = %{version}-%{release}

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

%description hdfs-namenode
The Hadoop Distributed Filesystem (HDFS) requires one unique server, the
namenode, which manages the block locations of files on the filesystem.


%package hdfs-secondarynamenode
Summary: Hadoop Secondary namenode
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description hdfs-secondarynamenode
The Secondary Name Node periodically compacts the Name Node EditLog
into a checkpoint.  This compaction ensures that Name Node restarts
do not incur unnecessary downtime.


%package hdfs-datanode
Summary: Hadoop Data Node
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description hdfs-datanode
The Data Nodes in the Hadoop Cluster are responsible for serving up
blocks of data over the network to Hadoop Distributed Filesystem
(HDFS) clients.

%package httpfs
Summary: HTTPFS for Hadoop
Group: System/Daemons
Requires: %{name}-hdfs = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description httpfs
The server providing HTTP REST API support for the complete FileSystem/FileContext
interface in HDFS.

%package yarn-resourcemanager
Summary: Yarn Resource Manager
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description yarn-resourcemanager
The resource manager manages the global assignment of compute resources to applications

%package yarn-nodemanager
Summary: Yarn Node Manager
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description yarn-nodemanager
The NodeManager is the per-machine framework agent who is responsible for
containers, monitoring their resource usage (cpu, memory, disk, network) and
reporting the same to the ResourceManager/Scheduler.

%package yarn-proxyserver
Summary: Yarn Web Proxy
Group: System/Daemons
Requires: %{name}-yarn = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description yarn-proxyserver
The web proxy server sits in front of the YARN application master web UI.

%package mapreduce-historyserver
Summary: MapReduce History Server
Group: System/Daemons
Requires: %{name}-mapreduce = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description mapreduce-historyserver
The History server keeps records of the different activities being performed on a Apache Hadoop cluster

%package conf-pseudo
Summary: Hadoop installation in pseudo-distributed mode
Group: System/Daemons
Requires: %{name} = %{version}-%{release}, %{name}-hdfs-namenode = %{version}-%{release}, %{name}-hdfs-datanode = %{version}-%{release}, %{name}-hdfs-secondarynamenode = %{version}-%{release}, %{name}-yarn-resourcemanager = %{version}-%{release}, %{name}-yarn-nodemanager = %{version}-%{release}, %{name}-mapreduce-historyserver = %{version}-%{release}

%description conf-pseudo
Installation of this RPM will setup your machine to run in pseudo-distributed mode
where each Hadoop daemon runs in a separate Java process.

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

%prep
%setup -n %{name}-%{hadoop_base_version}-src 
%patch0 -p0

%build
# This assumes that you installed Java JDK 6 and set JAVA_HOME
# This assumes that you installed Java JDK 5 and set JAVA5_HOME
# This assumes that you installed Forrest and set FORREST_HOME

env HADOOP_VERSION=%{hadoop_base_version} HADOOP_ARCH=%{hadoop_arch} bash %{SOURCE1}

%clean
%__rm -rf $RPM_BUILD_ROOT

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/%{lib_hadoop}

bash %{SOURCE2} \
  --distro-dir=$RPM_SOURCE_DIR \
  --build-dir=$PWD/build \
  --httpfs-dir=$RPM_BUILD_ROOT%{lib_httpfs} \
  --system-include-dir=$RPM_BUILD_ROOT%{_includedir} \
  --system-lib-dir=$RPM_BUILD_ROOT%{_libdir} \
  --system-libexec-dir=$RPM_BUILD_ROOT/%{lib_hadoop}/libexec \
  --hadoop-etc-dir=$RPM_BUILD_ROOT%{etc_hadoop} \
  --httpfs-etc-dir=$RPM_BUILD_ROOT%{etc_httpfs} \
  --prefix=$RPM_BUILD_ROOT \
  --doc-dir=$RPM_BUILD_ROOT%{doc_hadoop} \
  --example-dir=$RPM_BUILD_ROOT%{doc_hadoop}/examples \
  --native-build-string=%{hadoop_arch} \
  --installed-lib-dir=%{lib_hadoop} \
  --man-dir=$RPM_BUILD_ROOT%{man_hadoop} \

# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

# Generate the init.d scripts
for service in %{hadoop_services}
do
       init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
       bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/%{name}-${service}.svc > $init_file
       chmod 755 $init_file
done

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default
%__cp $RPM_SOURCE_DIR/hadoop.default $RPM_BUILD_ROOT/etc/default/hadoop
%__cp $RPM_SOURCE_DIR/yarn.default $RPM_BUILD_ROOT/etc/default/yarn
%__cp $RPM_SOURCE_DIR/%{name}-fuse.default $RPM_BUILD_ROOT/etc/default/%{name}-fuse
%__cp $RPM_SOURCE_DIR/%{name}-httpfs.default $RPM_BUILD_ROOT/etc/default/%{name}-httpfs
%__cp $RPM_SOURCE_DIR/%{name}-mapreduce-historyserver.default $RPM_BUILD_ROOT/etc/default/%{name}-mapreduce-historyserver

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE8} $RPM_BUILD_ROOT/etc/security/limits.d/hdfs.conf
%__install -m 0644 %{SOURCE9} $RPM_BUILD_ROOT/etc/security/limits.d/yarn.conf
%__install -m 0644 %{SOURCE10} $RPM_BUILD_ROOT/etc/security/limits.d/mapreduce.conf

# /var/lib/*/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{state_hadoop}/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{state_yarn}/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{state_hdfs}/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/%{state_mapreduce}/cache
# /var/log/*
%__install -d -m 0775 $RPM_BUILD_ROOT/%{log_hadoop}
%__install -d -m 0775 $RPM_BUILD_ROOT/%{log_yarn}
# %__install -d -m 0775 $RPM_BUILD_ROOT/%{log_hdfs}
# %__install -d -m 0775 $RPM_BUILD_ROOT/%{log_mapreduce}
%__install -d -m 0775 $RPM_BUILD_ROOT/%{log_httpfs}
# /var/run/*
%__install -d -m 0775 $RPM_BUILD_ROOT/%{run_hadoop}
%__install -d -m 0775 $RPM_BUILD_ROOT/%{run_yarn}
#%__install -d -m 0775 $RPM_BUILD_ROOT/%{run_hdfs}
#%__install -d -m 0775 $RPM_BUILD_ROOT/%{run_mapreduce}
%__install -d -m 0775 $RPM_BUILD_ROOT/%{run_httpfs}

%pre
getent group hadoop >/dev/null || groupadd -r hadoop

%pre hdfs
getent group hdfs >/dev/null   || groupadd -r hdfs
getent passwd hdfs >/dev/null || /usr/sbin/useradd --comment "Hadoop HDFS" --shell /bin/bash -M -r -g hdfs -G hadoop --home %{state_hdfs} hdfs

%pre httpfs 
getent group httpfs >/dev/null   || groupadd -r httpfs
getent passwd httpfs >/dev/null || /usr/sbin/useradd --comment "Hadoop HTTPFS" --shell /bin/bash -M -r -g httpfs -G httpfs --home %{run_httpfs} httpfs

%pre yarn
getent group yarn >/dev/null   || groupadd -r yarn
getent passwd yarn >/dev/null || /usr/sbin/useradd --comment "Hadoop Yarn" --shell /bin/bash -M -r -g yarn -G hadoop --home %{state_yarn} yarn

%pre mapreduce
getent group mapreduce >/dev/null   || groupadd -r mapreduce
getent passwd mapreduce >/dev/null || /usr/sbin/useradd --comment "Hadoop MapReduce" --shell /bin/bash -M -r -g mapreduce -G hadoop --home %{state_mapreduce} mapreduce

%post
%{alternatives_cmd} --install %{config_hadoop} %{name}-conf %{etc_hadoop}/conf.empty 10
#%{alternatives_cmd} --install %{config_yarn} yarn-conf %{etc_yarn}/conf.empty 10
%{alternatives_cmd} --install %{bin_hadoop}/%{hadoop_name} %{hadoop_name}-default %{bin_hadoop}/%{name} 20 \
  --slave %{log_hadoop_dirname}/%{hadoop_name} %{hadoop_name}-log %{log_hadoop} \
  --slave %{lib_hadoop_dirname}/%{hadoop_name} %{hadoop_name}-lib %{lib_hadoop} \
  --slave /etc/%{hadoop_name} %{hadoop_name}-etc %{etc_hadoop} \
  --slave %{man_hadoop}/man1/%{hadoop_name}.1.*z %{hadoop_name}-man %{man_hadoop}/man1/%{name}.1.*z

touch %{log_hadoop}/SecurityAuth.audit
chgrp hadoop %{log_hadoop}/SecurityAuth.audit
chmod g+w %{log_hadoop}/SecurityAuth.audit

%post httpfs
%{alternatives_cmd} --install %{config_httpfs} %{name}-httpfs-conf %{etc_httpfs}/conf.empty 10
chkconfig --add %{name}-httpfs

%preun
if [ "$1" = 0 ]; then
  # Stop any services that might be running
  for service in %{hadoop_services}
  do
     service hadoop-$service stop 1>/dev/null 2>/dev/null || :
  done
  %{alternatives_cmd} --remove %{name}-conf %{etc_hadoop}/conf.empty || :
  %{alternatives_cmd} --remove %{hadoop_name}-default %{bin_hadoop}/%{name} || :
fi

%preun httpfs
%{alternatives_cmd} --remove %{name}-httpfs-conf %{etc_httpfs}/conf.empty || :
if [ $1 = 0 ]; then
  service %{name}-httpfs stop > /dev/null 2>&1
  chkconfig --del %{name}-httpfs
fi

%postun httpfs
if [ $1 -ge 1 ]; then
  service %{name}-httpfs condrestart >/dev/null 2>&1
fi


%files yarn
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/yarn-env.sh
%config(noreplace) %{etc_hadoop}/conf.empty/yarn-site.xml
%config(noreplace) %{etc_hadoop}/conf.empty/mrapp-generated-classpath
%config(noreplace) /etc/default/yarn
%config(noreplace) /etc/security/limits.d/yarn.conf
%{lib_hadoop}/hadoop-yarn*.jar
%{lib_hadoop}/libexec/yarn-config.sh
%{lib_hadoop}/sbin/start-yarn.sh
%{lib_hadoop}/sbin/stop-yarn.sh
%{lib_hadoop}/sbin/yarn-daemon.sh
%{lib_hadoop}/sbin/yarn-daemons.sh
%{lib_hadoop}/bin/yarn
%attr(6050,root,yarn) %{lib_hadoop}/bin/container-executor
%{bin_hadoop}/yarn
%attr(0775,yarn,hadoop) %{run_yarn}
%attr(0775,yarn,hadoop) %{log_yarn}
%attr(0775,yarn,hadoop) %{state_yarn}
%attr(1777,yarn,hadoop) %{state_yarn}/cache

%files hdfs
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/hdfs-site.xml
%config(noreplace) /etc/default/hadoop-fuse
%config(noreplace) /etc/security/limits.d/hdfs.conf
%{lib_hadoop}/hadoop-hdfs*.jar
%{lib_hadoop}/hadoop-archives*.jar
%{lib_hadoop}/libexec/hdfs-config.sh
%{lib_hadoop}/libexec/jsvc
%{lib_hadoop}/webapps
%{lib_hadoop}/sbin/update-hdfs-env.sh
%{lib_hadoop}/sbin/start-secure-dns.sh
%{lib_hadoop}/sbin/stop-secure-dns.sh
%{lib_hadoop}/sbin/start-balancer.sh
%{lib_hadoop}/sbin/stop-balancer.sh
%{lib_hadoop}/sbin/start-dfs.sh
%{lib_hadoop}/sbin/stop-dfs.sh
%{lib_hadoop}/sbin/refresh-namenodes.sh
%{lib_hadoop}/sbin/distribute-exclude.sh
%{lib_hadoop}/bin/hdfs
%{bin_hadoop}/hdfs
%attr(0775,hdfs,hadoop) %{run_hdfs}
%attr(0775,hdfs,hadoop) %{log_hdfs}
%attr(0775,hdfs,hadoop) %{state_hdfs}
%attr(1777,hdfs,hadoop) %{state_hdfs}/cache

%files mapreduce
%defattr(-,root,root)
%config(noreplace) /etc/default/hadoop-mapreduce-historyserver
%config(noreplace) /etc/security/limits.d/mapreduce.conf
%{lib_hadoop}/hadoop-mapreduce*.jar
%{lib_hadoop}/hadoop-streaming*.jar
%{lib_hadoop}/hadoop-extras*.jar
%{lib_hadoop}/hadoop-distcp*.jar
%{lib_hadoop}/hadoop-rumen*.jar
%{lib_hadoop}/libexec/mapred-config.sh
%{lib_hadoop}/bin/mapred
%{lib_hadoop}/sbin/mr-jobhistory-daemon.sh
%{bin_hadoop}/mapred
%attr(0775,mapreduce,hadoop) %{run_mapreduce}
%attr(0775,mapreduce,hadoop) %{log_mapreduce}
%attr(0775,mapreduce,hadoop) %{state_mapreduce}
%attr(1777,mapreduce,hadoop) %{state_mapreduce}/cache


%files
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty/core-site.xml
%config(noreplace) %{etc_hadoop}/conf.empty/hadoop-metrics.properties
%config(noreplace) %{etc_hadoop}/conf.empty/hadoop-metrics2.properties
%config(noreplace) %{etc_hadoop}/conf.empty/log4j.properties
%config(noreplace) %{etc_hadoop}/conf.empty/slaves
%config(noreplace) %{etc_hadoop}/conf.empty/ssl-client.xml.example
%config(noreplace) %{etc_hadoop}/conf.empty/ssl-server.xml.example
%config(noreplace) /etc/default/hadoop
%{lib_hadoop}/hadoop-common*.jar
%{lib_hadoop}/hadoop-auth*.jar
%{lib_hadoop}/hadoop-annotations*.jar
%{lib_hadoop}/lib
%{lib_hadoop}/etc
%{lib_hadoop}/libexec/hadoop-config.sh
%{lib_hadoop}/sbin/hadoop-*.sh
%{lib_hadoop}/sbin/update-hadoop-env.sh
%{lib_hadoop}/sbin/slaves.sh
%{lib_hadoop}/sbin/start-all.sh
%{lib_hadoop}/sbin/stop-all.sh
%{lib_hadoop}/bin/hadoop
%{lib_hadoop}/bin/rcc
%{bin_hadoop}/hadoop
%{man_hadoop}/man1/hadoop.1.*
%attr(0775,root,hadoop) %{run_hadoop}
%attr(0775,root,hadoop) %{log_hadoop}
%attr(0775,root,hadoop) %{state_hadoop}
%attr(1777,root,hadoop) %{state_hadoop}/cache

%files doc
%defattr(-,root,root)
%doc %{doc_hadoop}

%files httpfs
%defattr(-,root,root)
%config(noreplace) %{etc_httpfs}/conf.empty
%config(noreplace) /etc/default/%{name}-httpfs
%{initd_dir}/%{name}-httpfs
%{lib_httpfs}
%attr(0775,httpfs,httpfs) %{run_httpfs}
%attr(0775,httpfs,httpfs) %{log_httpfs}

# Service file management RPMs
%define service_macro() \
%files %1 \
%defattr(-,root,root) \
%{initd_dir}/%{name}-%1 \
%post %1 \
chkconfig --add %{name}-%1 \
\
%preun %1 \
if [ $1 = 0 ]; then \
  service %{name}-%1 stop > /dev/null 2>&1 \
  chkconfig --del %{name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
  service %{name}-%1 condrestart >/dev/null 2>&1 \
fi

%service_macro hdfs-namenode
%service_macro hdfs-secondarynamenode
%service_macro hdfs-datanode
%service_macro yarn-resourcemanager
%service_macro yarn-nodemanager
%service_macro yarn-proxyserver
%service_macro mapreduce-historyserver

# Pseudo-distributed Hadoop installation
%post conf-pseudo
%{alternatives_cmd} --install %{config_hadoop} %{name}-conf %{etc_hadoop}/conf.pseudo 30
#%{alternatives_cmd} --install %{config_yarn} yarn-conf %{etc_yarn}/conf.pseudo 30

%preun conf-pseudo
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{etc_hadoop}/conf.pseudo
        rm -f %{etc_hadoop}/conf
fi

%files conf-pseudo
%defattr(-,root,root)
%config(noreplace) %attr(755,root,root) %{etc_hadoop}/conf.pseudo
#%config(noreplace) %attr(755,root,root) %{etc_yarn}/conf.pseudo
%dir %attr(0755,root,hadoop) /var/lib/%{name}
%dir %attr(1777,root,hadoop) /var/lib/%{name}/cache

%files libhdfs
%defattr(-,root,root)
%{_libdir}/libhdfs*
%{_includedir}/hdfs.h
# -devel should be its own package
#%doc %{_docdir}/libhdfs-%{hadoop_version}
