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

%define pkg_name elasticsearch
%define lib_elasticsearch /usr/lib/%{pkg_name}
%define etc_elasticsearch /etc/%{pkg_name}
%define config_elasticsearch %{etc_elasticsearch}/conf
%define log_elasticsearch /var/log/%{pkg_name}
%define bin_elasticsearch /usr/lib/%{pkg_name}/bin
%define man_elasticsearch /usr/share/man
%define run_elasticsearch /var/run/%{pkg_name}

%if  %{?suse_version:1}0
%define doc_elasticsearch %{_docdir}/elasticsearch-doc
%define alternatives_cmd update-alternatives
%define chkconfig_dep    aaa_base
%define service_dep      aaa_base
%global initd_dir %{_sysconfdir}/rc.d
%else
%define doc_elasticsearch %{_docdir}/elasticsearch
%define alternatives_cmd alternatives
%define chkconfig_dep    chkconfig
%define service_dep      initscripts
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: elasticsearch
Version: %{elasticsearch_version}
Release: %{elasticsearch_release}
Summary: Elasticsearch is a distributed RESTful search engine based on the Lucene library.
URL: https://www.elastic.co/
Group: Application/Internet
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: elasticsearch-%{elasticsearch_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{name}.sh
Source3: elasticsearch.default
Source4: elasticsearch.init
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: /lib/lsb/init-functions
Requires: initscripts
%endif

%description 
Elasticsearch is a search engine based on the Lucene library.
It provides a distributed, multitenant-capable full-text search engine with
an HTTP web interface and schema-free JSON documents.

%prep
%setup -n elasticsearch-%{elasticsearch_base_version}

#BIGTOP_PATCH_COMMANDS

%build
env FULL_VERSION=%{elasticsearch_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_elasticsearch.sh \
          --build-dir=build \
          --prefix=$RPM_BUILD_ROOT \
          --distro-dir=$RPM_SOURCE_DIR \
          --initd-dir=%{initd_dir} \
          --doc-dir=%{doc_elasticsearch}

%pre
getent group elasticsearch >/dev/null || groupadd -r elasticsearch
getent passwd elasticsearch > /dev/null || useradd -c "Elasticsearch" -s /sbin/nologin -g elasticsearch -r -d %{run_elasticsearch} elasticsearch 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_elasticsearch} %{elasticsearch_name}-conf %{config_elasticsearch}.dist 30
/usr/bin/chown -R root:elasticsearch /etc/elasticsearch

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{elasticsearch_name}-conf %{config_elasticsearch}.dist || :
fi


#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_elasticsearch}.dist
%config(noreplace) /etc/default/elasticsearch 
%{initd_dir}/elasticsearch
%{lib_elasticsearch}
%defattr(-,elasticsearch,elasticsearch,755)
/var/lib/elasticsearch
/var/run/elasticsearch
/var/log/elasticsearch
