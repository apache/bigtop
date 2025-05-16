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
%define spark_pkg_name spark%{pkg_name_suffix}
%define hadoop_pkg_name hadoop%{pkg_name_suffix}

%define etc_default %{parent_dir}/etc/default

%define usr_lib_spark %{parent_dir}/usr/lib/%{spark_name}
%define var_lib_spark %{parent_dir}/var/lib/%{spark_name}
%define etc_spark %{parent_dir}/etc/%{spark_name}

%define bin_dir %{parent_dir}/%{_bindir}
%define man_dir %{parent_dir}/%{_mandir}
%define doc_dir %{parent_dir}/%{_docdir}

# No prefix directory
%define np_var_log_spark /var/log/%{spark_name}
%define np_var_run_spark /var/run/%{spark_name}
%define np_etc_spark /etc/%{spark_name}

%define spark_services master worker history-server thriftserver

%if  %{?suse_version:1}0
%define doc_spark %{doc_dir}/spark
%define alternatives_cmd update-alternatives
%else
%define doc_spark %{doc_dir}/spark-%{spark_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: %{spark_pkg_name}
Version: %{spark_version}
Release: %{spark_release}
Summary: Lightning-Fast Cluster Computing
URL: http://spark.apache.org/
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0
Source0: %{spark_name}-%{spark_base_version}.tar.gz
Source1: do-component-build
Source2: install_%{spark_name}.sh
Source3: spark-master.svc
Source4: spark-worker.svc
Source6: init.d.tmpl
Source7: spark-history-server.svc
Source8: spark-thriftserver.svc
Source9: bigtop.bom
#BIGTOP_PATCH_FILES
Requires: bigtop-utils >= 0.7, %{hadoop_pkg_name}-client, %{hadoop_pkg_name}-yarn
Requires(preun): /sbin/service

%global initd_dir %{_sysconfdir}/init.d

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%global initd_dir %{_sysconfdir}/rc.d
%else
# Required for init scripts
%if 0%{?fedora} >= 40
Requires: redhat-lsb-core
%else
Requires: /lib/lsb/init-functions
%endif
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

%description 
Spark is a MapReduce-like cluster computing framework designed to support
low-latency iterative jobs and interactive use from an interpreter. It is
written in Scala, a high-level language for the JVM, and exposes a clean
language-integrated syntax that makes it easy to write parallel jobs.
Spark runs on top of the Apache Mesos cluster manager.

%package -n %{spark_pkg_name}-core
Summary: Spark core
Group: Development/Libraries
Requires: %{spark_pkg_name} = %{version}-%{release}

%description -n %{spark_pkg_name}-core
Spark core

%package -n %{spark_pkg_name}-master
Summary: Server for Spark master
Group: Development/Libraries
Requires: %{spark_pkg_name}-core = %{version}-%{release}

%description -n %{spark_pkg_name}-master
Server for Spark master

%package -n %{spark_pkg_name}-worker
Summary: Server for Spark worker
Group: Development/Libraries
Requires: %{spark_pkg_name}-core = %{version}-%{release}

%description -n %{spark_pkg_name}-worker
Server for Spark worker

%package -n %{spark_pkg_name}-python
Summary: Python client for Spark
Group: Development/Libraries
# No python dependency. You might need to install Python by yourself.
# See BIGTOP-4052 for details.
Requires: %{spark_pkg_name}-core = %{version}-%{release}

%description -n %{spark_pkg_name}-python
Includes PySpark, an interactive Python shell for Spark, and related libraries

%package -n %{spark_pkg_name}-history-server
Summary: History server for Apache Spark
Group: Development/Libraries
Requires: %{spark_pkg_name}-core = %{version}-%{release}

%description -n %{spark_pkg_name}-history-server
History server for Apache Spark

%package -n %{spark_pkg_name}-thriftserver
Summary: Thrift server for Spark SQL
Group: Development/Libraries
Requires: %{spark_pkg_name}-core = %{version}-%{release}

%description -n %{spark_pkg_name}-thriftserver
Thrift server for Spark SQL

%package -n %{spark_pkg_name}-datanucleus
Summary: DataNucleus libraries for Apache Spark
Group: Development/Libraries

%description -n %{spark_pkg_name}-datanucleus
DataNucleus libraries used by Spark SQL with Hive Support

%package -n %{spark_pkg_name}-external
Summary: External libraries for Apache Spark
Group: Development/Libraries

%description -n %{spark_pkg_name}-external
External libraries built for Apache Spark but not included in the main
distribution (e.g., external streaming libraries)

%package -n %{spark_pkg_name}-yarn-shuffle
Summary: Spark YARN Shuffle Service
Group: Development/Libraries

%description -n %{spark_pkg_name}-yarn-shuffle
Spark YARN Shuffle Service

%package -n %{spark_pkg_name}-sparkr
Summary: R package for Apache Spark
Group: Development/Libraries

%if 0%{?openEuler}
Requires: %{spark_pkg_name}-core = %{version}-%{release}
%else
Requires: %{spark_pkg_name}-core = %{version}-%{release}, R
%endif

%description -n %{spark_pkg_name}-sparkr
SparkR is an R package that provides a light-weight frontend to use Apache Spark from R.

%prep
%setup -n %{spark_name}-%{spark_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

bash $RPM_SOURCE_DIR/install_spark.sh \
          --build-dir=`pwd`         \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT  \
          --doc-dir=%{doc_spark} \
          --lib-dir=%{usr_lib_spark} \
          --var-dir=%{var_lib_spark} \
          --bin-dir=%{bin_dir} \
          --man-dir=%{man_dir} \
          --etc-default=%{etc_default} \
          --etc-spark=%{etc_spark}

%__rm -f $RPM_BUILD_ROOT/%{usr_lib_spark}/jars/hadoop-*.jar

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
%{alternatives_cmd} --install %{np_etc_spark}/conf %{spark_name}-conf %{etc_spark}/conf.dist 30

%preun
if [ "$1" = 0 ]; then
        %{alternatives_cmd} --remove %{spark_name}-conf %{etc_spark}/conf.dist || :
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
%config(noreplace) %{etc_spark}/conf.dist
%doc %{doc_spark}
%{usr_lib_spark}/LICENSE
%{usr_lib_spark}/NOTICE
%{usr_lib_spark}/RELEASE
%{usr_lib_spark}/bin
%exclude %{usr_lib_spark}/bin/pyspark
%{usr_lib_spark}/conf
%{usr_lib_spark}/data
%{usr_lib_spark}/examples
%{usr_lib_spark}/jars
%exclude %{usr_lib_spark}/jars/datanucleus-*.jar
%{usr_lib_spark}/licenses
%{usr_lib_spark}/sbin
%{usr_lib_spark}/work
%{usr_lib_spark}/kubernetes
%{np_etc_spark}
%attr(0755,spark,spark) %{var_lib_spark}
%attr(0755,spark,spark) %{np_var_run_spark}
%attr(0755,spark,spark) %{np_var_log_spark}
%{bin_dir}/spark-*
%{bin_dir}/find-spark-home
%exclude %{usr_lib_spark}/R
%exclude %{usr_lib_spark}/bin/sparkR
%exclude %{bin_dir}/sparkR

%files -n %{spark_pkg_name}-core
%defattr(-,root,root,755)
%{usr_lib_spark}/README.md

%files -n %{spark_pkg_name}-python
%defattr(-,root,root,755)
%attr(0755,root,root) %{bin_dir}/pyspark
%attr(0755,root,root) %{usr_lib_spark}/bin/pyspark
%{usr_lib_spark}/python

%files -n %{spark_pkg_name}-datanucleus
%defattr(-,root,root,755)
%{usr_lib_spark}/jars/datanucleus-*.jar
%{usr_lib_spark}/yarn/lib/datanucleus-*.jar

%files -n %{spark_pkg_name}-external
%defattr(-,root,root,755)
%{usr_lib_spark}/external

%files -n %{spark_pkg_name}-yarn-shuffle
%defattr(-,root,root,755)
%{usr_lib_spark}/yarn/spark-*-yarn-shuffle.jar
%{usr_lib_spark}/yarn/lib/spark-yarn-shuffle.jar

%files -n %{spark_pkg_name}-sparkr
%defattr(-,root,root,755)
%{usr_lib_spark}/R
%{usr_lib_spark}/bin/sparkR
%{bin_dir}/sparkR

%define service_macro() \
%files -n %{spark_pkg_name}-%1 \
%attr(0755,root,root)/%{initd_dir}/%{spark_name}-%1 \
%post -n %{spark_pkg_name}-%1 \
chkconfig --add %{spark_name}-%1 \
\
%preun -n %{spark_pkg_name}-%1 \
if [ $1 = 0 ] ; then \
        service %{spark_name}-%1 stop > /dev/null 2>&1 \
        chkconfig --del %{spark_name}-%1 \
fi \
%postun -n %{spark_pkg_name}-%1 \
if [ $1 -ge 1 ]; then \
        service %{spark_name}-%1 condrestart >/dev/null 2>&1 \
fi
%service_macro master
%service_macro worker
%service_macro history-server
%service_macro thriftserver
