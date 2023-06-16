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
%undefine _missing_build_ids_terminate_build

%define ranger_name ranger
%define ranger_pkg_name ranger%{pkg_name_suffix}
%define usr_lib_ranger %{parent_dir}/usr/lib/%{ranger_name}
%define var_lib_ranger %{parent_dir}/var/lib/%{ranger_name}
%define etc_ranger %{parent_dir}/etc/%{ranger_name}
%define np_etc_ranger /etc/%{ranger_name}

%define usr_lib_hadoop %{parent_dir}/usr/lib/hadoop
%define usr_lib_hive %{parent_dir}/usr/lib/hive
%define usr_lib_knox %{parent_dir}/usr/lib/knox
%define usr_lib_storm %{parent_dir}/usr/lib/storm
%define usr_lib_hbase %{parent_dir}/usr/lib/hbase
%define usr_lib_kafka %{parent_dir}/usr/lib/kafka
%define usr_lib_atlas %{parent_dir}/usr/lib/atlas
%define usr_lib_solr %{parent_dir}/usr/lib/solr
%define usr_lib_sqoop %{parent_dir}/usr/lib/sqoop
%define usr_lib_kylin %{parent_dir}/usr/lib/kylin
%define usr_lib_elasticsearch %{parent_dir}/usr/lib/elasticsearch
%define usr_lib_presto %{parent_dir}/usr/lib/presto

%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_run_ranger /var/run/%{ranger_name}

%define ranger_services ranger-admin ranger-usersync ranger-tagsync ranger-kms
%define ranger_dist build

