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

%define doris_name doris
%define doris_fe_name doris-fe
%define doris_be_name doris-be
%define doris_pkg_name %{doris_name}%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_doris_fe %{parent_dir}/usr/lib/%{doris_fe_name}
%define usr_lib_doris_be %{parent_dir}/usr/lib/%{doris_be_name}
%define var_lib_doris_fe %{parent_dir}/var/lib/%{doris_fe_name}
%define var_lib_doris_be %{parent_dir}/var/lib/%{doris_be_name}
%define etc_doris %{parent_dir}/etc/%{doris_name}

%define usr_lib_hadoop %{parent_dir}/usr/lib/hadoop

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_log_doris_fe /var/log/%{doris_fe_name}
%define np_var_log_doris_be /var/log/%{doris_be_name}
%define np_var_run_doris_fe /var/run/%{doris_fe_name}
%define np_var_run_doris_be /var/run/%{doris_be_name}
%define np_etc_doris /etc/%{doris_name}

%define doris_services fe be
%define build_target_doris output

%global __python %{__python3}


%if  %{!?suse_version:1}0
%define doc_doris %{doc_dir}/%{doris_name}-%{doris_version}
%define alternatives_cmd alternatives
%global initd_dir %{_sysconfdir}/rc.d/init.d
%else
%define doc_doris %{doc_dir}/%{doris_name}-%{doris_version}
%define alternatives_cmd update-alternatives
%global initd_dir %{_sysconfdir}/rc.d
%endif

Name: %{doris_pkg_name}
Version: %{doris_version}
Release: %{doris_release}
Summary: Apache Doris is an easy-to-use, high performance and unified analytics database.
License: ASL 2.0
URL: http://doris.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
Source0: %{doris_name}-%{doris_base_version}.tar.gz
Source1: do-component-build
Source2: install_doris.sh
Source3: init.d.tmpl
Source4: bigtop.bom
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7
Requires(preun): /sbin/service
Requires: python3

%description
Apache Doris is an easy-to-use, high performance and unified analytics database.

%package fe
Summary: Provides the Apache Doris FE service.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description fe
Apache Doris FE service.

%package be
Summary: Provides the Apache Doris BE service.
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description be
Apache Doris BE service.

##############################################

%prep
%setup -n apache-%{doris_name}-%{doris_base_version}-src
#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build



# Init.d scripts
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%install
%__rm -rf $RPM_BUILD_ROOT

sh -x %{SOURCE2} \
    --prefix=$RPM_BUILD_ROOT \
    --source-dir=$RPM_SOURCE_DIR \
    --build-dir=`pwd`/%{build_target_doris} \
    --doris-fe-lib-dir=%{usr_lib_doris_fe} \
    --doris-be-lib-dir=%{usr_lib_doris_be} \
    --bin-dir=%{bin_dir} \
    --lib-hadoop=%{usr_lib_hadoop} \
    --etc-doris=%{etc_doris}

%pre
getent group doris >/dev/null || groupadd -r doris
getent passwd doris >/dev/null || useradd -c "Doris" -s /sbin/nologin -g doris -r -d %{usr_lib_doris_fe} doris 2> /dev/null || :

%post
%{alternatives_cmd} --install %{np_etc_doris}/conf %{doris_name}-conf %{etc_doris}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{doris_fe_name}-conf %{etc_doris}/conf.dist || :
fi

###### FILES ###########

%files
%defattr(-,root,root,755)
%attr(0755,doris,doris) %{np_etc_doris}
%config(noreplace) %{etc_doris}/conf.dist

%files fe
%defattr(-,root,root,755)
%config(noreplace) %{etc_doris}/conf.dist/doris-fe
%attr(0755,doris,doris) %{np_var_log_doris_fe}
%attr(0755,doris,doris) %{np_var_run_doris_fe}
%{usr_lib_doris_fe}

%files be
%defattr(-,root,root,755)
%config(noreplace) %{etc_doris}/conf.dist/doris-be
%attr(0755,doris,doris) %{np_var_log_doris_be}
%attr(0755,doris,doris) %{np_var_run_doris_be}
%{usr_lib_doris_be}

