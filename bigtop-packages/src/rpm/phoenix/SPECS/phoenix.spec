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
%define phoenix_home /usr/lib/%{name}
%define bin_phoenix %{phoenix_home}/bin
%define etc_phoenix_conf %{_sysconfdir}/%{name}/conf
%define etc_phoenix_conf_dist %{etc_phoenix_conf}.dist
%define var_lib_phoenix /var/lib/%{name}
%define var_log_phoenix /var/log/%{name}
%define man_dir %{_mandir}
%define zookeeper_home /usr/lib/zookeeper
%define hadoop_home /usr/lib/hadoop
%define hadoop_mapreduce_home /usr/lib/hadoop-mapreduce
%define hadoop_yarn_home /usr/lib/hadoop-yarn
%define hadoop_hdfs_home /usr/lib/hadoop-hdfs
%define hbase_home /usr/lib/hbase
#BIGTOP_PATCH_FILES

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict anc check all symlinks point to valid path
# But we do point to a hadoop jar which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%define doc_phoenix %{_docdir}/%{name}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d

%else

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?mgaversion:1}0

# FIXME: brp-repack-jars uses unzip to expand jar files
# Unfortunately guice-2.0.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See BIGTOP-294
%define __os_install_post \
    %{_rpmconfigdir}/brp-compress ; \
    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
    %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}
%endif

%define doc_phoenix %{_docdir}/%{name}-%{phoenix_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif

Name: phoenix
Version: %{phoenix_version}
Release: %{phoenix_release}
Summary: Phoenix is a SQL skin over HBase and client-embedded JDBC driver.
URL: http://phoenix.apache.org
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: %{name}-%{phoenix_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_phoenix.sh
Source3: phoenix.default
Source4: bigtop.bom
Source5: %{name}-queryserver.svc
Source6: %{name}-queryserver.default
BuildArch: noarch
Requires: hadoop, hadoop-mapreduce, hadoop-yarn, hbase, zookeeper

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: coreutils
%endif

%description
Phoenix is a SQL skin over HBase, delivered as a client-embedded JDBC driver.
The Phoenix query engine transforms an SQL query into one or more HBase scans,
and orchestrates their execution to produce standard JDBC result sets. Direct
use of the HBase API, along with coprocessors and custom filters, results in
performance on the order of milliseconds for small queries, or seconds for
tens of millions of rows. Applications interact with Phoenix through a
standard JDBC interface; all the usual interfaces are supported.

%package queryserver
Summary: A stand-alone server that exposes Phoenix to thin clients
Group: Development/Libraries
Requires: phoenix = %{version}-%{release}

%description queryserver
The Phoenix Query Server provides an alternative means for interaction 
with Phoenix and HBase. Soon this will enable access from environments 
other than the JVM.

%prep
%setup -n apache-%{name}-%{phoenix_base_version}-src
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
	--build-dir=build \
        --doc-dir=%{doc_phoenix} \
	--prefix=$RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE3} $RPM_BUILD_ROOT/etc/default/%{name}
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/default/%{name}-queryserver

# Install init script
init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-queryserver
bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/%{name}-queryserver.svc rpm $init_file

%pre
getent group phoenix >/dev/null || groupadd -r phoenix
getent passwd phoenix >/dev/null || useradd -c "Phoenix" -s /sbin/nologin -g phoenix -r -d %{var_lib_phoenix} phoenix 2> /dev/null || :
    
%post
%{alternatives_cmd} --install %{etc_phoenix_conf} %{name}-conf %{etc_phoenix_conf_dist} 30


%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{name}-conf %{etc_phoenix_conf_dist} || :
fi


#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%doc %{doc_phoenix}
%{phoenix_home}/phoenix-*.jar
%{bin_phoenix}
%config(noreplace) %{etc_phoenix_conf_dist}
%config(noreplace) %{_sysconfdir}/default/phoenix

%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{name}-%1 \
%attr(0775,phoenix,phoenix) %{var_lib_phoenix} \
%attr(0775,phoenix,phoenix) %{var_log_phoenix} \
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
%service_macro queryserver
