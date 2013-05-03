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
%define conf_hcatalog %{_sysconfdir}/%{name}/conf
%define conf_webhcat  %{_sysconfdir}/webhcat/conf
%define usr_lib_hcatalog /usr/lib/%{name}
%define bin_hcatalog /usr/bin
%define man_dir %{_mandir}
%define hcatalog_svcs hcatalog-server webhcat-server
# After we run "ant package" we'll find the distribution here
%define hcatalog_dist build/hcatalog-%{hcatalog_base_version}

%if  %{!?suse_version:1}0

%define doc_hcatalog %{_docdir}/%{name}-%{hcatalog_version}
%define initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%else

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_hcatalog %{_docdir}/%{name}
%define initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}
%endif


Name: hcatalog
Version: %{hcatalog_version}
Release: %{hcatalog_release}
Summary: Apache Hcatalog (incubating) is a data warehouse infrastructure built on top of Hadoop
License: Apache License v2.0
URL: http://incubator.apache.org/hcatalog
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: %{name}-%{hcatalog_base_version}.tar.gz
Source1: do-component-build
Source2: install_hcatalog.sh
Source3: hcatalog.1
Source4: hcatalog-server.svc
Source5: webhcat-server.svc
Source6: hcatalog-server.default
Source7: webhcat-server.default
Source8: init.d.tmpl
Requires: hadoop, hive, bigtop-utils

%description 
Apache HCatalog (incubating) is a table and storage management service for data created using Apache Hadoop.
This includes:
    * Providing a shared schema and data type mechanism.
    * Providing a table abstraction so that users need not be concerned with where or how their data is stored.
    * Providing interoperability across data processing tools such as Pig, Map Reduce, Streaming, and Hive.


%package -n webhcat
Summary: WEBHcat provides a REST-like web API for HCatalog and related Hadoop components.
Group: Development/Libraries
Requires: hcatalog = %{version}-%{release}

%description -n webhcat
WEBHcat provides a REST-like web API for HCatalog and related Hadoop components.


%package server
Summary: Server for HCatalog.
Group: System/Daemons
Requires: hcatalog = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%endif

%if  0%{?mgaversion}
# Required for init scripts
Requires: initscripts
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description server
Server for HCatalog.

%package -n webhcat-server
Summary: Server for WEBHcat.
Group: System/Daemons
Requires: webhcat = %{version}-%{release}

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%endif

%if  0%{?mgaversion}
# Required for init scripts
Requires: initscripts
%endif

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0
# Required for init scripts
Requires: redhat-lsb
%endif

%description -n webhcat-server
Server for WEBHcat.

%prep
%setup -n %{name}-src-%{hcatalog_base_version}

%build
env bash %{SOURCE1}

#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT
cp $RPM_SOURCE_DIR/hcatalog.1 .
/bin/bash %{SOURCE2} \
  --prefix=$RPM_BUILD_ROOT \
  --build-dir=%{hcatalog_dist} \
  --doc-dir=$RPM_BUILD_ROOT/%{doc_hcatalog}

# Install init script
for svc in %{hcatalog_svcs} ; do
  bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/$svc.svc rpm \
       $RPM_BUILD_ROOT/%{initd_dir}/$svc
done

# Install defaults
%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE6} $RPM_BUILD_ROOT/etc/default/hcatalog-server
%__install -m 0644 %{SOURCE7} $RPM_BUILD_ROOT/etc/default/webhcat-server

# Service file management RPMs
%define service_macro() \
%files -n %1 \
%defattr(-,root,root) \
%{initd_dir}/%1 \
%config(noreplace) /etc/default/%1 \
%post -n %1 \
chkconfig --add %1 \
\
%preun -n %1 \
if [ $1 = 0 ]; then \
  service %1 stop > /dev/null 2>&1 \
  chkconfig --del %1 \
fi \
%postun -n %1 \
if [ $1 -ge 1 ]; then \
  service %1 condrestart >/dev/null 2>&1 \
fi

%service_macro hcatalog-server
%service_macro webhcat-server

%pre
getent group hcatalog >/dev/null || groupadd -r hcatalog || :
getent passwd hcatalog >/dev/null || useradd -c "HCatalog User" -s /sbin/nologin -g hcatalog -r -d /var/run/hcatalog hcatalog 2> /dev/null || :

%post
%{alternatives_cmd} --install %{conf_hcatalog} hcatalog-conf %{conf_hcatalog}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove hcatalog-conf %{conf_hcatalog}.dist || :
fi

%post -n webhcat
%{alternatives_cmd} --install %{conf_webhcat} webhcat-conf %{conf_webhcat}.dist 30

%preun -n webhcat
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove webhcat-conf %{conf_webhcat}.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root,755)
%config(noreplace) %attr(755,root,root) %{conf_hcatalog}.dist
%{usr_lib_hcatalog}/bin
%{usr_lib_hcatalog}/etc/hcatalog
%{usr_lib_hcatalog}/libexec
%{usr_lib_hcatalog}/share/hcatalog
%{usr_lib_hcatalog}/share/doc
%{usr_lib_hcatalog}/sbin/hcat_server.sh
%{usr_lib_hcatalog}/sbin/update-hcatalog-env.sh
%{bin_hcatalog}/hcat
%doc %{doc_hcatalog}
%{man_dir}/man1/hcatalog.1.*
%attr(755,hcatalog,hcatalog) /var/run/hcatalog
%attr(755,hcatalog,hcatalog) /var/log/hcatalog

%files -n webhcat
%defattr(-,root,root,755)
%config(noreplace) %attr(755,root,root) %{conf_webhcat}.dist
%{usr_lib_hcatalog}/share/webhcat
%{usr_lib_hcatalog}/etc/webhcat
%{usr_lib_hcatalog}/sbin/webhcat_config.sh
%{usr_lib_hcatalog}/sbin/webhcat_server.sh
