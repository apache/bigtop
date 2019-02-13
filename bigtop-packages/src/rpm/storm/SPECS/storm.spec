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

%define storm_name storm
%define etc_storm_conf /etc/%{storm_name}/conf
%define conf_dir_shipped %{_sysconfdir}/%{storm_name}/
%define etc_storm_conf_dist %{etc_storm_conf}.dist

%define storm_home /usr/lib/%{storm_name}
%define storm_user_home /var/lib/%{storm_name}
%define bin_storm %{storm_home}/bin
%define lib_storm %{storm_home}
%define conf_storm %{storm_home}/conf
%define logs_storm %{storm_home}/logs
%define pids_storm %{storm_home}/pids
%define man_dir %{storm_home}/man
%define storm_username storm
#fix BUG-26074
%global debug_package %{nil}
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

%define doc_storm %{storm_name}/doc
%global initd_dir %{_sysconfdir}/rc.d/init.d
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
%define __os_install_post \
    /usr/lib/rpm/redhat/brp-compress ; \
    /usr/lib/rpm/redhat/brp-strip-static-archive %{__strip} ; \
    /usr/lib/rpm/redhat/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}
%endif


%define doc_storm %{_docdir}/%{storm_name}
%global initd_dir %{_sysconfdir}/rc.d/init.d
%define alternatives_cmd alternatives

%endif


Name: storm
Version: %{storm_base_version}
Release: %{storm_release}
Summary: Storm is a distributed, fault-tolerant, and high-performance realtime computation system that provides strong guarantees on the processing of data.
URL: http://incubator.apache.org/storm/
Group: Applications/Server
Buildroot: %{_topdir}/INSTALL/%{storm_name}-%{version}
License:  Apache License, Version 2.0
Source0: apache-%{storm_name}-%{storm_base_version}-src.tar.gz
Source1: do-component-build
Source2: install_storm.sh
Requires: zookeeper


%description
Storm is a distributed, fault-tolerant, and high-performance realtime computation system that provides strong guarantees on the processing of data.

%prep
%setup -q -n apache-%{storm_name}-%{storm_base_version}

%build
env STORM_VERSION=%{version} storm_base_version=%{storm_base_version} bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
sh %{SOURCE2} \
        --build-dir=build \
        --prefix=$RPM_BUILD_ROOT

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/log/%{storm_name}
ln -s %{_localstatedir}/log/%{storm_name} %{buildroot}/%{logs_storm}

%__install -d  -m 0755  %{buildroot}/%{_localstatedir}/run/%{storm_name}
ln -s %{_localstatedir}/run/%{storm_name} %{buildroot}/%{pids_storm}
#ln -s %{conf_storm}/storm.yaml %{buildroot}%{_sysconfdir}/%{name}/storm.yaml

%pre
getent group storm 2>&1 > /dev/null || /usr/sbin/groupadd -r storm
getent group hadoop 2>&1 > /dev/null || /usr/sbin/groupadd -r hadoop
getent passwd storm 2>&1 > /dev/null || /usr/sbin/useradd -c "STORM" -s /bin/bash -g storm -G storm, hadoop -r -m -d %{storm_user_home} storm 2> /dev/null || :

%post
%{alternatives_cmd} --install %{etc_storm_conf} %{name}-conf %{etc_storm_conf_dist} 30


#######################
#### FILES SECTION ####
#######################
%files
%defattr(-,root,root)
%{storm_home}/
#%{_sysconfdir}/%{name}/storm.yaml
%config(noreplace) %{etc_storm_conf_dist}
%defattr(-,storm,storm)
%{logs_storm}
%{pids_storm}
%dir %{_localstatedir}/log/%{storm_name}/
%dir %{_localstatedir}/run/%{storm_name}/