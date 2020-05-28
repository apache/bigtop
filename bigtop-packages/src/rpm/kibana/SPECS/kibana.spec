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

%define pkg_name kibana
%define lib_kibana /usr/lib/%{pkg_name}
%define etc_kibana /etc/%{pkg_name}
%define config_kibana %{etc_kibana}/conf
%define log_kibana /var/log/%{pkg_name}
%define bin_kibana /usr/bin
%define man_kibana /usr/share/man
%define run_kibana /var/run/%{pkg_name}
%define _unpackaged_files_terminate_build 0

%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%define chkconfig_dep    aaa_base
%define service_dep      aaa_base
%global initd_dir %{_sysconfdir}/rc.d
%else
%define alternatives_cmd alternatives
%define chkconfig_dep    chkconfig
%define service_dep      initscripts
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: kibana
Version: %{kibana_version}
Release: %{kibana_release}
Summary: Kibana is a browser-based analytics and search dashboard.
URL: https://www.elastic.co/kibana
Group: Application/Internet
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: kibana-%{kibana_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: kibana.default
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7
AutoProv: no
AutoReqProv: no

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: /lib/lsb/init-functions
Requires: initscripts
%endif

%description
Kibana is a free and open user interface that lets you visualize your Elasticsearch data
and navigate the Elastic Stack. Do anything from tracking query load to
understanding the way requests flow through your apps.

%prep
%setup -n kibana-%{kibana_base_version}

#BIGTOP_PATCH_COMMANDS
%build
env FULL_VERSION=%{kibana_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_kibana.sh \
          --build-dir=build \
          --prefix=$RPM_BUILD_ROOT \
          --distro-dir=$RPM_SOURCE_DIR \

ln -sf %{lib_kibana}/bin/* ${RPM_BUILD_ROOT}%{bin_kibana}

%pre
getent group kibana >/dev/null || groupadd -r kibana
getent passwd kibana > /dev/null || useradd -c "Kibana" -s /sbin/nologin -g kibana -r -d %{run_kibana} kibana 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_kibana} %{kibana_name}-conf %{config_kibana}.dist 30
/usr/bin/chown -R root:kibana /etc/kibana

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{kibana_name}-conf %{config_kibana}.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root,755)
%config(noreplace) %{config_kibana}.dist
%{lib_kibana}
%{bin_kibana}

%defattr(-,kibana,kibana,755)
/var/lib/kibana
/var/run/kibana
/var/log/kibana
