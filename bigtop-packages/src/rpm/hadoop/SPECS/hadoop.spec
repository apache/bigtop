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
%define config_hadoop %{etc_hadoop}/conf
%define config_yarn %{etc_yarn}/conf
%define lib_hadoop_dirname /usr/lib
%define lib_hadoop %{lib_hadoop_dirname}/%{name}
%define log_hadoop_dirname /var/log
%define log_hadoop %{log_hadoop_dirname}/%{name}
%define log_yarn %{log_hadoop_dirname}/yarn
%define bin_hadoop %{_bindir}
%define man_hadoop %{_mandir}
%define doc_hadoop %{_docdir}/%{name}-%{hadoop_version}
%define hadoop_services namenode secondarynamenode datanode
%define yarn_services resourcemanager nodemanager historyserver
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
Source4: hadoop-init.tmpl
Source5: hadoop-init.tmpl.suse
Source6: hadoop.1
Source7: hadoop-fuse-dfs.1
Source8: hadoop-fuse.default
Source9: hadoop.nofiles.conf
Source10: yarn-init.tmpl
Patch0: MAPREDUCE-3436_rev2.patch
Buildroot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id} -u -n)
BuildRequires: python >= 2.4, git, fuse-devel,fuse, automake, autoconf
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service, bigtop-utils
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


%package namenode
Summary: The Hadoop namenode manages the block locations of HDFS files
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description namenode
The Hadoop Distributed Filesystem (HDFS) requires one unique server, the
namenode, which manages the block locations of files on the filesystem.


%package secondarynamenode
Summary: Hadoop Secondary namenode
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description secondarynamenode
The Secondary Name Node periodically compacts the Name Node EditLog
into a checkpoint.  This compaction ensures that Name Node restarts
do not incur unnecessary downtime.


%package datanode
Summary: Hadoop Data Node
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description datanode
The Data Nodes in the Hadoop Cluster are responsible for serving up
blocks of data over the network to Hadoop Distributed Filesystem
(HDFS) clients.

%package resourcemanager
Summary: Yarn Resource Manager
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description resourcemanager
The resource manager manages the global assignment of compute resources to applications

%package nodemanager
Summary: Yarn Node Manager
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description nodemanager
The NodeManager is the per-machine framework agent who is responsible for
containers, monitoring their resource usage (cpu, memory, disk, network) and
reporting the same to the ResourceManager/Scheduler.

%package historyserver
Summary: Yarn History Server
Group: System/Daemons
Requires: %{name} = %{version}-%{release}

%description historyserver
The History server keeps records of the different activities being performed on a Apache Hadoop cluster

%package conf-pseudo
Summary: Hadoop installation in pseudo-distributed mode
Group: System/Daemons
Requires: %{name} = %{version}-%{release}, %{name}-namenode = %{version}-%{release}, %{name}-datanode = %{version}-%{release}, %{name}-secondarynamenode = %{version}-%{release}, %{name}-resourcemanager = %{version}-%{release}, %{name}-nodemanager = %{version}-%{release}, %{name}-historyserver = %{version}-%{release}

%description conf-pseudo
Installation of this RPM will setup your machine to run in pseudo-distributed mode
where each Hadoop daemon runs in a separate Java process.

%package doc
Summary: Hadoop Documentation
Group: Documentation
Obsoletes: %{name}-docs
%description doc
Documentation for Hadoop

%package libhdfs
Summary: Hadoop Filesystem Library
Group: Development/Libraries
Requires: %{name} = %{version}-%{release}
# TODO: reconcile libjvm
AutoReq: no

%description libhdfs
Hadoop Filesystem Library

%prep
%setup -n apache-hadoop-common-aa85a29
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
  --system-include-dir=$RPM_BUILD_ROOT%{_includedir} \
  --system-lib-dir=$RPM_BUILD_ROOT%{_libdir} \
  --system-libexec-dir=$RPM_BUILD_ROOT%{libexecdir} \
  --hadoop-etc-dir=$RPM_BUILD_ROOT%{etc_hadoop} \
  --prefix=$RPM_BUILD_ROOT \
  --doc-dir=$RPM_BUILD_ROOT%{doc_hadoop} \
  --example-dir=$RPM_BUILD_ROOT%{doc_hadoop}/examples \
  --native-build-string=%{hadoop_arch} \
  --installed-lib-dir=%{lib_hadoop} \
  --man-dir=$RPM_BUILD_ROOT%{man_hadoop} \

# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/


%if  %{?suse_version:1}0
orig_init_file=$RPM_SOURCE_DIR/hadoop-init.tmpl.suse
%else
orig_init_file=$RPM_SOURCE_DIR/hadoop-init.tmpl
%endif

yarn_orig_init_file=$RPM_SOURCE_DIR/yarn-init.tmpl

# Generate the init.d scripts
for service in %{hadoop_services}
do
       init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
       %__cp $orig_init_file $init_file
       %__sed -i -e 's|@HADOOP_COMMON_ROOT@|%{lib_hadoop}|' $init_file
       %__sed -i -e "s|@HADOOP_DAEMON@|${service}|" $init_file
       %__sed -i -e 's|@HADOOP_CONF_DIR@|%{config_hadoop}|' $init_file
       %__sed -i -e 's|@HADOOP_DAEMON_USER@|hdfs|' $init_file
       chmod 755 $init_file
