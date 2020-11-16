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
%define spark_services master worker history-server thriftserver
%define lib_hadoop_client /usr/lib/hadoop/client
%define lib_hadoop_yarn /usr/lib/hadoop-yarn/

%if  %{?suse_version:1}0
%define doc_spark %{_docdir}/spark
%define alternatives_cmd update-alternatives
%else
%define doc_spark %{_docdir}/spark-%{spark_version}
%define alternatives_cmd alternatives
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: spark-core
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
Requires: bigtop-utils >= 0.7, hadoop-client, hadoop-yarn
Requires(preun): /sbin/service

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
Spark is a MapReduce-like cluster computing framework designed to support
low-latency iterative jobs and interactive use from an interpreter. It is
written in Scala, a high-level language for the JVM, and exposes a clean
language-integrated syntax that makes it easy to write parallel jobs.
Spark runs on top of the Apache Mesos cluster manager.

%package -n spark-master
Summary: Server for Spark master
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description -n spark-master
Server for Spark master

%package -n spark-worker
Summary: Server for Spark worker
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description -n spark-worker
Server for Spark worker

%package -n spark-python
Summary: Python client for Spark
Group: Development/Libraries
%if 0%{?rhel} >= 8
Requires: spark-core = %{version}-%{release}, python2
%else
Requires: spark-core = %{version}-%{release}, python
%endif

%description -n spark-python
Includes PySpark, an interactive Python shell for Spark, and related libraries

%package -n spark-history-server
Summary: History server for Apache Spark
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description -n spark-history-server
History server for Apache Spark

%package -n spark-thriftserver
Summary: Thrift server for Spark SQL
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}

%description -n spark-thriftserver
Thrift server for Spark SQL

%package -n spark-datanucleus
Summary: DataNucleus libraries for Apache Spark
Group: Development/Libraries

%description -n spark-datanucleus
DataNucleus libraries used by Spark SQL with Hive Support

%package -n spark-external
Summary: External libraries for Apache Spark
Group: Development/Libraries

%description -n spark-external
External libraries built for Apache Spark but not included in the main
distribution (e.g., external streaming libraries)

%package -n spark-yarn-shuffle
Summary: Spark YARN Shuffle Service
Group: Development/Libraries

%description -n spark-yarn-shuffle
Spark YARN Shuffle Service

%package -n spark-sparkr
Summary: R package for Apache Spark
Group: Development/Libraries
Requires: spark-core = %{version}-%{release}, R

%description -n spark-sparkr
SparkR is an R package that provides a light-weight frontend to use Apache Spark from R.

%prep
%setup -n %{spark_name}-%{spark_base_version}

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%if 0%{?rhel} >= 8
PYSPARK_PYTHON=python2 bash $RPM_SOURCE_DIR/install_spark.sh \
          --build-dir=`pwd`         \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT  \
          --doc-dir=%{doc_spark}
%else
bash $RPM_SOURCE_DIR/install_spark.sh \
          --build-dir=`pwd`         \
          --source-dir=$RPM_SOURCE_DIR \
          --prefix=$RPM_BUILD_ROOT  \
          --doc-dir=%{doc_spark}
%endif

%__rm -f $RPM_BUILD_ROOT/%{lib_spark}/jars/hadoop-*.jar

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
%{lib_spark}/LICENSE
%{lib_spark}/NOTICE
%{lib_spark}/README.md
%{lib_spark}/RELEASE
%{bin_spark}
%exclude %{bin_spark}/pyspark
%{lib_spark}/conf
%{lib_spark}/data
%{lib_spark}/examples
%{lib_spark}/jars
%exclude %{lib_spark}/jars/datanucleus-*.jar
%{lib_spark}/licenses
%{lib_spark}/sbin
%{lib_spark}/work
%{etc_spark}
%attr(0755,spark,spark) %{var_lib_spark}
%attr(0755,spark,spark) %{var_run_spark}
%attr(0755,spark,spark) %{var_log_spark}
%{bin}/spark-*
%{bin}/find-spark-home
%exclude %{lib_spark}/R
%exclude %{lib_spark}/bin/sparkR
%exclude %{bin}/sparkR

%files -n spark-python
%defattr(-,root,root,755)
%attr(0755,root,root) %{bin}/pyspark
%attr(0755,root,root) %{lib_spark}/bin/pyspark
%{lib_spark}/python

%files -n spark-datanucleus
%defattr(-,root,root,755)
%{lib_spark}/jars/datanucleus-*.jar
%{lib_spark}/yarn/lib/datanucleus-*.jar

%files -n spark-external
%defattr(-,root,root,755)
%{lib_spark}/external

%files -n spark-yarn-shuffle
%defattr(-,root,root,755)
%{lib_spark}/yarn/spark-*-yarn-shuffle.jar
%{lib_spark}/yarn/lib/spark-yarn-shuffle.jar

%files -n spark-sparkr
%defattr(-,root,root,755)
%{lib_spark}/R
%{lib_spark}/bin/sparkR
%{bin}/sparkR

%define service_macro() \
%files -n %1 \
%attr(0755,root,root)/%{initd_dir}/%1 \
%post -n %1 \
chkconfig --add %1 \
\
%preun -n %1 \
if [ $1 = 0 ] ; then \
        service %1 stop > /dev/null 2>&1 \
        chkconfig --del %1 \
fi \
%postun -n %1 \
if [ $1 -ge 1 ]; then \
        service %1 condrestart >/dev/null 2>&1 \
fi
%service_macro spark-master
%service_macro spark-worker
%service_macro spark-history-server
%service_macro spark-thriftserver
