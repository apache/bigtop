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

%define hama_name hama
%define lib_hama /usr/lib/%{hama_name}
%define etc_hama /etc/%{hama_name}
%define config_hama %{etc_hama}/conf
%define log_hama /var/log/%{hama_name}
%define bin_hama /usr/bin
%define man_dir /usr/share/man

%if  %{?suse_version:1}0
%define doc_hama %{_docdir}/hama
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d
%else
%define doc_hama %{_docdir}/hama-%{hama_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: hama
Version: %{hama_version}
Release: %{hama_release}
Summary: A set of Java libraries for scalable machine learning.
URL: http://hama.apache.org
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{name}-dist-%{hama_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{name}.sh
Source3: %{name}-bspmaster.init
Source4: %{name}-groom.init
Source5: %{name}.default
Source6: bigtop.bom
Requires: hadoop, bigtop-utils, zookeeper

# Disable automatic Requires generation
AutoReq: no

%description 
Apache Hama is a pure BSP(Bulk Synchronous Parallel) computing framework on top of HDFS (Hadoop Distributed File System) for massive scientific computations such as matrix, graph and network algorithms. Currently, it has the following features:

Job submission and management interface.
Multiple tasks per node.
Input/Output Formatter.
Checkpoint recovery.
Support to run with Apache Mesos.
Support to run with Hadoop YARN.

%package bspmaster
Summary: Maintain the global state of Apache Hama services
Group: Development/Libraries
BuildArch: noarch
Requires: %{name} = %{version}-%{release}
Requires: /sbin/service
Requires(pre): %{name} = %{version}-%{release}
Requires(post): /sbin/chkconfig
Requires(preun): /sbin/chkconfig

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%endif

%if  0%{?mgaversion}
# Required for init scripts
Requires: initscripts
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description bspmaster
BSPMaster is responsible for the following:
- Maintaining groom server status.
- Maintaining supersteps and other counters in a cluster.
- Maintaining job progress information.
- Scheduling Jobs and assigning tasks to groom servers
- Distributing execution classes and configuration across groom servers.
- Providing users with the cluster control interface (web and console based). 

%package groom
Summary: A Groom Server (shortly referred to as groom) is a process that launches bsp tasks assigned by BSPMaster
Group: Development/Libraries
BuildArch: noarch
Requires: %{name} = %{version}-%{release}
Requires: /sbin/service
Requires(pre): %{name} = %{version}-%{release}
Requires(post): /sbin/chkconfig
Requires(preun): /sbin/chkconfig

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%endif

%if  0%{?mgaversion}
# Required for init scripts
Requires: initscripts
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description groom
A Groom Server (shortly referred to as groom) is a process that launches bsp tasks
assigned by BSPMaster. Each groom contacts the BSPMaster, and it takes assigned
tasks and reports its status by means of periodical piggybacks with BSPMaster.
Each groom is designed to run with HDFS or other distributed storages. Basically,
a groom server and a data node should be run on one physical node to get the best performance. (Data-locality) 


%package conf-pseudo
Summary: Apache Hama installation in pseudo-distributed mode
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires: %{name}-groom = %{version}-%{release}
Requires: %{name}-bspmaster = %{version}-%{release}
Requires: zookeeper-server

%description conf-pseudo
Installation of this RPM will setup your machine to run in pseudo-distributed mode
where each Apache Hama daemon runs in a separate Java process.


%package doc
Summary: Apache Hama Documentation
Group: Documentation
Requires: %{name} = %{version}-%{release}

%description doc
Documentation for Apache Hama

  
%prep
%setup -n %{name}-%{hama_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
sh -x $RPM_SOURCE_DIR/install_hama.sh \
          --distro-dir=$RPM_SOURCE_DIR \
          --build-dir=dist/target/hama-%{hama_base_version}/hama-%{hama_base_version} \
          --prefix=$RPM_BUILD_ROOT \
          --doc-dir=%{doc_hama} 

# Reuse Apache Hadoop jar
rm -f $RPM_BUILD_ROOT/usr/lib/hama/lib/hadoop*.jar
ln -s /usr/lib/hadoop/hadoop-core.jar $RPM_BUILD_ROOT/usr/lib/hama/lib/hadoop-core.jar

# Reuse Apache Zookeeper jar
rm -f $RPM_BUILD_ROOT/usr/lib/hama/lib/zookeeper*.jar
ln -s /usr/lib/zookeeper/zookeeper.jar $RPM_BUILD_ROOT/usr/lib/hama/lib/zookeeper.jar


# Install init script
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

init_file_bspmaster=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-bspmaster
%__cp %{SOURCE3} $init_file_bspmaster
chmod 755 $init_file_bspmaster

init_file_groom=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-groom
%__cp %{SOURCE4} $init_file_groom
chmod 755 $init_file_groom


# Install default file
%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default
%__cp %{SOURCE5} $RPM_BUILD_ROOT/etc/default/%{name}

%pre
getent group hama >/dev/null || groupadd -r hama
getent passwd hama >/dev/null || useradd -c "Apache Hama" -s /sbin/nologin -g hama -r -d /var/run/hama hama 2> /dev/null || :
%__install -d -o hama -g hama -m 0755 /var/log/hama


%post
%{alternatives_cmd} --install %{config_hama} %{hama_name}-conf %{config_hama}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{hama_name}-conf %{config_hama}.dist || :
fi

# bspmaster service
%post bspmaster
chkconfig --add %{name}-bspmaster

%preun bspmaster
if [ $1 = 0 ] ; then
        service %{name}-bspmaster stop > /dev/null 2>&1
        chkconfig --del %{name}-bspmaster
fi
%postun bspmaster
if [ $1 -ge 1 ]; then
        service %{name}-bspmaster condrestart >/dev/null 2>&1
fi

# Groom service
%post groom
chkconfig --add %{name}-groom

%preun groom
if [ $1 = 0 ] ; then
        service %{name}-groom stop > /dev/null 2>&1
        chkconfig --del %{name}-groom
fi
%postun groom
if [ $1 -ge 1 ]; then
        service %{name}-groom condrestart >/dev/null 2>&1
fi

# Pseudo-distributed Hadoop installation
%post conf-pseudo
%{alternatives_cmd} --install %{config_hama} %{name}-conf %{etc_hama}/conf.pseudo 40

%preun conf-pseudo
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{etc_hama}/conf.pseudo
fi



#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_hama}.dist
%doc *.txt
%{lib_hama}
%{bin_hama}/hama
%config(noreplace) /etc/default/%{name}

%files bspmaster
%attr(0755,root,root)/%{initd_dir}/%{name}-bspmaster

%files groom
%attr(0755,root,root)/%{initd_dir}/%{name}-groom

%files conf-pseudo
%defattr(-,root,root,755)
%config(noreplace) %attr(755,root,root) %{etc_hama}/conf.pseudo

%files doc
%defattr(-,root,root,755)
%doc %{doc_hama}