done
for service in %{yarn_services}
do
       init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
       %__cp $yarn_orig_init_file $init_file
       %__sed -i -e 's|@YARN_COMMON_ROOT@|%{lib_hadoop}|' $init_file
       %__sed -i -e "s|@YARN_DAEMON@|${service}|" $init_file
       %__sed -i -e 's|@YARN_CONF_DIR@|%{config_hadoop}|' $init_file
       %__sed -i -e 's|@YARN_DAEMON_USER@|yarn|' $init_file
       chmod 755 $init_file
done


%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default
%__cp $RPM_SOURCE_DIR/hadoop.default $RPM_BUILD_ROOT/etc/default/hadoop
%__cp $RPM_SOURCE_DIR/yarn.default $RPM_BUILD_ROOT/etc/default/yarn
%__cp $RPM_SOURCE_DIR/hadoop-fuse.default $RPM_BUILD_ROOT/etc/default/hadoop-fuse

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE9} $RPM_BUILD_ROOT/etc/security/limits.d/hadoop.nofiles.conf

# /var/lib/hadoop/cache
%__install -d -m 1777 $RPM_BUILD_ROOT/var/lib/%{name}/cache/hadoop
# /var/log/hadoop
%__install -d -m 0755 $RPM_BUILD_ROOT/var/log
%__install -d -m 0775 $RPM_BUILD_ROOT/var/run/%{name}
%__install -d -m 0775 $RPM_BUILD_ROOT/var/run/yarn
%__install -d -m 0775 $RPM_BUILD_ROOT/%{log_hadoop}
%__install -d -m 0775 $RPM_BUILD_ROOT/%{log_yarn}


%pre
getent group hadoop >/dev/null || groupadd -r hadoop
getent group hdfs >/dev/null   || groupadd -r hdfs
getent group yarn >/dev/null   || groupadd -r yarn

getent passwd hdfs >/dev/null || /usr/sbin/useradd --comment "Hadoop HDFS" --shell /bin/bash -M -r -g hdfs -G hadoop --home %{lib_hadoop} hdfs
getent passwd yarn >/dev/null || /usr/sbin/useradd --comment "Hadoop Yarn" --shell /bin/bash -M -r -g yarn -G hadoop --home %{lib_hadoop} yarn

%post
%{alternatives_cmd} --install %{config_hadoop} %{name}-conf %{etc_hadoop}/conf.empty 10
#%{alternatives_cmd} --install %{config_yarn} yarn-conf %{etc_yarn}/conf.empty 10
%{alternatives_cmd} --install %{bin_hadoop}/%{hadoop_name} %{hadoop_name}-default %{bin_hadoop}/%{name} 20 \
  --slave %{log_hadoop_dirname}/%{hadoop_name} %{hadoop_name}-log %{log_hadoop} \
  --slave %{lib_hadoop_dirname}/%{hadoop_name} %{hadoop_name}-lib %{lib_hadoop} \
  --slave /etc/%{hadoop_name} %{hadoop_name}-etc %{etc_hadoop} \
  --slave %{man_hadoop}/man1/%{hadoop_name}.1.*z %{hadoop_name}-man %{man_hadoop}/man1/%{name}.1.*z

mkdir -p /var/lib/hadoop/cache/hadoop || :
chown hdfs:hadoop /var/lib/hadoop/cache/hadoop || :
chmod g+w /var/lib/hadoop/cache/hadoop/
mkdir -p /var/log/hadoop || :
touch /var/log/hadoop/SecurityAuth.audit
chgrp hadoop /var/log/hadoop/SecurityAuth.audit
chmod g+w /var/log/hadoop/SecurityAuth.audit

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

%files
%defattr(-,root,root)
%config(noreplace) %{etc_hadoop}/conf.empty
#%config(noreplace) %{etc_yarn}/conf.empty
%config(noreplace) /etc/default/hadoop
%config(noreplace) /etc/default/yarn
%config(noreplace) /etc/security/limits.d/hadoop.nofiles.conf
%{lib_hadoop}
%{libexecdir}/hadoop-config.sh
%{libexecdir}/hdfs-config.sh
%{libexecdir}/mapred-config.sh
%{libexecdir}/yarn-config.sh
%{bin_hadoop}/%{name}
%{bin_hadoop}/yarn
%{bin_hadoop}/hdfs
%{bin_hadoop}/mapred
%attr(0775,root,hadoop) /var/run/%{name}
%attr(0775,root,hadoop) %{log_hadoop}
%attr(0775,root,hadoop) /var/run/yarn
%attr(0775,root,hadoop) %{log_yarn}
%{man_hadoop}/man1/hadoop.1.*

%files doc
%defattr(-,root,root)
%doc %{doc_hadoop}


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

%service_macro namenode
%service_macro secondarynamenode
%service_macro datanode
%service_macro resourcemanager
%service_macro nodemanager
%service_macro historyserver

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
