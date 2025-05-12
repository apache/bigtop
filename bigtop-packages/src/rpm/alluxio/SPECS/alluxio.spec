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
%define        alluxio_name alluxio

%define alluxio_pkg_name alluxio%{pkg_name_suffix}
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

%define etc_default %{parent_dir}/etc/default
%define usr_lib_alluxio %{parent_dir}/usr/lib/%{alluxio_name}
%define etc_alluxio %{parent_dir}/etc/%{alluxio_name}
%define etc_alluxio_conf_dist %{parent_dir}/etc/%{alluxio_name}/conf.dist
%define bin_dir %{parent_dir}/%{_bindir}

%define libexec_dir %{usr_lib_alluxio}/libexec

%define np_var_lib_alluxio_data /var/lib/%{alluxio_name}/data
%define np_var_run_alluxio /var/run/%{alluxio_name}
%define np_var_log_alluxio /var/log/%{alluxio_name}
%define np_etc_alluxio /etc/%{alluxio_name}


%define        alluxio_home %{parent_dir}/usr/lib/%{alluxio_name}
%define        alluxio_services master worker job-master job-worker

%global        initd_dir %{_sysconfdir}/init.d



Name:           %{alluxio_pkg_name}
Version:        %{alluxio_version}
Release:        %{alluxio_release}
Summary:       Reliable file sharing at memory speed across cluster frameworks
License:       ASL 2.0
URL:           http://www.alluxio.org/
Group:         Development/Libraries
# BuildArch:     noarch

Source0:       %{alluxio_name}-%{alluxio_base_version}.tar.gz
Source1:       do-component-build
Source2:       install_alluxio.sh
Source3:       init.d.tmpl
Source4:       alluxio-master.svc
Source5:       alluxio-worker.svc
Source6:       alluxio-job-master.svc
Source7:       alluxio-job-worker.svc
#BIGTOP_PATCH_FILES

%if  %{?suse_version:1}0
%define alternatives_cmd update-alternatives
%else
%define alternatives_cmd alternatives
%endif

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global        initd_dir %{_sysconfdir}/rc.d
%else
# Required for init scripts
%if 0%{?fedora} >= 40
Requires: redhat-lsb-core
%else
Requires: /lib/lsb/init-functions
%endif
Requires: initscripts
%global        initd_dir %{_sysconfdir}/rc.d/init.d
%endif

Requires: bigtop-utils

# disable repacking jars
%define __os_install_post %{nil}
%define __jar_repack %{nil}
%define  debug_package %{nil}

# disable debug package
%define debug_package %{nil}

%description
Alluxio is a fault tolerant distributed file system
enabling reliable file sharing at memory-speed
across cluster frameworks, such as Spark and MapReduce.
It achieves high performance by leveraging lineage
information and using memory aggressively.
Alluxio caches working set files in memory, and
enables different jobs/queries and frameworks to
access cached files at memory speed. Thus, Alluxio
avoids going to disk to load data-sets that
are frequently read.

%prep
%setup -n %{alluxio_name}-%{alluxio_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
rm -rf $RPM_BUILD_ROOT

# See /usr/lib/rpm/macros for info on how vars are defined.
# Here we run the alluxio installation script.
bash %{SOURCE2} \
    --build-dir=%{buildroot} \
    --lib-dir=%{usr_lib_alluxio}  \
    --bin-dir=%{bin_dir} \
    --data-dir=%{_datadir} \
    --libexec-dir=%{libexec_dir} \
    --prefix="${RPM_BUILD_ROOT}" \
    --conf-dist-dir=%{etc_alluxio_conf_dist}

for service in %{alluxio_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{alluxio_name}-${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/alluxio-${service}.svc rpm $init_file
done

%pre
getent group alluxio >/dev/null || groupadd -r alluxio
getent passwd alluxio >/dev/null || useradd -c "Alluxio" -s /sbin/nologin -g alluxio -r -d %{var_lib} alluxio 2> /dev/null || :

%post
%{alternatives_cmd} --install %{np_etc_alluxio}/conf %{alluxio_name}-conf %{etc_alluxio}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{alluxio_name}-conf %{etc_alluxio}/conf.dist || :
fi

for service in %{alluxio_services}; do
  /sbin/service %{alluxio_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{alluxio_name}-${service} stop > /dev/null 2>&1
  fi
done


%files
%defattr(-,root,root,-)
%doc LICENSE README.md
%dir %{np_etc_alluxio}
%config(noreplace) %{etc_alluxio}/conf.dist

%config(noreplace) %{initd_dir}/%{alluxio_name}-master
%config(noreplace) %{initd_dir}/%{alluxio_name}-worker
%config(noreplace) %{initd_dir}/%{alluxio_name}-job-master
%config(noreplace) %{initd_dir}/%{alluxio_name}-job-worker

%attr(0755,alluxio,alluxio) %{np_var_run_alluxio}
%attr(0755,alluxio,alluxio) %{np_var_log_alluxio}
%{alluxio_home}
%{_datadir}/%{alluxio_name}
%{bin_dir}/alluxio


%clean
rm -rf $RPM_BUILD_ROOT