%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_ranger %{doc_dir}/%{ranger_name}-%{ranger_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_ranger %{doc_dir}/%{ranger_name}
%define alternatives_cmd update-alternatives

%global initd_dir %{_sysconfdir}/rc.d

%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif


%if  0%{?mgaversion}
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

# Disable debuginfo package
%define debug_package %{nil}

Name: %{ranger_pkg_name}
Version: %{ranger_base_version}
Release: %{ranger_release}
Summary: Ranger is a framework for securing Hadoop data
License: Apache License v2.0
URL: http://ranger.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{ranger_name}-%{version}
Source0: release-%{ranger_name}-%{ranger_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{ranger_name}.sh
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no


%if  %{?suse_version:1}0
# Required for init scripts
Requires: coreutils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: coreutils, redhat-lsb
%endif

%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif

%description 
Ranger is a framework to secure hadoop data 

%package admin
Summary: Web Interface for Ranger
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no


%if  %{?suse_version:1}0
# Required for init scripts
Requires: coreutils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: coreutils, redhat-lsb
%endif

%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif

%description admin
Ranger-admin is admin component associated with the Ranger framework

%package usersync
Summary: Synchronize User/Group information from Corporate LD/AD or Unix
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no


%if  %{?suse_version:1}0
# Required for init scripts
Requires: coreutils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: coreutils, redhat-lsb
%endif

%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif

%description usersync
Ranger-usersync is user/group synchronization component associated with the Ranger framework

%package kms
Summary: Key Management Server
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no


%if  %{?suse_version:1}0
# Required for init scripts
Requires: coreutils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: coreutils, redhat-lsb
%endif

%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif

%description kms
Ranger-kms is key management server component associated with the Ranger framework


%package tagsync
Summary: Tag Synchronizer
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no
%if  %{?suse_version:1}0
# Required for init scripts
Requires: coreutils, insserv
%endif
# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: coreutils, redhat-lsb
%endif
%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif
%description tagsync
Ranger-tagsync is tag synchronizer component associated with the Ranger framework

%package hdfs-plugin
Summary: ranger plugin for hdfs
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description hdfs-plugin
Ranger HDFS plugin component runs within namenode to provide enterprise security using ranger framework

%package yarn-plugin
Summary: ranger plugin for yarn
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description yarn-plugin
Ranger YARN plugin component runs within resourcemanager to provide enterprise security using ranger framework

%package hive-plugin
Summary: ranger plugin for hive
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description hive-plugin
Ranger Hive plugin component runs within hiveserver2 to provide enterprise security using ranger framework

%package hbase-plugin
Summary: ranger plugin for hbase
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description hbase-plugin
Ranger HBASE plugin component runs within master and region servers as co-processor to provide enterprise security using ranger framework

%package knox-plugin
Summary: ranger plugin for knox
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description knox-plugin
Ranger KNOX plugin component runs within knox proxy server to provide enterprise security using ranger framework

%package storm-plugin
Summary: ranger plugin for storm
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description storm-plugin
Ranger STORM plugin component runs within storm to provide enterprise security using ranger framework

%package kafka-plugin
Summary: ranger plugin for kafka
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description kafka-plugin
Ranger KAFKA plugin component runs within kafka to provide enterprise security using ranger framework

%package atlas-plugin
Summary: ranger plugin for atlas
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description atlas-plugin
Ranger ATLAS plugin component runs within atlas to provide enterprise security using ranger framework

%package solr-plugin
Summary: ranger plugin for solr
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description solr-plugin
Ranger SOLR plugin component runs within solr to provide enterprise security using ranger framework

%package sqoop-plugin
Summary: ranger plugin for sqoop
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description sqoop-plugin
Ranger SQOOP plugin component runs within sqoop to provide enterprise security using ranger framework

%package kylin-plugin
Summary: ranger plugin for kylin
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description kylin-plugin
Ranger KYLIN plugin component runs within kylin to provide enterprise security using ranger framework

%package elasticsearch-plugin
Summary: ranger plugin for elasticsearch
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description elasticsearch-plugin
Ranger ELASTICSEARCH plugin component runs within elasticsearch to provide enterprise security using ranger framework

%package presto-plugin
Summary: ranger plugin for presto
Group: System/Daemons
# On Rocky 8, find-requires picks up /usr/bin/python, but it's not provided by any package.
# So installing ranger-*-plugin fails with a "nothing provides /usr/bin/python" message,
# even when python3 is installed and /usr/bin/python is created as a symlink to python3.
# Therefore we disable find-requires for each plugins with the following option.
AutoReq: no

%description presto-plugin
Ranger PRESTO plugin component runs within presto to provide enterprise security using ranger framework


%prep
%setup -q -n %{ranger_name}-release-%{ranger_name}-%{ranger_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%clean
%__rm -rf $RPM_BUILD_ROOT

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT
for comp in admin usersync kms tagsync hdfs-plugin yarn-plugin hive-plugin hbase-plugin knox-plugin storm-plugin kafka-plugin atlas-plugin sqoop-plugin solr-plugin kylin-plugin elasticsearch-plugin presto-plugin
do
	env RANGER_VERSION=%{ranger_base_version} /bin/bash %{SOURCE2} \
  		--prefix=$RPM_BUILD_ROOT \
  		--build-dir=%{ranger_dist} \
  		--component=${comp} \
        --comp-dir=%{usr_lib_ranger}-${comp} \
        --var-ranger=%{var_lib_ranger} \
        --etc-ranger=%{etc_ranger} \
  		--doc-dir=$RPM_BUILD_ROOT/%{doc_ranger}
done

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%pre admin
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d %{var_lib_ranger} ranger 2> /dev/null || :

%post admin
%{alternatives_cmd} --install %{np_etc_ranger}/admin/conf ranger-admin-conf %{etc_ranger}/admin/conf.dist 30

%preun admin
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove ranger-admin-conf %{etc_ranger}/admin/conf.dist || :
fi

%pre usersync
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d %{var_lib_ranger}} ranger 2> /dev/null || :

%post usersync
%{alternatives_cmd} --install %{np_etc_ranger}/usersync/conf ranger-usersync-conf %{etc_ranger}/usersync/conf.dist 30
if [ -f %{usr_lib_ranger}-usersync/native/credValidator.uexe ]; then
    chmod u+s %{usr_lib_ranger}-usersync/native/credValidator.uexe
fi

%preun usersync
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove ranger-usersync-conf %{etc_ranger}/usersync/conf.dist || :
fi

%pre kms
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d %{var_lib_ranger} ranger 2> /dev/null || :

%post kms
%{alternatives_cmd} --install %{np_etc_ranger}/kms/conf ranger-kms-conf %{etc_ranger}/kms/conf.dist 30

%preun kms
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove ranger-kms-conf %{etc_ranger}/kms/conf.dist || :
fi

%pre tagsync
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d %{var_lib_ranger} ranger 2> /dev/null || :

%post tagsync
%{alternatives_cmd} --install %{np_etc_ranger}/tagsync/conf ranger-tagsync-conf %{etc_ranger}/tagsync/conf.dist 30

%preun tagsync
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove ranger-tagsync-conf %{etc_ranger}/tagsync/conf.dist || :


%preun

%postun

#######################
#### FILES SECTION ####
#######################
%files admin
%defattr(-,root,root,755)
%attr(0775,ranger,ranger) %{var_lib_ranger}
%attr(0775,ranger,ranger) %{np_var_run_ranger}
%config(noreplace) %{etc_ranger}/admin/conf.dist
%attr(0755,ranger,ranger) %{np_etc_ranger}/admin
%{usr_lib_ranger}-admin

%files usersync
%defattr(-,root,root,755)
%{usr_lib_ranger}-usersync
%attr(750,root,ranger) %{usr_lib_ranger}-usersync/native/credValidator.uexe
%config(noreplace) %{etc_ranger}/usersync/conf.dist
%attr(0755,ranger,ranger) %{np_etc_ranger}/usersync

%files kms
%defattr(-,root,root,755)
%{usr_lib_ranger}-kms
%config(noreplace) %{etc_ranger}/kms/conf.dist
%attr(0755,ranger,ranger) %{np_etc_ranger}/kms

%files tagsync
%defattr(-,root,root,755)
%{usr_lib_ranger}-tagsync
%config(noreplace) %{etc_ranger}/tagsync/conf.dist
%attr(0755,ranger,ranger) %{np_etc_ranger}/tagsync

%files hdfs-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hdfs-plugin
%{usr_lib_hadoop}/lib

%files yarn-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-yarn-plugin
%{usr_lib_hadoop}/lib

%files hive-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hive-plugin
%{usr_lib_hive}/lib

%files hbase-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hbase-plugin
%{usr_lib_hbase}/lib

%files knox-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-knox-plugin
%{usr_lib_knox}/ext

%files storm-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-storm-plugin
%{usr_lib_storm}/extlib-daemon

%files kafka-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-kafka-plugin
%{usr_lib_kafka}/libs

%files atlas-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-atlas-plugin
%{usr_lib_atlas}/libext

%files solr-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-solr-plugin
%{usr_lib_solr}/server/solr-webapp/webapp/WEB-INF/lib/

%files sqoop-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-sqoop-plugin
%{usr_lib_sqoop}/server/lib

%files kylin-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-kylin-plugin
%{usr_lib_kylin}/tomcat/webapps/kylin/WEB-INF/lib

%files elasticsearch-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-elasticsearch-plugin
%{usr_lib_elasticsearch}/plugins

%files presto-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-presto-plugin
%{usr_lib_presto}/plugin/ranger
