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

%define datafu_name datafu
%define lib_datafu /usr/lib/pig

%if  %{?suse_version:1}0
%define doc_datafu %{_docdir}/datafu-doc
%else
%define doc_datafu %{_docdir}/datafu-doc-%{datafu_version}
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: pig-udf-datafu
Version: %{datafu_version}
Release: %{datafu_release}
Summary: A collection of user-defined functions for Hadoop and Pig.
URL: https://github.com/linkedin/datafu
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{datafu_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{datafu_name}-%{datafu_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{datafu_name}.sh
Requires: hadoop-client, bigtop-utils >= 0.7


%description 
DataFu is a collection of user-defined functions for working with large-scale
data in Hadoop and Pig. This library was born out of the need for a stable,
well-tested library of UDFs for data mining and statistics. It is used
at LinkedIn in many of our off-line workflows for data derived products like
"People You May Know" and "Skills".

It contains functions for: PageRank, Quantiles (median), variance, Sessionization,
Convenience bag functions (e.g., set operations, enumerating bags, etc),
Convenience utility functions (e.g., assertions, easier writing of EvalFuncs)
and more...

%prep
%setup -n apache-%{datafu_name}-incubating-sources-%{datafu_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_datafu.sh \
          --build-dir=datafu-pig/build/libs \
          --prefix=$RPM_BUILD_ROOT

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%{lib_datafu}
