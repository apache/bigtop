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
%define pig_name pig
%define etc_pig /etc/%{pig_name}
%define config_pig %{etc_pig}/conf
%define lib_pig /usr/lib/%{pig_name}
%define log_pig /var/log/%{pig_name}
%define bin_pig /usr/bin
%define pig_config_virtual pig_active_configuration
%define man_dir %{_mandir}

# CentOS 5 does not have any dist macro
# So I will suppose anything that is not Mageia or a SUSE will be a RHEL/CentOS/Fedora
%if %{!?suse_version:1}0 && %{!?mgaversion:1}0

# brp-repack-jars uses unzip to expand jar files
# Unfortunately aspectjtools-1.6.5.jar pulled by ivy contains some files and directories without any read permission
# and make whole process to fail.
# So for now brp-repack-jars is being deactivated until this is fixed.
# See CDH-2151
%define __os_install_post \
    /usr/lib/rpm/redhat/brp-compress ; \
    /usr/lib/rpm/redhat/brp-strip-static-archive %{__strip} ; \
    /usr/lib/rpm/redhat/brp-strip-comment-note %{__strip} %{__objdump} ; \
    /usr/lib/rpm/brp-python-bytecompile ; \
    %{nil}

%define doc_pig %{_docdir}/pig-%{pig_version}
%define alternatives_cmd alternatives

%endif


%if  %{?suse_version:1}0

# Only tested on openSUSE 11.4. le'ts update it for previous release when confirmed
%if 0%{suse_version} > 1130
%define suse_check \# Define an empty suse_check for compatibility with older sles
%endif

%define doc_pig %{_docdir}/pig
%define alternatives_cmd update-alternatives
%define __os_install_post \
    %{suse_check} ; \
    /usr/lib/rpm/brp-compress ; \
    %{nil}

%endif


%if  0%{?mgaversion}
%define doc_pig %{_docdir}/pig-%{pig_version}
%define alternatives_cmd update-alternatives
%endif


Name: pig
Version: %{pig_version}
Release: %{pig_release}
Summary: Pig is a platform for analyzing large data sets
License: Apache License v2.0
URL: http://pig.apache.org/
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
BuildArch: noarch
Source0: pig-%{pig_base_version}.tar.gz
Source1: do-component-build
Source2: install_pig.sh
Source3: log4j.properties
Source4: pig.1
Source5: pig.properties
Requires: hadoop, bigtop-utils

%description 
Pig is a platform for analyzing large data sets that consists of a high-level language 
for expressing data analysis programs, coupled with infrastructure for evaluating these 
programs. The salient property of Pig programs is that their structure is amenable 
to substantial parallelization, which in turns enables them to handle very large data sets.

At the present time, Pig's infrastructure layer consists of a compiler that produces 
sequences of Map-Reduce programs, for which large-scale parallel implementations already 
exist (e.g., the Hadoop subproject). Pig's language layer currently consists of a textual 
language called Pig Latin, which has the following key properties:

* Ease of programming
   It is trivial to achieve parallel execution of simple, "embarrassingly parallel" data 
   analysis tasks. Complex tasks comprised of multiple interrelated data transformations 
   are explicitly encoded as data flow sequences, making them easy to write, understand, 
   and maintain.
* Optimization opportunities
   The way in which tasks are encoded permits the system to optimize their execution 
   automatically, allowing the user to focus on semantics rather than efficiency.
* Extensibility
   Users can create their own functions to do special-purpose processing.


%prep
%setup -n pig-%{pig_base_version}

%build
env PIG_BASE_VERSION=%{pig_base_version} bash %{SOURCE1}


#########################
#### INSTALL SECTION ####
#########################
%install
%__rm -rf $RPM_BUILD_ROOT

cp $RPM_SOURCE_DIR/log4j.properties .
cp $RPM_SOURCE_DIR/pig.1 .
cp $RPM_SOURCE_DIR/pig.properties .
sh -x %{SOURCE2} \
          --build-dir=build/pig-%{pig_base_version} \
          --doc-dir=$RPM_BUILD_ROOT%{doc_pig} \
          --prefix=$RPM_BUILD_ROOT

%pre
# workaround for https://issues.cloudera.org/browse/DISTRO-223
if [ $1 -gt 1 -a -d %{lib_pig}/conf ]; then
  %__mv %{lib_pig}/conf %{lib_pig}/conf.old.`date +'%s'` || :
fi

# Manage configuration symlink
%post
%{alternatives_cmd} --install %{config_pig} pig-conf %{etc_pig}/conf.dist 30 

%preun
# If we are uninstalling pig
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove pig-conf %{etc_pig}/conf.dist || :
fi

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{etc_pig}/conf.dist
%doc %{doc_pig}
%{lib_pig}
%{bin_pig}/pig
%{man_dir}/man1/pig.1.*
