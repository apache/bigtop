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

%define lib_giraph /usr/lib/giraph
%define conf_giraph %{_sysconfdir}/%{name}/conf

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

%define doc_giraph %{_docdir}/giraph
%global initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%else

%define doc_giraph %{_docdir}/giraph-%{giraph_version}
%global initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif


Name: giraph
Version: %{giraph_version}
Release: %{giraph_release}
Summary: Giraph is a BSP inspired graph processing platform that runs on Hadoop
URL: http://incubator.apache.org/giraph/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: %{name}-%{giraph_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: giraph-site.xml
Buildarch: noarch
Requires: zookeeper, hadoop-client, bigtop-utils >= 0.7

%description 
Giraph implements a graph processing platform to run large scale algorithms (such as page rank, shared connections, personalization-based popularity, etc.) on top of Hadoop infrastructure. Giraph builds upon the graph-oriented nature of Pregel but additionally adds fault-tolerance to the coordinator process with the use of ZooKeeper as its centralized coordination service.

%package doc
Summary: Documentation for Apache Giraph
Group: Documentation
%description doc
This package contains the documentation for Apache Giraph

%description doc
Documentation for Apache Solr

%prep
%setup -n %{name}-%{giraph_base_version}

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
          --build-dir=`pwd` \
          --conf-dir=%{conf_giraph}.dist \
          --doc-dir=%{doc_giraph} \
          --prefix=$RPM_BUILD_ROOT
# Workaround for GIRAPH-198
%__cp -f %{SOURCE3} $RPM_BUILD_ROOT/etc/giraph/conf.dist

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%post
%{alternatives_cmd} --install %{conf_giraph} %{name}-conf %{conf_giraph}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{conf_giraph} || :
fi

# Files for main package
%files 
%defattr(0755,root,root)
%{lib_giraph}
%{_bindir}/giraph
%config(noreplace) %{conf_giraph}.dist

%files doc
%defattr(-,root,root)
%doc %{doc_giraph}
