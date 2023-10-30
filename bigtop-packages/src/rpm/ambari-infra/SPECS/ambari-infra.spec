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


%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0
# disable repacking jars
%define __os_install_post %{nil}
%define __jar_repack %{nil}

%define ambari_infra_name ambari-infra

Name: ambari-infra
Version: %{ambari_infra_version}
Release: %{ambari_infra_release}
Summary: Ambari Infra
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/apache-%{ambari_infra_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: apache-%{ambari_infra_name}-%{ambari_infra_base_version}.tar.gz
Source1: do-component-build 
Source2: install_infra.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n apache-%{ambari_infra_name}-%{ambari_infra_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
AMBARI_VERSION=%{ambari_version} bash $RPM_SOURCE_DIR/install_infra.sh \
          --build-dir=`pwd` \
          --distro-dir=$RPM_SOURCE_DIR \
          --source-dir=`pwd` \
          --prefix=$RPM_BUILD_ROOT


%package solr
Summary: Ambari Infra Solr
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description solr
Ambari Infra Solr

%post solr
/bin/chmod -R 755 /usr/lib/ambari-infra-solr/bin/solr
/bin/chmod -R 755 /usr/lib/ambari-infra-solr/server/scripts

%package solr-client
Summary: Ambari Infra Solr Client
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description solr-client
Ambari Infra Solr Client

%post solr-client
/bin/chmod -R 755 /usr/lib/ambari-infra-solr-client

SOLR_CLOUD_CLI_LINK_NAME="/usr/bin/infra-solr-cloud-cli"
SOLR_CLOUD_CLI_SOURCE="/usr/lib/ambari-infra-solr-client/solrCloudCli.sh"
SOLR_INDEX_TOOL_LINK_NAME="/usr/bin/infra-lucene-index-tool"
SOLR_INDEX_TOOL_SOURCE="/usr/lib/ambari-infra-solr-client/solrIndexHelper.sh"
SOLR_DATA_MANAGER_LINK_NAME="/usr/bin/infra-solr-data-manager"
SOLR_DATA_MANAGER_SOURCE="/usr/lib/ambari-infra-solr-client/solrDataManager.py"

rm -f $SOLR_CLOUD_CLI_LINK_NAME ; ln -s $SOLR_CLOUD_CLI_SOURCE $SOLR_CLOUD_CLI_LINK_NAME
rm -f $SOLR_INDEX_TOOL_LINK_NAME ; ln -s $SOLR_INDEX_TOOL_SOURCE $SOLR_INDEX_TOOL_LINK_NAME
rm -f $SOLR_DATA_MANAGER_LINK_NAME ; ln -s $SOLR_DATA_MANAGER_SOURCE $SOLR_DATA_MANAGER_LINK_NAME


%package manager
Summary: Ambari Infra Manager
Group: Development/Libraries
Requires: openssl, python3 >= 3.0
AutoProv: no
AutoReqProv: no
%description manager
Ambari Infra Manager

%post manager
INFRA_MANAGER_LINK_NAME="/usr/bin/infra-manager"
INFRA_MANAGER_SOURCE="/usr/lib/ambari-infra-manager/bin/infraManager.sh"
INFRA_MANAGER_CONF_LINK_DIR="/etc/ambari-infra-manager"
INFRA_MANAGER_CONF_LINK_NAME="$INFRA_MANAGER_CONF_LINK_DIR/conf"
INFRA_MANAGER_CONF_SOURCE="/usr/lib/ambari-infra-manager/conf"
rm -f $INFRA_MANAGER_LINK_NAME ; ln -s $INFRA_MANAGER_SOURCE $INFRA_MANAGER_LINK_NAME
rm -f $INFRA_MANAGER_CONF_LINK_NAME
rm -rf $INFRA_MANAGER_CONF_LINK_DIR
mkdir -p $INFRA_MANAGER_CONF_LINK_DIR
ln -s $INFRA_MANAGER_CONF_SOURCE $INFRA_MANAGER_CONF_LINK_NAME

%postun manager
INFRA_MANAGER_CONF_LINK_DIR="/etc/ambari-infra-manager"
INFRA_MANAGER_CONF_LINK_NAME="$INFRA_MANAGER_CONF_LINK_DIR/conf"
INFRA_MANAGER_LINK_NAME="/usr/bin/infra-manager"
rm -f $INFRA_MANAGER_LINK_NAME
rm -f $INFRA_MANAGER_CONF_LINK_NAME
rm -rf $INFRA_MANAGER_CONF_LINK_DIR


#######################
#### FILES SECTION ####
#######################

%files solr
%defattr(644,root,root,755)
/usr/lib/ambari-infra-solr


%files solr-client
%defattr(644,root,root,755)
/usr/lib/ambari-infra-solr-client

%files manager
%defattr(644,root,root,755)
/usr/lib/ambari-infra-manager


