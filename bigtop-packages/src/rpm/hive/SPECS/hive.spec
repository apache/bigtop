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
%define hadoop_username hadoop
%define etc_hive /etc/%{name}
%define config_hive %{etc_hive}/conf
%define usr_lib_hive /usr/lib/%{name}
%define var_lib_hive /var/lib/%{name}
%define bin_hive /usr/bin
%define hive_config_virtual hive_active_configuration
%define man_dir %{_mandir}
%define hive_services server metastore
# After we run "ant package" we'll find the distribution here
%define hive_dist src/build/dist

%if  %{!?suse_version:1}0

%define doc_hive %{_docdir}/%{name}-%{hive_version}
%define alternatives_cmd alternatives

%global initd_dir %{_sysconfdir}/rc.d/init.d

%else

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_hive %{_docdir}/%{name}
%define alternatives_cmd update-alternatives

%global initd_dir %{_sysconfdir}/rc.d

%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif


Name: hive
Version: %{hive_version}
Release: %{hive_release}
Summary: Hive is a data warehouse infrastructure built on top of Hadoop
License: Apache License v2.0
URL: http://hive.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: %{name}-%{hive_base_version}.tar.gz
Source1: do-component-build
Source2: install_hive.sh
Source3: init.d.tmpl
Source4: hive-site.xml
Source5: hive-server.default
Source6: hive-metastore.default
Source7: hive.1
Source8: hive-site.xml
Source9: hive-server.svc
Source10: hive-metastore.svc
Requires: hadoop-client, bigtop-utils >= 0.6, hbase, zookeeper
Obsoletes: %{name}-webinterface

%description 
Hive is a data warehouse infrastructure built on top of Hadoop that provides tools to enable easy data summarization, adhoc querying and analysis of large datasets data stored in Hadoop files. It provides a mechanism to put structure on this data and it also provides a simple query language called Hive QL which is based on SQL and which enables users familiar with SQL to query this data. At the same time, this language also allows traditional map/reduce programmers to be able to plug in their custom mappers and reducers to do more sophisticated analysis which may not be supported by the built-in capabilities of the language. 

%package server
Summary: Provides a Hive Thrift service.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
# Required for init scripts
Requires: redhat-lsb
%endif


%description server
This optional package hosts a Thrift server for Hive clients across a network to use.

%package metastore
Summary: Shared metadata repository for Hive.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
# Required for init scripts
Requires: redhat-lsb
%endif


%description metastore
This optional package hosts a metadata server for Hive clients across a network to use.


%package hbase
Summary: Provides integration between Apache HBase and Apache Hive
Group: Development/Libraries
Requires: hbase


%description hbase
This optional package provides integration between Apache HBase and Apache Hive



%prep
%setup -n %{name}-%{hive_base_version}

%build
bash %{SOURCE1}

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

cp $RPM_SOURCE_DIR/hive.1 .
cp $RPM_SOURCE_DIR/hive-site.xml .
/bin/bash %{SOURCE2} \
  --prefix=$RPM_BUILD_ROOT \
  --build-dir=%{hive_dist} \
  --doc-dir=%{doc_hive}

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/default/%{name}-metastore
%__install -m 0644 %{SOURCE5} $RPM_BUILD_ROOT/etc/default/%{name}-server

%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/log/%{name}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/run/%{name}

# Workaround for BIGTOP-583
%__rm -f $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/slf4j-log4j12-*.jar

# We need to get rid of jars that happen to be shipped in other CDH packages
%__rm -f $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/hbase-*.jar $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/zookeeper-*.jar
%__ln_s  /usr/lib/hbase/hbase.jar /usr/lib/zookeeper/zookeeper.jar  $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/

for service in %{hive_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/hive-${service}.svc rpm $init_file
done

%pre
getent group hive >/dev/null || groupadd -r hive
getent passwd hive >/dev/null || useradd -c "Hive" -s /sbin/nologin -g hive -r -d %{var_lib_hive} hive 2> /dev/null || :

# Manage configuration symlink
%post

# Install config alternatives
%{alternatives_cmd} --install %{config_hive} %{name}-conf %{etc_hive}/conf.dist 30


# Upgrade
if [ "$1" -gt 1 ]; then
  old_metastore="${var_lib_hive}/metastore/\${user.name}_db"
  new_metastore="${var_lib_hive}/metastore/metastore_db"
  if [ -d $old_metastore ]; then
    mv $old_metastore $new_metastore || echo "Failed to automatically rename old metastore. Make sure to resolve this before running Hive."
  fi
fi

%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{name}-conf %{etc_hive}/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%attr(1777,hive,hive) %dir %{var_lib_hive}/metastore
%defattr(-,root,root,755)
%config(noreplace) %{etc_hive}/conf.dist
%{usr_lib_hive}
%{bin_hive}/hive
%attr(0755,hive,hive) %dir %{var_lib_hive}
%attr(0755,hive,hive) %dir %{_localstatedir}/log/%{name}
%attr(0755,hive,hive) %dir %{_localstatedir}/run/%{name}
%doc %{doc_hive}
%{man_dir}/man1/hive.1.*
%exclude %{usr_lib_hive}/lib/hbase.jar

%files hbase
%defattr(-,root,root,755)
%{usr_lib_hive}/lib/hbase.jar


%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{name}-%1 \
%config(noreplace) /etc/default/%{name}-%1 \
%post %1 \
chkconfig --add %{name}-%1 \
\
%preun %1 \
if [ "$1" = 0 ] ; then \
        service %{name}-%1 stop > /dev/null \
        chkconfig --del %{name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
	service %{name}-%1 condrestart >/dev/null 2>&1 || : \
fi
%service_macro server
%service_macro metastore
