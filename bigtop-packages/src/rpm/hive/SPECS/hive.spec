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

%define etc_default %{prefix}/etc/default

%define usr_lib_hive %{prefix}/usr/lib/%{name}
%define usr_lib_hcatalog %{prefix}/usr/lib/%{name}-hcatalog
%define var_lib_hive %{prefix}/var/lib/%{name}
%define var_lib_hcatalog %{prefix}/var/lib/%{name}-hcatalog

%define usr_lib_zookeeper %{prefix}/usr/lib/zookeeper
%define usr_lib_hbase %{prefix}/usr/lib/hbase

%define bin_dir %{prefix}/%{_bindir}
%define man_dir %{prefix}/%{_mandir}
%define doc_dir %{prefix}/%{_docdir}

# No prefix directory
%define np_var_log_hive /var/log/%{name}
%define np_var_run_hive /var/run/%{name}
%define np_var_log_hcatalog /var/log/%{name}-hcatalog
%define np_etc_hive /etc/%{name}
%define np_etc_hcatalog /etc/%{name}-hcatalog
%define np_etc_webhcat /etc/%{name}-webhcat

%define hive_config_virtual hive_active_configuration
%define hive_services hive-metastore hive-server2 hive-hcatalog-server hive-webhcat-server
# After we run "ant package" we'll find the distribution here
%define hive_dist build/dist

%if  %{!?suse_version:1}0

%define doc_hive %{doc_dir}/%{name}-%{hive_version}
%define alternatives_cmd alternatives

%global initd_dir %{_sysconfdir}/rc.d/init.d

%else

# Only tested on openSUSE 11.4. let's update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_hive %{doc_dir}/%{name}
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
License: ASL 2.0
URL: http://hive.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: apache-%{name}-%{hive_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_hive.sh
Source3: init.d.tmpl
Source4: hive-site.xml
Source6: hive-metastore.default
Source7: hive.1
Source8: hive-site.xml
Source10: hive-metastore.svc
Source11: hive-server2.default
Source12: hive-server2.svc
Source13: hive-hcatalog.1
Source14: hive-hcatalog-server.svc
Source15: hive-webhcat-server.svc
Source16: hive-hcatalog-server.default
Source17: hive-webhcat-server.default
Source18: bigtop.bom
#BIGTOP_PATCH_FILES
Requires: hadoop-client, bigtop-utils >= 0.7, zookeeper, hive-jdbc = %{version}-%{release}
Conflicts: hadoop-hive
Obsoletes: %{name}-webinterface

%description 
Hive is a data warehouse infrastructure built on top of Hadoop that provides tools to enable easy data summarization, adhoc querying and analysis of large datasets data stored in Hadoop files. It provides a mechanism to put structure on this data and it also provides a simple query language called Hive QL which is based on SQL and which enables users familiar with SQL to query this data. At the same time, this language also allows traditional map/reduce programmers to be able to plug in their custom mappers and reducers to do more sophisticated analysis which may not be supported by the built-in capabilities of the language.

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
# Required for init scripts
Requires: /lib/lsb/init-functions
%endif

%package server2
Summary: Provides a Hive Thrift service with improved concurrency support.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
# Required for init scripts
Requires: /lib/lsb/init-functions
%endif

%description server2
This optional package hosts a Thrift server for Hive clients across a network to use with improved concurrency support.
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
Requires: /lib/lsb/init-functions
%endif


%description metastore
This optional package hosts a metadata server for Hive clients across a network to use.

%package hbase
Summary: Provides integration between Apache HBase and Apache Hive
Group: Development/Libraries
Requires: hive = %{version}-%{release}, hbase

%description hbase
This optional package provides integration between Apache HBase and Apache Hive

%package jdbc
Summary: Provides libraries necessary to connect to Apache Hive via JDBC
Group: Development/Libraries
Requires: hadoop-client

%description jdbc
This package provides libraries necessary to connect to Apache Hive via JDBC

