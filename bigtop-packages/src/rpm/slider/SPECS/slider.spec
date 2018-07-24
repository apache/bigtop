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

%define slider_name slider
%define lib_slider /usr/lib/slider

%if  %{?suse_version:1}0
%define doc_slider %{_docdir}/slider-doc
%else
%define doc_slider %{_docdir}/slider-doc-%{slider_version}
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: slider
Version: %{slider_version}
Release: %{slider_release}
Summary: A application to deploy existing distributed applications on an Apache Hadoop YARN cluster.
URL: https://github.com/linkedin/slider
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{slider_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{slider_name}-%{slider_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{slider_name}.sh
Requires: bigtop-utils >= 0.7


%description 
Apache Slider is a application to deploy existing distributed applications on an Apache Hadoop YARN cluster,
monitor them and make them larger or smaller as desired -even while the application is running.

%prep
%setup -n apache-%{slider_name}-%{slider_base_version}-incubating

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{slider_version}

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config /etc/slider
%{lib_slider}
