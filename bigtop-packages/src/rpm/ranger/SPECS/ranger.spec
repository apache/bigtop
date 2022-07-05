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
%define etc_ranger /etc/%{ranger_name}
%define ranger_home /usr/lib/%{ranger_name}
%define ranger_user_home /var/lib/%{ranger_name}
%define config_ranger %{etc_ranger}/conf
%define config_ranger_dist %{config_ranger}.dist

%define usr_lib_ranger /usr/lib/%{ranger_name}
%define var_log_ranger /var/log/%{ranger_name}
%define var_run_ranger /var/run/%{ranger_name}
%define usr_bin /usr/bin
%define man_dir %{ranger_home}/man
%define ranger_services ranger-admin ranger-usersync ranger-tagsync ranger-kms
%define ranger_dist build

%define hadoop_home /usr/lib/hadoop
%define hive_home /usr/lib/hive
%define knox_home /usr/lib/knox
%define storm_home /usr/lib/storm
%define hbase_home /usr/lib/hbase
%define kafka_home /usr/lib/kafka
%define atlas_home /usr/lib/atlas


%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_ranger %{_docdir}/%{ranger_name}-%{ranger_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_ranger %{_docdir}/%{ranger_name}
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

Name: %{ranger_name}
Version: %{ranger_base_version}
Release: %{ranger_release}
Summary: Ranger is a security framework for securing Hadoop data
License: Apache License v2.0
URL: http://ranger.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{ranger_name}-%{version}
Source0: release-%{ranger_name}-%{ranger_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{ranger_name}.sh
#BIGTOP_PATCH_FILES
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
# Sadly, Sun/Oracle JDK in RPM form doesn't provide libjvm.so, which means we have
# to set AutoReq to no in order to minimize confusion. Not ideal, but seems to work.
# I wish there was a way to disable just one auto dependency (libjvm.so)
AutoReq: no


%if  %{?suse_version:1}0
# Required for init scripts
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: sh-utils, redhat-lsb
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
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: sh-utils, redhat-lsb
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
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: sh-utils, redhat-lsb
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
Requires: sh-utils, insserv
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: sh-utils, redhat-lsb
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
Requires: sh-utils, insserv
%endif
# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: sh-utils, redhat-lsb
%endif
%if  0%{?mgaversion}
Requires: chkconfig, xinetd-simple-services, zlib, initscripts
%endif
%description tagsync
Ranger-tagsync is tag synchronizer component associated with the Ranger framework

%package hdfs-plugin
Summary: ranger plugin for hdfs
Group: System/Daemons

%description hdfs-plugin
Ranger HDFS plugnin component runs within namenode to provoide enterprise security using ranger framework

%package yarn-plugin
Summary: ranger plugin for yarn
Group: System/Daemons

%description yarn-plugin
Ranger YARN plugnin component runs within namenode to provoide enterprise security using ranger framework

%package hive-plugin
Summary: ranger plugin for hive
Group: System/Daemons

%description hive-plugin
Ranger Hive plugnin component runs within hiveserver2 to provoide enterprise security using ranger framework

%package hbase-plugin
Summary: ranger plugin for hbase
Group: System/Daemons

%description hbase-plugin
Ranger HBASE plugnin component runs within master and regional servers as co-processor to provoide enterprise security using ranger framework

%package knox-plugin
Summary: ranger plugin for knox
Group: System/Daemons

%description knox-plugin
Ranger KNOX plugnin component runs within knox proxy server to provoide enterprise security using ranger framework

%package storm-plugin
Summary: ranger plugin for storm
Group: System/Daemons

%description storm-plugin
Ranger STORM plugnin component runs within storm to provoide enterprise security using ranger framework

%package kafka-plugin
Summary: ranger plugin for kafka
Group: System/Daemons

%description kafka-plugin
Ranger KAFKA plugnin component runs within namenode to provoide enterprise security using ranger framework

%package atlas-plugin
Summary: ranger plugin for atlas
Group: System/Daemons

%description atlas-plugin
Ranger ATLAS plugnin component runs within namenode to provoide enterprise security using ranger framework

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
echo
for comp in admin usersync kms tagsync hdfs-plugin yarn-plugin hive-plugin hbase-plugin knox-plugin storm-plugin kafka-plugin atlas-plugin
do
	env RANGER_VERSION=%{ranger_base_version} /bin/bash %{SOURCE2} \
  		--prefix=$RPM_BUILD_ROOT \
  		--build-dir=%{ranger_dist} \
  		--component=${comp} \
  		--doc-dir=$RPM_BUILD_ROOT/%{doc_ranger}
echo;echo
done

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%pre admin
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d /var/lib/%{ranger_name} ranger 2> /dev/null || :

%pre usersync
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d /var/lib/%{ranger_name} ranger 2> /dev/null || :

%pre kms
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d /var/lib/%{ranger_name} ranger 2> /dev/null || :

%pre tagsync
getent group ranger >/dev/null || groupadd -r ranger
getent passwd ranger >/dev/null || useradd -c "Ranger" -s /bin/bash -g ranger -m -d /var/lib/%{ranger_name} ranger 2> /dev/null || :

%post usersync
if [ -f %{usr_lib_ranger}-usersync/native/credValidator.uexe ]; then
    chmod u+s %{usr_lib_ranger}-usersync/native/credValidator.uexe
fi

%preun

%postun

#######################
#### FILES SECTION ####
#######################
%files admin
%defattr(-,root,root,755)
%attr(0775,ranger,ranger) %{var_run_ranger}/admin
%attr(0775,ranger,ranger) %{var_log_ranger}/admin
%{usr_lib_ranger}-admin
%config(noreplace) /etc/ranger/admin/conf.dist

%files usersync
%defattr(-,root,root,755)
%attr(0775,ranger,ranger) %{var_run_ranger}/usersync
%attr(0775,ranger,ranger) %{var_log_ranger}/usersync
%{usr_lib_ranger}-usersync
%attr(750,root,ranger) %{usr_lib_ranger}-usersync/native/credValidator.uexe
%config(noreplace) /etc/ranger/usersync/conf.dist

%files kms
%defattr(-,root,root,755)
%attr(0775,ranger,ranger) %{var_run_ranger}/kms
%attr(0775,ranger,ranger) %{var_log_ranger}/kms
%{usr_lib_ranger}-kms
%config(noreplace) /etc/ranger/kms/conf.dist

%files tagsync
%defattr(-,root,root,755)
%attr(0775,ranger,ranger) %{var_run_ranger}/tagsync
%attr(0775,ranger,ranger) %{var_log_ranger}/tagsync
%{usr_lib_ranger}-tagsync
%config(noreplace) /etc/ranger/tagsync/conf.dist

%files hdfs-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hdfs-plugin
%{hadoop_home}/lib

%files yarn-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-yarn-plugin
%{hadoop_home}/lib

%files hive-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hive-plugin
%{hive_home}/lib

%files hbase-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-hbase-plugin
%{hbase_home}/lib

%files knox-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-knox-plugin
%{knox_home}/lib

%files storm-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-storm-plugin
%{storm_home}/lib

%files kafka-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-kafka-plugin
%{kafka_home}/lib

%files atlas-plugin
%defattr(-,root,root,755)
%{usr_lib_ranger}-atlas-plugin
%{atlas_home}/lib