%package hcatalog
Summary: Apache Hcatalog is a data warehouse infrastructure built on top of Hadoop
Group: Development/Libraries
Requires: hadoop, hive, bigtop-utils >= 0.7

%description hcatalog
Apache HCatalog is a table and storage management service for data created using Apache Hadoop.
This includes:
    * Providing a shared schema and data type mechanism.
    * Providing a table abstraction so that users need not be concerned with where or how their data is stored.
    * Providing interoperability across data processing tools such as Pig, Map Reduce, Streaming, and Hive.


%package webhcat
Summary: WebHcat provides a REST-like web API for HCatalog and related Hadoop components.
Group: Development/Libraries
Requires: %{name}-hcatalog = %{version}-%{release}

%description webhcat
WebHcat provides a REST-like web API for HCatalog and related Hadoop components.


%package hcatalog-server
Summary: Init scripts for HCatalog server
Group: System/Daemons
Requires: %{name}-hcatalog = %{version}-%{release}

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
Requires: /lib/lsb/init-functions
%endif

%description hcatalog-server
Init scripts for HCatalog server


%package webhcat-server
Summary: Init scripts for WebHcat server
Group: System/Daemons
Requires: %{name}-webhcat = %{version}-%{release}

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
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}
# Required for init scripts
Requires: /lib/lsb/init-functions
%endif

%description webhcat-server
Init scripts for WebHcat server.

%prep
%setup -q -n apache-%{name}-%{hive_base_version}-src

#BIGTOP_PATCH_COMMANDS

%build
env \
  DO_MAVEN_DEPLOY=%{?do_maven_deploy} \
  MAVEN_DEPLOY_SOURCE=%{?maven_deploy_source} \
  MAVEN_REPO_ID=%{?maven_repo_id} \
  MAVEN_REPO_URI=%{?maven_repo_uri} \
bash %{SOURCE1}

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

cp $RPM_SOURCE_DIR/hive.1 .
cp $RPM_SOURCE_DIR/hive-hcatalog.1 .
cp $RPM_SOURCE_DIR/hive-site.xml .
/bin/bash %{SOURCE2} \
  --build-dir=%{hive_dist} \
  --prefix=$RPM_BUILD_ROOT \
  --doc-dir=%{doc_hive} \
  --bin-dir=%{bin_dir} \
  --man-dir=%{man_dir} \
  --etc-default=%{etc_default} \
  --hive-dir=%{usr_lib_hive} \
  --var-lib-hive=%{var_lib_hive} \
  --hcatalog-dir=%{usr_lib_hcatalog} \
  --var-lib-hcatalog=%{var_lib_hcatalog} \
  --hive-version=%{hive_base_version}

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{etc_default}/
%__install -m 0644 $RPM_SOURCE_DIR/hive-metastore.default $RPM_BUILD_ROOT/%{etc_default}/%{name}-metastore
%__install -m 0644 $RPM_SOURCE_DIR/hive-server2.default $RPM_BUILD_ROOT/%{etc_default}/%{name}-server2
%__install -m 0644 $RPM_SOURCE_DIR/hive-hcatalog-server.default $RPM_BUILD_ROOT/%{etc_default}/%{name}-hcatalog-server
%__install -m 0644 $RPM_SOURCE_DIR/hive-webhcat-server.default $RPM_BUILD_ROOT/%{etc_default}/%{name}-webhcat-server

%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_log_hive}
%__install -d -m 0755 $RPM_BUILD_ROOT/%{np_var_run_hive}

# We need to get rid of jars that happen to be shipped in other Bigtop packages
%__rm -f $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/hbase-*.jar $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/zookeeper-*.jar
%__ln_s  %{usr_lib_zookeeper}/zookeeper.jar  $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/
%__ln_s  %{usr_lib_hbase}/hbase-common.jar %{usr_lib_hbase}/hbase-client.jar %{usr_lib_hbase}/hbase-hadoop-compat.jar %{usr_lib_hbase}/hbase-hadoop2-compat.jar $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/
%__ln_s  %{usr_lib_hbase}/hbase-procedure.jar %{usr_lib_hbase}/hbase-protocol.jar %{usr_lib_hbase}/hbase-server.jar $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/

