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

%define solr_name solr
%define solr_pkg_name %{solr_name}%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_solr %{parent_dir}/usr/lib/%{solr_name}
%define var_lib_solr %{parent_dir}/var/lib/%{solr_name}
%define etc_solr %{parent_dir}/etc/%{solr_name}

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_log_solr /var/log/%{solr_name}
%define np_var_run_solr /var/run/%{solr_name}
%define np_etc_solr /etc/%{solr_name}

%define svc_solr %{solr_name}-server
%define tomcat_deployment_solr %{etc_solr}/tomcat-conf

%if  %{?suse_version:1}0
%define doc_solr %{doc_dir}/solr-doc
%define alternatives_cmd update-alternatives
%define chkconfig_dep    aaa_base
%define service_dep      aaa_base
%global initd_dir %{_sysconfdir}/rc.d
%else
%define doc_solr %{doc_dir}/solr-doc-%{solr_version}
%define alternatives_cmd alternatives
%define chkconfig_dep    chkconfig
%define service_dep      initscripts
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: %{solr_pkg_name}
Version: %{solr_version}
Release: %{solr_release}
Summary: Apache Solr is the popular, blazing fast open source enterprise search platform
URL: http://lucene.apache.org/solr
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: solr-%{solr_base_version}-src.tgz
Source1: do-component-build 
Source2: install_%{solr_name}.sh
Source3: solr.default
Source4: solr-server.init
Source5: solrctl.sh
Source6: solr.in.sh
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
%if 0%{?fedora} >= 40
Requires: redhat-lsb-core
%else
Requires: /lib/lsb/init-functions
%endif
%endif

%description 
Solr is the popular, blazing fast open source enterprise search platform from
the Apache Lucene project. Its major features include powerful full-text
search, hit highlighting, faceted search, dynamic clustering, database
integration, rich document (e.g., Word, PDF) handling, and geospatial search.
Solr is highly scalable, providing distributed search and index replication,
and it powers the search and navigation features of many of the world's
largest internet sites.

Solr is written in Java and runs as a standalone full-text search server within
a servlet container such as Tomcat. Solr uses the Lucene Java search library at
its core for full-text indexing and search, and has REST-like HTTP/XML and JSON
APIs that make it easy to use from virtually any programming language. Solr's
powerful external configuration allows it to be tailored to almost any type of
application without Java coding, and it has an extensive plugin architecture
when more advanced customization is required.

%package server
Summary: The Solr server
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(post): %{chkconfig_dep}
Requires(preun): %{service_dep}, %{chkconfig_dep}
BuildArch: noarch

%description server
This package starts the Solr server on startup

%package doc
Summary: Documentation for Apache Solr
Group: Documentation
%description doc
This package contains the documentation for Apache Solr

%description doc
Documentation for Apache Solr

%prep
%setup -n solr-%{solr_base_version}

#BIGTOP_PATCH_COMMANDS

%build
env FULL_VERSION=%{solr_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_solr.sh \
        --build-dir=solr/build/solr-%{solr_base_version} \
        --prefix=$RPM_BUILD_ROOT \
        --distro-dir=$RPM_SOURCE_DIR \
        --doc-dir=%{doc_solr} \
        --bin-dir=%{bin_dir} \
        --man-dir=%{man_dir} \
        --etc-default=%{etc_default} \
        --lib-dir=%{usr_lib_solr} \
        --var-dir=%{var_lib_solr} \
        --etc-solr=%{etc_solr}

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/
init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{svc_solr}
%__cp %{SOURCE4} $init_file
chmod 755 $init_file

%pre
getent group solr >/dev/null || groupadd -r solr
getent passwd solr > /dev/null || useradd -c "Solr" -s /sbin/nologin -g solr -r -d %{np_var_run_solr} solr 2> /dev/null || :

%post
%{alternatives_cmd} --install %{np_etc_solr}/conf %{solr_name}-conf %{etc_solr}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{solr_name}-conf %{etc_solr}/conf.dist || :
fi

%post server
chkconfig --add %{svc_solr}

%preun server
if [ $1 = 0 ] ; then
        service %{svc_solr} stop > /dev/null 2>&1
        chkconfig --del %{svc_solr}
fi

%postun server
if [ $1 -ge 1 ]; then
        service %{svc_solr} condrestart > /dev/null 2>&1
fi

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{etc_solr}/conf.dist
%config(noreplace) %{etc_default}/solr 
%config(noreplace) %{etc_default}/solr.in.sh
%dir %{np_etc_solr}
%{usr_lib_solr}
%{bin_dir}/solrctl
%defattr(-,solr,solr,755)
%{var_lib_solr}
%{np_var_run_solr}
%{np_var_log_solr}

%files doc
%defattr(-,root,root)
%doc %{doc_solr}

%files server
%attr(0755,root,root) %{initd_dir}/%{svc_solr}
