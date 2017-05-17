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

%define kafka_name kafka
%define lib_kafka /usr/lib/%{kafka_name}
%define var_lib_kafka /var/lib/%{kafka_name}
%define var_run_kafka /var/run/%{kafka_name}
%define var_log_kafka /var/log/%{kafka_name}
%define bin_kafka /usr/lib/%{kafka_name}/bin
%define etc_kafka /etc/%{kafka_name}
%define config_kafka %{etc_kafka}/conf
%define bin /usr/bin
%define man_dir /usr/share/man

%if  %{!?suse_version:1}0
%define doc_kafka %{_docdir}/%{kafka_name}-%{kafka_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d

%else

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define alternatives_cmd update-alternatives
%define doc_kafka %{_docdir}/%{kafka_name}-%{kafka_version}
%global initd_dir %{_sysconfdir}/rc.d

%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif


# disable repacking jars
%define __os_install_post %{nil}

Name: kafka
Version: %{kafka_version}
Release: %{kafka_release}
Summary: Apache Kafka is publish-subscribe messaging rethought as a distributed commit log.
URL: http://kafka.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{kafka_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{kafka_name}-%{kafka_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{kafka_name}.sh
Source3: kafka-server.svc
Source4: init.d.tmpl
Source6: kafka.default
Requires: zookeeper
Requires: bigtop-utils >= 0.7
Requires(preun): /sbin/service

%description
Apache Kafka is publish-subscribe messaging rethought as a distributed commit log.
A single Kafka broker can handle hundreds of megabytes of reads and writes per second
from thousands of clients. It can be elastically and transparently expanded without downtime.
Data streams are partitioned and spread over a cluster of machines to allow data streams
larger than the capability of any single machine and to allow clusters of co-ordinated consumers

%package server
Summary: Server for kafka
Group: System/Daemons
Requires: kafka = %{version}-%{release}

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description server
Bundles the init script for kafka server.

%prep
%setup -n %{kafka_name}-%{kafka_base_version}-src

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/default/%{kafka_name}

bash $RPM_SOURCE_DIR/install_kafka.sh \
          --build-dir=`pwd`         \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT  \
		  --doc-dir=%{doc_kafka} \

# Generate the init script
init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{kafka_name}-server
bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/%{kafka_name}-server.svc rpm $init_file


#######################
#### Kafka core section ####
#######################
%pre
getent group kafka >/dev/null || groupadd -r kafka
getent passwd kafka >/dev/null || useradd -c "Kafka" -s /sbin/nologin -g kafka -r -d %{var_lib_kafka} kafka 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_kafka} %{kafka_name}-conf %{config_kafka}.dist 30

%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{kafka_name}-conf %{config_kafka}.dist || :
fi

/sbin/service %{kafka_name}-server status > /dev/null 2>&1
if [ $? -eq 0 ]; then
  /sbin/service %{kafka_name}-server stop > /dev/null 2>&1
fi

#######################
#### Kafka-server section ####
#######################
%post server
chkconfig --add %{kafka_name}-server

%preun server
/sbin/service %{kafka_name}-server status > /dev/null 2>&1
if [ $? -eq 0  ] ; then
  service kafka-server stop > /dev/null 2>&1
  chkconfig --del %{kafka_name}-server
fi

%postun server
if [ $1 -ge 1 ]; then
  service %{kafka_name}-server condrestart >/dev/null 2>&1
fi

#######################
#### FILES SECTION ####
#######################
%files server
%attr(0755,root,root) %{initd_dir}/%{kafka_name}-server

%files
%defattr(-,root,root,755)
%{bin}/*
%config(noreplace) %{config_kafka}.dist
%config(noreplace) /etc/default/kafka
%attr(0755,kafka,kafka) %{lib_kafka}
%attr(0755,kafka,kafka) %docdir %{doc_kafka}
%attr(0755,kafka,kafka) %{var_lib_kafka}
%attr(0755,kafka,kafka) %{var_run_kafka}
%attr(0755,kafka,kafka) %{var_log_kafka}
%doc %{doc_kafka}
