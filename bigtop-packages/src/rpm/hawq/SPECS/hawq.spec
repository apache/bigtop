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
%define etc_hawq_conf %{_sysconfdir}/%{name}/conf
%define etc_hawq_conf_dist %{etc_hawq_conf}.dist
%define hawq_home /usr/lib/%{name}
%define bin_hawq %{hawq_home}/bin
%define lib_hawq %{hawq_home}/lib
%define conf_hawq %{hawq_home}/config
%define logs_hawq %{hawq_home}/logs
%define pids_hawq %{hawq_home}/pids
%define man_dir %{_mandir}
%define hawq_username hawq
%define vcs_tag incubator-%{name}-HAWQ-307

%define pxf %{hawq_home}/pxf

%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

# SLES is more strict anc check all symlinks point to valid path
# But we do point to a hadoop jar which is not there at build time
# (but would be at install time).
# Since our package build system does not handle dependencies,
# these symlink checks are deactivated
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%define doc_hawq %{_docdir}/%{name}
%global initd_dir %{_sysconfdir}/rc.d
%define alternatives_cmd update-alternatives

%else

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?mgaversion:1}0

# FIXME: brp-repack-jars uses unzip to expand jar files
# Unfortunately guice-2.0.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See BIGTOP-294
#%define __os_install_post \
#    %{_rpmconfigdir}/brp-compress ; \
#    %{_rpmconfigdir}/brp-strip-static-archive %{__strip} ; \
#   %{_rpmconfigdir}/brp-strip-comment-note %{__strip} %{__objdump} ; \
#   /usr/lib/rpm/brp-python-bytecompile ; \
#   %{nil}
%endif

%define doc_hawq %{_docdir}/%{name}-%{hawq_version}
%global initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif


Name: hawq
Version: %{hawq_version}
Release: %{hawq_release}
Summary: Apache Hawq (incubating) is an advanced analytics MPP database
URL: http://hawq.incubator.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{vcs_tag}.tar.gz
Source1: do-component-build
Source2: install_hawq.sh
Source3: hawq-master.svc
Source4: init.d.tmpl
Source5: hawq.default
Source6: hawq-segment.svc
Requires: coreutils, /usr/sbin/useradd, /sbin/chkconfig, /sbin/service
Requires: hadoop-hdfs, bigtop-utils >= 1.0, bigtop-tomcat

%if  0%{?mgaversion}
Requires: bsh-utils
%else
Requires: sh-utils
%endif

%description
Hawq is an open-source, distributed, MPP database engine

%package doc
Summary: Hawq Documentation
Group: Documentation
BuildArch: noarch

%description doc
Documentation for Hawq platform

%prep
%setup -n %{vcs_tag}

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
env HAWQ_VERSION=%{version} bash %{SOURCE2} \
  --build-dir=target/bin \
  --doc-dir=%{doc_hawq} \
  --conf-dir=%{etc_hawq_conf_dist} \
	--prefix=$RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%__install -d -m 0755 $RPM_BUILD_ROOT/etc/default/
%__install -m 0644 %{SOURCE5} $RPM_BUILD_ROOT/etc/default/%{name}

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/log/%{name}

ln -s %{_localstatedir}/log/%{name} %{buildroot}/%{logs_hawq}

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/run/%{name}
ln -s %{_localstatedir}/run/%{name} %{buildroot}/%{pids_hawq}

master_init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}
segment_init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{name}-segment
bash %{SOURCE4} ${RPM_SOURCE_DIR}/%{SOURCE3} rpm $master_init_file
bash %{SOURCE4} ${RPM_SOURCE_DIR}/%{SOURCE6} rpm $segment_init_file
chmod 755 $master_init_file $segment_init_file

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin

%pre
getent group hawq 2>/dev/null >/dev/null || /usr/sbin/groupadd -r hawq
getent passwd hawq 2>&1 > /dev/null || /usr/sbin/useradd -c "hawq" -s /sbin/nologin -g hawq -r -d /var/run/hawq hawq 2> /dev/null || :

%post
%{alternatives_cmd} --install %{etc_hawq_conf} %{name}-conf %{etc_hawq_conf_dist} 30
chkconfig --add %{name}

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{name}-conf %{etc_hawq_conf_dist} || :
fi

if [ $1 = 0 ] ; then
        service %{name} stop > /dev/null 2>&1
        chkconfig --del %{name}
fi

%postun
if [ $1 -ge 1 ]; then
        service %{name} condrestart >/dev/null 2>&1
fi


#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,hawq,hawq)
%attr(0755,root,root)/%{initd_dir}/%{name}
%attr(0755,root,root)/%{initd_dir}/%{name}-segment
%dir %{_localstatedir}/log/%{name}
%dir %{_localstatedir}/run/%{name}

%defattr(-,root,root)
%config(noreplace) %{_sysconfdir}/default/%{name}
%{hawq_home}
%config(noreplace) %{etc_hawq_conf_dist}

%files doc
%defattr(-,root,root)
%doc %{doc_hawq}

%clean
