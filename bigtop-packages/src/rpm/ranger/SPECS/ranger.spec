%define __jar_repack 0
%define _binaries_in_noarch_packages_terminate_build 0
%define _unpackaged_files_terminate_build 0

Name:		ranger
Version:	%{ranger_version}
Release:	%{ranger_release}
Summary:	Ranger is a security framework for securing Hadoop data

Group:		Application/Server
License:	Apache License, Version 2.0
URL:		  http://ranger.incubator.apache.org/
Source0:	ranger-%{ranger_version}.tar.gz
Source1:  do-component-build
Source2:  install_ranger.sh
#BIGTOP_PATCH_FILES

BuildArch:  noarch
Requires:	bash
Provides: 	ranger
AutoReqProv: 	no

%description
Ranger is a framework to secure hadoop data 


%package admin
Summary: Web Interface for Ranger 
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description admin
Ranger-admin is admin component associated with the Ranger framework

%package atlas-plugin
Summary:  Ranger plugin for atlas
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description atlas-plugin
Ranger ATLAS plugnin component runs within namenode to provoide enterprise security using ranger framework


%package hbase-plugin
Summary:  Ranger plugin for hbase
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description hbase-plugin
Ranger HBASE plugnin component runs within master and regional servers as co-processor to provoide enterprise security using ranger framework

%package hdfs-plugin
Summary:  Ranger plugin for hdfs
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description hdfs-plugin
Ranger HDFS plugnin component runs within namenode to provoide enterprise security using ranger framework

%package hive-plugin
Summary:   plugin for hive
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description hive-plugin
Ranger Hive plugnin component runs within hiveserver2 to provoide enterprise security using ranger framework

%package kafka-plugin
Summary:  Ranger plugin for kafka
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description kafka-plugin
Ranger KAFKA plugnin component runs within namenode to provoide enterprise security using ranger framework

%package kms
Summary:  Ranger Key Management Server
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description kms
Ranger-kms is key management server component associated with the Ranger framework

%package knox-plugin
Summary:  Ranger plugin for knox
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description knox-plugin
Ranger KNOX plugnin component runs within knox proxy server to provoide enterprise security using ranger framework

%package solr-plugin
Summary:  Ranger plugin for solr
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description solr-plugin
Ranger SOLR plugnin component runs within solr to provoide enterprise security using ranger framework

%package storm-plugin
Summary:  Ranger plugin for storm
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description storm-plugin
Ranger STORM plugnin component runs within storm to provoide enterprise security using ranger framework

%package tagsync
Summary:  Ranger Tag Synchronizer
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description tagsync
Ranger-tagsync is tag synchronizer component associated with the Ranger framework

%package usersync
Summary:  Ranger Synchronize User/Group information from Corporate LD/AD or Unix
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description usersync
Ranger-usersync is user/group synchronization component associated with the Ranger framework

%package yarn-plugin
Summary:  Ranger plugin for yarn
Group: System/Daemons
Requires: coreutils, /usr/sbin/useradd, /usr/sbin/usermod, /sbin/chkconfig, /sbin/service
Requires: psmisc
AutoReq: no
%description yarn-plugin
Ranger YARN plugnin component runs within namenode to provoide enterprise security using ranger framework

%prep
%setup -q  -n apache-ranger-%{ranger_version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{ranger_version}

%files admin
/usr/lib/ranger-admin

%files atlas-plugin
/usr/lib/atlas-server/*
/usr/lib/ranger-atlas-plugin

%files hbase-plugin
/usr/lib/hbase/*
/usr/lib/ranger-hbase-plugin

%files hdfs-plugin
/usr/lib/hadoop/*
/usr/lib/ranger-hdfs-plugin

%files hive-plugin
/usr/lib/hive/*
/usr/lib/hive2/*
/usr/lib/ranger-hive-plugin

%files kafka-plugin
/usr/lib/kafka/*
/usr/lib/ranger-kafka-plugin

%files kms
/usr/lib/ranger-kms

%files knox-plugin
/usr/lib/knox/*
/usr/lib/ranger-knox-plugin

%files solr-plugin
/usr/lib/solr/*
/usr/lib/ranger-solr-plugin

%files storm-plugin
/usr/lib/storm/extlib-daemon/*
/usr/lib/ranger-storm-plugin

%files tagsync
/usr/lib/ranger-tagsync

%files usersync
/usr/lib/ranger-usersync

%files yarn-plugin
/usr/lib/ranger-yarn-plugin

%changelog
