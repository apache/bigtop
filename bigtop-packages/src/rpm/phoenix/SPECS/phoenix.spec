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
%define lib_phoenix %{phoenix_home}/lib
%define man_dir %{_mandir}
%define zookeeper_home /usr/lib/zookeeper
%define hadoop_home /usr/lib/hadoop
%define hadoop_mapreduce_home /usr/lib/hadoop-mapreduce
%define hadoop_yarn_home /usr/lib/hadoop-yarn
%define hbase_home /usr/lib/hbase

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
    /usr/lib/rpm/redhat/brp-compress ; \
    /usr/lib/rpm/redhat/brp-strip-static-archive %{__strip} ; \
    /usr/lib/rpm/redhat/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}
%endif

%define doc_phoenix %{_docdir}/%{name}-%{phoenix_version}
%define alternatives_cmd alternatives

%endif


Name: phoenix
Version: %{phoenix_version}
Release: %{phoenix_release}
Summary: Phoenix is a SQL skin over HBase and client-embedded JDBC driver.
URL: https://github.com/forcedotcom/phoenix
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{phoenix_base_version}.tar.gz
Source1: do-component-build
Source2: install_phoenix.sh
BuildArch: noarch
Requires: hadoop, hadoop-mapreduce, hadoop-yarn, hbase, zookeeper

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: sh-utils
%endif

%description
Phoenix is a SQL skin over HBase, delivered as a client-embedded JDBC driver.
The Phoenix query engine transforms an SQL query into one or more HBase scans,
and orchestrates their execution to produce standard JDBC result sets. Direct
use of the HBase API, along with coprocessors and custom filters, results in
performance on the order of milliseconds for small queries, or seconds for
tens of millions of rows. Applications interact with Phoenix through a
standard JDBC interface; all the usual interfaces are supported.


%prep
%setup -n %{name}-%{phoenix_base_version}

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} \
	--build-dir=build \
        --doc-dir=%{doc_phoenix} \
	--prefix=$RPM_BUILD_ROOT

%post
%{alternatives_cmd} --install %{etc_phoenix_conf} %{name}-conf %{etc_phoenix_conf_dist} 30

%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{name}-conf %{etc_phoenix_conf_dist} || :
fi

# Pull zookeeper, hadoop, hadoop-mapreduce, hadoop-yarn, and hbase deps from their packages
rm -f $RPM_BUILD_ROOT/%{lib_phoenix}/zookeeper*.jar
ln -f -s %{zookeeper_home}/zookeeper.jar $RPM_BUILD_ROOT/%{lib_phoenix}
rm -f $RPM_BUILD_ROOT/%{lib_phoenix}/hadoop*.jar
ln -f -s %{hadoop_home}/hadoop-annotations.jar $RPM_BUILD_ROOT/%{lib_phoenix}
ln -f -s %{hadoop_home}/hadoop-auth.jar $RPM_BUILD_ROOT/%{lib_phoenix}
ln -f -s %{hadoop_home}/hadoop-common.jar $RPM_BUILD_ROOT/%{lib_phoenix}
ln -f -s %{hadoop_mapreduce_home}/hadoop-mapreduce-client-core.jar $RPM_BUILD_ROOT/%{lib_phoenix}
ln -f -s %{hadoop_yarn_home}/hadoop-yarn-api.jar $RPM_BUILD_ROOT/%{lib_phoenix}
ln -f -s %{hadoop_yarn_home}/hadoop-yarn-common.jar $RPM_BUILD_ROOT/%{lib_phoenix}

rm -f $RPM_BUILD_ROOT/%{lib_phoenix}/hbase*.jar
ln -f -s %{hbase_home}/hbase.jar $RPM_BUILD_ROOT/%{lib_phoenix}


#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%doc %{doc_phoenix}
%{phoenix_home}/phoenix-*.jar
%{lib_phoenix}
%{bin_phoenix}
%config(noreplace) %{etc_phoenix_conf_dist}
