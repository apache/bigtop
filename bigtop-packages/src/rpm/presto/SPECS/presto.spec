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

%define presto_name presto
%define lib_presto /usr/lib/%{presto_name}
%define var_lib_presto /var/lib/%{presto_name}
%define var_run_presto /var/run/%{presto_name}
%define var_log_presto /var/log/%{presto_name}
%define bin_presto /usr/lib/%{presto_name}/bin
%define config_presto /etc/%{presto_name}
%define bin /usr/bin
%define man_dir /usr/share/man
%define presto_services server

%if  %{?suse_version:1}0
%define doc_presto %{_docdir}/presto
%define alternatives_cmd update-alternatives
%else
%define doc_presto %{_docdir}/presto-%{presto_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: presto
Version: %{presto_version}
Release: %{presto_release}
Summary: Distributed SQL Query Engine for Big Data
URL: https://prestodb.io/
Group: Development/Libraries
BuildArch: x86_64
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{presto_name}-%{presto_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_%{presto_name}.sh
Source4: presto-server.svc
Source5: init.d.tmpl
Source6: bigtop.bom
Source7: presto.conf
Requires: bigtop-utils >= 0.7, python
Requires(preun): /sbin/service

%global initd_dir %{_sysconfdir}/init.d

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d

%else
# Required for init scripts
Requires: /lib/lsb/init-functions

%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif

%description
Presto is an open source distributed SQL query engine for running 
interactive analytic queries against data sources of all sizes ranging 
from gigabytes to petabytes.

%package -n presto-server
Summary: Presto Server
Group: Development/Libraries
BuildArch: noarch
Requires: presto = %{version}-%{release}

%description -n presto-server
Server for Presto

%package -n presto-cli
Summary: Presto CLI
Group: Development/Libraries
BuildArch: noarch
Requires: presto = %{version}-%{release}

%description -n presto-cli
CLI for Presto

%package -n presto-jdbc
Summary: Presto JDBC Driver
Group: Development/Libraries
BuildArch: noarch

%description -n presto-jdbc
JDBC Driver for Presto

%package -n presto-doc
Summary: Presto Documentation
Group: Development/Libraries
BuildArch: noarch

%description -n presto-doc
Presto Documentation

%prep
%setup -n %{presto_name}-%{presto_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

bash $RPM_SOURCE_DIR/install_presto.sh \
          --build-dir=build/%{presto_name}-%{presto_version} \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT \
          --doc-dir=%{doc_presto}

for service in %{presto_services}
do
  # Install init script
  init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{presto_name}-${service}
  bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/presto-${service}.svc rpm $init_file
done

# Install security limits
%__install -d -m 0755 $RPM_BUILD_ROOT/etc/security/limits.d
%__install -m 0644 %{SOURCE7} $RPM_BUILD_ROOT/etc/security/limits.d/presto.conf

%pre
getent group presto >/dev/null || groupadd -r presto
getent passwd presto >/dev/null || useradd -c "Presto" -s /sbin/nologin -g presto -r -d %{var_lib_presto} presto 2> /dev/null || :

%post
%{alternatives_cmd} --install /etc/presto %{presto_name}-conf /etc/presto.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{presto_name}-conf /etc/presto.dist || :
fi

for service in %{presto_services}; do
  /sbin/service %{presto_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{presto_name}-${service} stop > /dev/null 2>&1
  fi
done

%files
%defattr(-,root,root,755)
/etc/security/limits.d/presto.conf
%config(noreplace) %{config_presto}.dist
%{lib_presto}/README.txt
%{lib_presto}/NOTICE
%{lib_presto}/bin
%{lib_presto}/lib
%{lib_presto}/plugin
%{lib_presto}/log
%{lib_presto}/etc
%attr(0755,presto,presto) %{var_lib_presto}
%attr(0755,presto,presto) %{var_log_presto}
%attr(0755,presto,presto) %{var_run_presto}
%exclude %{lib_presto}/bin/presto

%files -n presto-cli
%{bin_presto}/presto
%{bin}/presto

%files -n presto-doc
%doc %{doc_presto}

%files -n presto-jdbc
%{lib_presto}/lib/presto-jdbc*.jar

%define service_macro() \
%files -n %1 \
%attr(0755,root,root)/%{initd_dir}/%1 \
%post -n %1 \
chkconfig --add %1 \
\
%preun -n %1 \
if [ $1 = 0 ] ; then \
        service %1 stop > /dev/null 2>&1 \
        chkconfig --del %1 \
fi \
%postun -n %1 \
if [ $1 -ge 1 ]; then \
        service %1 condrestart >/dev/null 2>&1 \
fi
%service_macro presto-server
