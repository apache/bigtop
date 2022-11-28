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

%define zeppelin_name zeppelin
%define zeppelin_pkg_name zeppelin%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_zeppelin %{parent_dir}/usr/lib/%{zeppelin_name}
%define var_lib_zeppelin %{parent_dir}/var/lib/%{zeppelin_name}
%define etc_zeppelin_conf_dist %{parent_dir}/etc/%{zeppelin_name}/conf.dist

%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}
%define lib_dir %{parent_dir}/%{_libdir}

# No prefix directory
%define np_var_log_zeppelin /var/log/%{zeppelin_name}
%define np_var_run_zeppelin /var/run/%{zeppelin_name}
%define np_etc_zeppelin /etc/%{zeppelin_name}

%if  %{?suse_version:1}0
%define doc_zeppelin %{doc_dir}/%{zeppelin_name}
%define alternatives_cmd update-alternatives
%else
%define doc_zeppelin %{doc_dir}/%{zeppelin_name}-%{zeppelin_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}
%define __jar_repack ${nil}

Name: %{zeppelin_pkg_name}
Version: %{zeppelin_version}
Release: %{zeppelin_release}
Summary: Web-based notebook for Apache Spark
URL: http://zeppelin.apache.org/
Group: Applications/Engineering
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{zeppelin_name}-%{zeppelin_base_version}.tar.gz
Source1: bigtop.bom
Source2: do-component-build
Source3: init.d.tmpl
Source4: install_zeppelin.sh
Source5: zeppelin-env.sh
Source6: zeppelin.svc
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7, hadoop-client, spark-core >= 1.5, spark-python >= 1.5
Requires(preun): /sbin/service
AutoReq: no

%global debug_package %{nil}
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
Zeppelin is a web-based notebook that enables interactive data analytics with Apache Spark.
You can make beautiful data-driven, interactive and collaborative documents with SQL, Scala and more.

%prep
%setup -n %{zeppelin_name}-%{zeppelin_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT

# Init.d scripts directory
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

bash $RPM_SOURCE_DIR/install_zeppelin.sh \
  --build-dir=`pwd`         \
  --source-dir=$RPM_SOURCE_DIR \
  --prefix=$RPM_BUILD_ROOT  \
  --doc-dir=%{doc_zeppelin} \
  --lib-dir=%{usr_lib_zeppelin} \
  --var-dir=%{var_lib_zeppelin} \
  --man-dir=%{man_dir} \
  --conf-dist-dir=%{etc_zeppelin_conf_dist}

# Install init script
initd_script=$RPM_BUILD_ROOT/%{initd_dir}/%{zeppelin_name}
bash %{SOURCE3} $RPM_SOURCE_DIR/%{zeppelin_name}.svc rpm $initd_script

%pre
getent group zeppelin >/dev/null || groupadd -r zeppelin
getent passwd zeppelin >/dev/null || useradd -c "Zeppelin" -s /sbin/nologin -g zeppelin -r -d %{var_lib_zeppelin} zeppelin 2> /dev/null || :

%post
%{alternatives_cmd} --install %{np_etc_zeppelin}/conf %{zeppelin_name}-conf %{etc_zeppelin_conf_dist} 30
chkconfig --add %{zeppelin_name}

%preun
if [ "$1" = 0 ]; then
  %{alternatives_cmd} --remove %{zeppelin_name}-conf %{etc_zeppelin_conf_dist} || :
fi

/sbin/service %{zeppelin_name} status > /dev/null 2>&1
if [ $? -eq 0 ]; then
  service %{zeppelin_name} stop > /dev/null 2>&1
fi
chkconfig --del %{zeppelin_name}

#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root,755)
%doc %{doc_zeppelin}
%{usr_lib_zeppelin}/*.war
%{usr_lib_zeppelin}/bin
%{usr_lib_zeppelin}/plugins
%{usr_lib_zeppelin}/conf
%{usr_lib_zeppelin}/interpreter
%{usr_lib_zeppelin}/lib
%config(noreplace) %attr(0755,zeppelin,zeppelin) %{etc_zeppelin_conf_dist}
%attr(0755,zeppelin,zeppelin) %{np_etc_zeppelin}
%attr(0755,zeppelin,zeppelin) %{var_lib_zeppelin}
%attr(0755,zeppelin,zeppelin) %{np_var_run_zeppelin}
%attr(0755,zeppelin,zeppelin) %{np_var_log_zeppelin}
%attr(0755,root,root)/%{initd_dir}/%{zeppelin_name}
