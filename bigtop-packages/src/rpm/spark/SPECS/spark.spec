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

%define spark_name spark
%define lib_spark /usr/lib/%{spark_name}
%define var_lib_spark /var/lib/%{spark_name}
%define var_run_spark /var/run/%{spark_name}
%define var_log_spark /var/log/%{spark_name}
%define bin_spark /usr/lib/%{spark_name}/bin
%define etc_spark /etc/%{spark_name}
%define config_spark %{etc_spark}/conf
%define bin /usr/bin
%define man_dir /usr/share/man
%define spark_services master worker

%if  %{?suse_version:1}0
%define doc_spark %{_docdir}/spark
%define alternatives_cmd update-alternatives
%else
%define doc_spark %{_docdir}/spark-%{spark_version}
%define alternatives_cmd alternatives
%endif

%define pyspark_python python


# disable repacking jars
%define __os_install_post %{nil}

Name: spark-core
Version: %{spark_version}
Release: %{spark_release}
Summary: Lightning-Fast Cluster Computing
URL: http://spark.incubator.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{spark_name}-%{spark_base_version}.tar.gz
Source1: do-component-build 
Source2: install_%{spark_name}.sh
Source3: spark-master.svc
Source4: spark-worker.svc
Requires: bigtop-utils
Requires(preun): /sbin/service

%global initd_dir %{_sysconfdir}/init.d

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d

%else
# Required for init scripts
Requires: redhat-lsb

%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif

%description 
Spark is a MapReduce-like cluster computing framework designed to support
low-latency iterative jobs and interactive use from an interpreter. It is
written in Scala, a high-level language for the JVM, and exposes a clean
language-integrated syntax that makes it easy to write parallel jobs.
Spark runs on top of the Apache Mesos cluster manager.

%package master
Summary: Server for Spark master
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description master 
Server for Spark master

%package worker
Summary: Server for Spark worker
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description worker 
Server for Spark worker

%package python
Summary: Python client for Spark
Group: Development/Libraries
Requires: spark = %{version}-%{release}, %{pyspark_python}

%description python
Includes PySpark, an interactive Python shell for Spark, and related libraries

%prep
%setup -n %{spark_name}-%{spark_base_version}

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{bin_spark}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/lib/%{spark_name}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/log/%{spark_name}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/run/%{spark_name}/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{_localstatedir}/run/%{spark_name}/work/
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

sh $RPM_SOURCE_DIR/install_spark.sh \
          --build-dir=`pwd`         \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT  \
          --doc-dir=%{doc_spark} \
          --pyspark-python=%{pyspark_python}

for service in %{spark_services}
do
    # Install init script
    init_file=$RPM_BUILD_ROOT/%{initd_dir}/%{spark_name}-${service}
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/spark-${service}.svc rpm $init_file
done

%pre
getent group spark >/dev/null || groupadd -r spark
getent passwd spark >/dev/null || useradd -c "Spark" -s /sbin/nologin -g spark -r -d %{var_lib_spark} spark 2> /dev/null || :

%post
%{alternatives_cmd} --install %{config_spark} %{spark_name}-conf %{config_spark}.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{spark_name}-conf %{config_spark}.dist || :
fi

for service in %{spark_services}; do
  /sbin/service %{spark_name}-${service} status > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    /sbin/service %{spark_name}-${service} stop > /dev/null 2>&1
  fi
done

#######################
#### FILES SECTION ####
#######################
%files 
%defattr(-,root,root,755)
%config(noreplace) %{config_spark}.dist
%doc %{doc_spark}
%{lib_spark}
%exclude %{lib_spark}/pyspark
%exclude %{lib_spark}/python
%{etc_spark}
%attr(0755,spark,spark) %{var_lib_spark}
%attr(0755,spark,spark) %{var_run_spark}
%attr(0755,spark,spark) %{var_log_spark}
%attr(0755,root,root) %{bin_spark}
%{bin}/spark-shell
%{bin}/spark-executor

%files python
%defattr(-,root,root,755)
%attr(0755,root,root) %{bin}/pyspark
%attr(0755,root,root) %{lib_spark}/pyspark
%{lib_spark}/python

%define service_macro() \
%files %1 \
%attr(0755,root,root)/%{initd_dir}/%{spark_name}-%1 \
%post %1 \
chkconfig --add %{spark_name}-%1 \
\
%preun %1 \
if [ $1 = 0 ] ; then \
        service %{spark_name}-%1 stop > /dev/null 2>&1 \
        chkconfig --del %{spark_name}-%1 \
fi \
%postun %1 \
if [ $1 -ge 1 ]; then \
        service %{spark_name}-%1 condrestart >/dev/null 2>&1 \
fi
%service_macro master
%service_macro worker
