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

%define pkg_name logstash
%define lib_logstash /usr/lib/%{pkg_name}
%define etc_logstash /etc/%{pkg_name}
%define config_logstash %{etc_logstash}/conf
%define log_logstash /var/log/%{pkg_name}
%define bin_logstash /usr/lib/%{pkg_name}/bin
%define man_logstash /usr/share/man
%define run_logstash /var/run/%{pkg_name}

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

# Disable debuginfo package
%define debug_package %{nil}

Name: logstash
Version: %{logstash_version}
Release: %{logstash_release}
Summary: Logstash is a server-side data processing pipeline that ingests data from a multitude of sources simultaneously.
URL: https://www.elastic.co/logstash
Group: Application/Internet
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: logstash-%{logstash_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{name}.sh
Source3: log4j2.properties
Source4: logstash.default
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
Logstash is an open source, server-side data processing pipeline
that ingests data from a multitude of sources simultaneously,
transforms it, and then sends it to your favorite "stash."

%prep
%setup -n logstash-%{logstash_base_version}

%build
env FULL_VERSION=%{logstash_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_logstash.sh \
          --build-dir=build \
          --prefix=$RPM_BUILD_ROOT \
          --distro-dir=$RPM_SOURCE_DIR

%pre
getent group logstash >/dev/null || groupadd -r logstash
getent passwd logstash > /dev/null || useradd -c "Logstash" -s /sbin/nologin -g logstash -r -d %{run_logstash} logstash 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_logstash} %{logstash_name}-conf %{config_logstash}.dist 30
/usr/bin/chown -R root:logstash /etc/logstash

%preun
if [ "$1" = 0 ]; then
    %{alternatives_cmd} --remove %{logstash_name}-conf %{config_logstash}.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root,755)
%config(noreplace) %{config_logstash}.dist
%{lib_logstash}
%defattr(-,logstash,logstash,755)
/var/lib/logstash
/var/run/logstash
/var/log/logstash
