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
# Tajo RPM spec file
#

%define tajo_name   tajo
%define tajo_home   /usr/lib/%{tajo_name}
%define log_tajo    /var/log/%{tajo_name}
%define run_tajo    /var/run/%{tajo_name}
%define pid_tajo    /var/lib/%{tajo_name}
%define bin         %{_bindir}
%define etc_tajo    /etc/%{tajo_name}
%define config_tajo %{etc_tajo}/conf
%define man_tajo    %{_mandir}
%define doc_tajo    %{_docdir}/%{name}
%define libexecdir  /usr/lib
%define tajo_services master worker

%ifarch i386
%global tajo_arch Linux-i386-32
%endif
%ifarch amd64 x86_64
%global tajo_arch Linux-amd64-64
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

#%define netcat_package nc
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

%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d
%endif

%if  0%{?mgaversion}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif


Name:    %{tajo_name}
Version: %{tajo_version}
Release: %{tajo_release}
Summary: Apache Tajo™: A big data warehouse system on Hadoop
License: ASL 2.0
URL:     http://tajo.apache.org/
Group:   Development/Libraries
Source0: %{tajo_name}-%{tajo_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{tajo_name}.sh
Source3: init.d.tmpl
Source4: tajo-master.svc
Source5: tajo-worker.svc
Buildroot: %{mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX}
Requires:  hadoop, hadoop-hdfs, hadoop-yarn, hadoop-mapreduce, hadoop-client

%description
Apache Tajo is a robust big data relational and distributed data warehouse system for Apache Hadoop. 
Tajo is designed for low-latency and scalable ad-hoc queries, online aggregation, and ETL (extract-transform-load process) 
on large-data sets stored on HDFS (Hadoop Distributed File System) and other data sources. 
By supporting SQL standards and leveraging advanced database techniques, 
Tajo allows direct control of distributed execution and data flow across a variety of query evaluation strategies and optimization opportunities.

%package -n %{name}-master
Summary: Server for Tajo master
Group: Development/Libraries
Requires: %{name} = %{version}-%{release}

%description -n %{name}-master
Server for Tajo master

%package -n %{name}-worker
Summary: Server for Tajo worker
Group: Development/Libraries
Requires: %{name} = %{version}-%{release}

%description -n %{name}-worker
Server for Tajo worker

%prep
%setup -n %{name}-%{tajo_base_version}-src

#BIGTOP_PATCH_COMMANDS
%build

env TAJO_VERSION=%{tajo_base_version} TAJO_ARCH=%{tajo_arch} bash %{SOURCE1}

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{tajo_home}

env TAJO_VERSION=%{tajo_base_version} bash %{SOURCE2} \
    --build-dir=build \
    --source-dir=$RPM_SOURCE_DIR \
    --bin-dir=%{bin} \
    --prefix=$RPM_BUILD_ROOT \

# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/
%__install -d -m 0755  %{buildroot}/%{_localstatedir}/log/%{name}
%__install -d -m 0755  %{buildroot}/%{_localstatedir}/run/%{name}
%__install -d -m 0755  %{buildroot}/%{_localstatedir}/lib/%{name}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{bin}

# Generate the init.d scripts
for service in %{tajo_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{tajo_name}-${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/tajo-${service}.svc rpm $init_file
done

%pre
getent group tajo >/dev/null || groupadd -r tajo
getent passwd tajo >/dev/null || useradd -c "Tajo" -s /sbin/nologin -g tajo -r -d %{pid_tajo} tajo 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_tajo} %{tajo_name}-conf %{config_tajo}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{tajo_name}-conf %{config_tajo}.dist || :
fi
for service in %{tajo_services}; do
  /sbin/service %{tajo_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{tajo_name}-${service} stop > /dev/null 2>&1
  fi
done

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root,755)
%{tajo_home}
%dir %{_sysconfdir}/%{tajo_name}
%config(noreplace) %{config_tajo}.dist
%dir %{_localstatedir}/log/tajo
%dir %{_localstatedir}/run/tajo
%dir %{_localstatedir}/lib/tajo
%attr(0775,tajo,tajo) %{log_tajo}
%attr(0775,tajo,tajo) %{run_tajo}
%attr(0775,tajo,tajo) %{pid_tajo}
%attr(0775,tajo,tajo) %(config_tajo)
%{bin}/tsql
%{bin}/tajo

%define service_macro() \
%files -n %1 \
%attr(0755,root,root)/%{initd_dir}/%1 \
%post -n %1 \
chkconfig --add %1 \
\
%preun -n %1 \
if [ $1 = 0 ] ; then \
        service %1 stop > /dev/null 2>&1 \
        chkconfig --del %1 \
fi \
%postun -n %1 \
if [ $1 -ge 1 ]; then \
        service %1 condrestart >/dev/null 2>&1 \
fi
%service_macro %{tajo_name}-master
%service_macro %{tajo_name}-worker