# Workaround for BIGTOP-583
%__rm -f $RPM_BUILD_ROOT/%{usr_lib_hive}/lib/slf4j-log4j12-*.jar

for service in %{hive_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/${service}.svc rpm $init_file
done

%pre
getent group hive >/dev/null || groupadd -r hive
getent passwd hive >/dev/null || useradd -c "Hive" -s /sbin/nologin -g hive -r -d %{var_lib_hive} hive 2> /dev/null || :

# Manage configuration symlink
%post

# Install config alternatives
%{alternatives_cmd} --install %{np_etc_hive}/conf %{name}-conf %{np_etc_hive}/conf.dist 30


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
  %{alternatives_cmd} --remove %{name}-conf %{np_etc_hive}/conf.dist || :
fi


%post hcatalog
%{alternatives_cmd} --install %{np_etc_hcatalog}/conf hive-hcatalog-conf %{np_etc_hcatalog}/conf.dist 30

%preun hcatalog
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove hive-hcatalog-conf %{np_etc_hcatalog}/conf.dist || :
fi

%post webhcat
%{alternatives_cmd} --install %{np_etc_webhcat}/conf hive-webhcat-conf %{np_etc_webhcat}/conf.dist 30

%preun webhcat
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove hive-webhcat-conf %{np_etc_webhcat}/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%attr(1777,hive,hive) %dir %{var_lib_hive}/metastore
%defattr(-,root,root,755)
%config(noreplace) %{np_etc_hive}/conf.dist
%{usr_lib_hive}
%{bin_dir}/hive
%{bin_dir}/beeline
%{bin_dir}/hiveserver2
%attr(0755,hive,hive) %dir %{var_lib_hive}
%attr(0755,hive,hive) %dir %{np_var_log_hive}
%attr(0755,hive,hive) %dir %{np_var_run_hive}
%doc %{doc_hive}
%{man_dir}/man1/hive.1.*
%exclude %dir %{usr_lib_hive}
%exclude %dir %{usr_lib_hive}/jdbc
%exclude %{usr_lib_hive}/jdbc/hive-jdbc-*.jar

%files hbase
%defattr(-,root,root,755)
%{usr_lib_hive}/lib/hbase-*.jar
%{usr_lib_hive}/lib/hive-hbase-handler*.jar

%files jdbc
%defattr(-,root,root,755)
%dir %{usr_lib_hive}
%dir %{usr_lib_hive}/jdbc
%{usr_lib_hive}/jdbc/hive-jdbc-*.jar

%files hcatalog
%defattr(-,root,root,755)
%config(noreplace) %attr(755,root,root) %{np_etc_hcatalog}/conf.dist
%attr(0775,hive,hive) %{var_lib_hcatalog}
%attr(0775,hive,hive) %{np_var_log_hcatalog}
%dir %{usr_lib_hcatalog}
%{usr_lib_hcatalog}/bin
%{usr_lib_hcatalog}/etc/hcatalog
%{usr_lib_hcatalog}/libexec
%{usr_lib_hcatalog}/share/hcatalog
%{usr_lib_hcatalog}/sbin/update-hcatalog-env.sh
%{usr_lib_hcatalog}/sbin/hcat*
%{bin_dir}/hcat
%{man_dir}/man1/hive-hcatalog.1.*

%files webhcat
%defattr(-,root,root,755)
%config(noreplace) %attr(755,root,root) %{np_etc_webhcat}/conf.dist
%{usr_lib_hcatalog}/share/webhcat
%{usr_lib_hcatalog}/etc/webhcat
%{usr_lib_hcatalog}/sbin/webhcat*

%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{name}-%1 \
%config(noreplace) %{etc_default}/%{name}-%1 \
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
%service_macro server2
%service_macro metastore
%service_macro hcatalog-server
%service_macro webhcat-server
