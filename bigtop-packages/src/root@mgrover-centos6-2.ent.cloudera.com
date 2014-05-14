diff --git a/bigtop-packages/src/common/spark/compute-classpath.sh b/bigtop-packages/src/common/spark/compute-classpath.sh
index 24be837..61db280 100644
--- a/bigtop-packages/src/common/spark/compute-classpath.sh
+++ b/bigtop-packages/src/common/spark/compute-classpath.sh
@@ -17,7 +17,7 @@
 # This script computes Spark's classpath and prints it to stdout; it's used by both the "run"
 # script and the ExecutorRunner in standalone cluster mode.
 
-SCALA_VERSION=2.9.3
+SCALA_VERSION=2.10
 
 # Figure out where Spark is installed
 FWDIR="$(cd `dirname $0`/..; pwd)"
@@ -28,25 +28,15 @@ if [ -e $FWDIR/conf/spark-env.sh ] ; then
 fi
 
 CORE_DIR="$FWDIR/core"
-REPL_DIR="$FWDIR/repl"
-REPL_BIN_DIR="$FWDIR/repl-bin"
+ASSEMBLY_DIR="$FWDIR/assembly"
 EXAMPLES_DIR="$FWDIR/examples"
-BAGEL_DIR="$FWDIR/bagel"
-MLLIB_DIR="$FWDIR/mllib"
-STREAMING_DIR="$FWDIR/streaming"
 PYSPARK_DIR="$FWDIR/python"
 
 # Build up classpath
 CLASSPATH="$SPARK_CLASSPATH"
 CLASSPATH="$CLASSPATH:$FWDIR/conf"
-CLASSPATH="$CLASSPATH:$CORE_DIR/lib/*"
-CLASSPATH="$CLASSPATH:$REPL_DIR/lib/*"
+CLASSPATH="$CLASSPATH:$ASSEMBLY_DIR/lib/*"
 CLASSPATH="$CLASSPATH:$EXAMPLES_DIR/lib/*"
-CLASSPATH="$CLASSPATH:$BAGEL_DIR/lib/*"
-CLASSPATH="$CLASSPATH:$MLLIB_DIR/lib/*"
-CLASSPATH="$CLASSPATH:$STREAMING_DIR/lib/*"
-CLASSPATH="$CLASSPATH:$FWDIR/lib/*"
-#CLASSPATH="$CLASSPATH:$CORE_DIR/src/main/resources"
 if [ -e "$PYSPARK_DIR" ]; then
   for jar in `find $PYSPARK_DIR/lib -name '*jar'`; do
     CLASSPATH="$CLASSPATH:$jar"
@@ -60,6 +50,9 @@ fi
 export DEFAULT_HADOOP=/usr/lib/hadoop
 export DEFAULT_HADOOP_CONF=/etc/hadoop/conf
 export HADOOP_HOME=${HADOOP_HOME:-$DEFAULT_HADOOP}
+export HADOOP_HDFS_HOME=${HADOOP_HDFS_HOME:-${HADOOP_HOME}/../hadoop-hdfs}
+export HADOOP_MAPRED_HOME=${HADOOP_MAPRED_HOME:-${HADOOP_HOME}/../hadoop-mapreduce}
+export HADOOP_YARN_HOME=${HADOOP_YARN_HOME:-${HADOOP_HOME}/../hadoop-yarn}
 export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-$DEFAULT_HADOOP_CONF}
 
 CLASSPATH="$CLASSPATH:$HADOOP_CONF_DIR"
@@ -67,7 +60,7 @@ if [ "x" != "x$YARN_CONF_DIR" ]; then
   CLASSPATH="$CLASSPATH:$YARN_CONF_DIR"
 fi
 # Let's make sure that all needed hadoop libs are added properly
-CLASSPATH="$CLASSPATH:$HADOOP_HOME/lib/*:$HADOOP_HOME/*:${HADOOP_HOME}-hdfs/lib/*:${HADOOP_HOME}-hdfs/*:${HADOOP_HOME}-yarn/*:/usr/lib/hadoop-mapreduce/*"
+CLASSPATH="$CLASSPATH:$HADOOP_HOME/*:$HADOOP_HDFS_HOME/*:$HADOOP_YARN_HOME/*:$HADOOP_MAPRED_HOME/*"
 # Add Scala standard library
 if [ -z "$SCALA_LIBRARY_PATH" ]; then
   if [ -z "$SCALA_HOME" ]; then
diff --git a/bigtop-packages/src/common/spark/do-component-build b/bigtop-packages/src/common/spark/do-component-build
index 428540e..6f930d0 100644
--- a/bigtop-packages/src/common/spark/do-component-build
+++ b/bigtop-packages/src/common/spark/do-component-build
@@ -26,10 +26,12 @@ fi
 BUILD_OPTS="-Divy.home=${HOME}/.ivy2 -Dsbt.ivy.home=${HOME}/.ivy2 -Duser.home=${HOME} \
             -Drepo.maven.org=$IVY_MIRROR_PROP \
             -Dreactor.repo=file://${HOME}/.m2/repository \
-            -Dhadoop.version=$HADOOP_VERSION
-            -DskipTests"
+            -Dhadoop.version=$HADOOP_VERSION \
+            -Dyarn.version=$HADOOP_VERSION \
+            -Dprotobuf.version=2.5.0 \
+            -DskipTests -DrecompileMode=all"
 ## this might be an issue at times
 #        http://maven.40175.n5.nabble.com/Not-finding-artifact-in-local-repo-td3727753.html
 export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m -XX:PermSize=1024m -XX:MaxPermSize=1024m"
 
-mvn -Pbigtop-dist $BUILD_OPTS package
+mvn -Pbigtop-dist -Pyarn $BUILD_OPTS install
diff --git a/bigtop-packages/src/common/spark/install_spark.sh b/bigtop-packages/src/common/spark/install_spark.sh
index 45996db..a78c06a 100644
--- a/bigtop-packages/src/common/spark/install_spark.sh
+++ b/bigtop-packages/src/common/spark/install_spark.sh
@@ -113,7 +113,6 @@ fi
 MAN_DIR=${MAN_DIR:-/usr/share/man/man1}
 DOC_DIR=${DOC_DIR:-/usr/share/doc/spark}
 LIB_DIR=${LIB_DIR:-/usr/lib/spark}
-SPARK_BIN_DIR=${BIN_DIR:-/usr/lib/spark/bin}
 INSTALLED_LIB_DIR=${INSTALLED_LIB_DIR:-/usr/lib/spark}
 EXAMPLES_DIR=${EXAMPLES_DIR:-$DOC_DIR/examples}
 BIN_DIR=${BIN_DIR:-/usr/bin}
@@ -123,7 +122,8 @@ PYSPARK_PYTHON=${PYSPARK_PYTHON:-python}
 
 install -d -m 0755 $PREFIX/$LIB_DIR
 install -d -m 0755 $PREFIX/$LIB_DIR/lib
-install -d -m 0755 $PREFIX/$SPARK_BIN_DIR
+install -d -m 0755 $PREFIX/$LIB_DIR/bin
+install -d -m 0755 $PREFIX/$LIB_DIR/sbin
 install -d -m 0755 $PREFIX/$DOC_DIR
 
 install -d -m 0755 $PREFIX/var/lib/spark/
@@ -133,7 +133,7 @@ install -d -m 0755 $PREFIX/var/run/spark/work/
 
 tar --wildcards -C $PREFIX/$LIB_DIR -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz 'lib/*'
 
-for comp in core repl bagel mllib streaming; do
+for comp in core repl bagel mllib streaming assembly; do
   install -d -m 0755 $PREFIX/$LIB_DIR/$comp/lib
   tar --wildcards -C $PREFIX/$LIB_DIR/$comp/lib -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz spark-$comp\*
 done
@@ -141,11 +141,16 @@ done
 install -d -m 0755 $PREFIX/$LIB_DIR/examples/lib
 cp ${BUILD_DIR}/examples/target/spark-examples*${SPARK_VERSION}.jar $PREFIX/$LIB_DIR/examples/lib
 
+cp -a ${BUILD_DIR}/bin/*.sh $PREFIX/$LIB_DIR/bin/
+cp -a ${BUILD_DIR}/sbin/*.sh $PREFIX/$LIB_DIR/sbin/
+chmod 755 $PREFIX/$LIB_DIR/bin/*
+chmod 755 $PREFIX/$LIB_DIR/sbin/*
+
 # FIXME: executor scripts need to reside in bin
-cp -a $BUILD_DIR/spark-class $PREFIX/$LIB_DIR
-cp -a $BUILD_DIR/spark-executor $PREFIX/$LIB_DIR
-cp -a ${SOURCE_DIR}/compute-classpath.sh $PREFIX/$SPARK_BIN_DIR
-cp -a ${BUILD_DIR}/spark-shell $PREFIX/$LIB_DIR
+cp -a $BUILD_DIR/bin/spark-class $PREFIX/$LIB_DIR/bin/
+cp -a $BUILD_DIR/sbin/spark-executor $PREFIX/$LIB_DIR/sbin/
+cp -a ${SOURCE_DIR}/compute-classpath.sh $PREFIX/$LIB_DIR/bin/
+cp -a ${BUILD_DIR}/bin/spark-shell $PREFIX/$LIB_DIR/bin/
 touch $PREFIX/$LIB_DIR/RELEASE
 
 # Copy in the configuration files
@@ -158,15 +163,15 @@ ln -s /etc/spark/conf $PREFIX/$LIB_DIR/conf
 tar --wildcards -C $PREFIX/$LIB_DIR -zxf ${BUILD_DIR}/assembly/target/spark-assembly*-dist.tar.gz ui-resources/\*
 
 # set correct permissions for exec. files
-for execfile in spark-class spark-shell spark-executor ; do
+for execfile in bin/spark-class bin/spark-shell sbin/spark-executor ; do
   chmod 755 $PREFIX/$LIB_DIR/$execfile
 done
-chmod 755 $PREFIX/$SPARK_BIN_DIR/compute-classpath.sh
+chmod 755 $PREFIX/$LIB_DIR/bin/compute-classpath.sh
 
 # Copy in the wrappers
 install -d -m 0755 $PREFIX/$BIN_DIR
-for wrap in spark-executor spark-shell ; do
-  cat > $PREFIX/$BIN_DIR/$wrap <<EOF
+for wrap in sbin/spark-executor bin/spark-shell ; do
+  cat > $PREFIX/$BIN_DIR/`basename $wrap` <<EOF
 #!/bin/bash 
 
 # Autodetect JAVA_HOME if not defined
@@ -174,7 +179,7 @@ for wrap in spark-executor spark-shell ; do
 
 exec $INSTALLED_LIB_DIR/$wrap "\$@"
 EOF
-  chmod 755 $PREFIX/$BIN_DIR/$wrap
+  chmod 755 $PREFIX/$BIN_DIR/`basename $wrap`
 done
 
 cat >> $PREFIX/$CONF_DIR/spark-env.sh <<EOF
@@ -185,12 +190,19 @@ export SPARK_LIBRARY_PATH=\${SPARK_HOME}/lib
 export SCALA_LIBRARY_PATH=\${SPARK_HOME}/lib
 export SPARK_MASTER_WEBUI_PORT=18080
 export SPARK_MASTER_PORT=7077
+export SPARK_WORKER_PORT=7078
+export SPARK_WORKER_WEBUI_PORT=18081
+export SPARK_WORKER_DIR=/var/run/spark/work
+export SPARK_LOG_DIR=/var/log/spark
+
+if [ -n "\$HADOOP_HOME" ]; then
+  export SPARK_LIBRARY_PATH=\$SPARK_LIBRARY_PATH:\${HADOOP_HOME}/lib/native
+fi
 
 ### Comment above 2 lines and uncomment the following if
 ### you want to run with scala version, that is included with the package
 #export SCALA_HOME=\${SCALA_HOME:-$LIB_DIR/scala}
 #export PATH=\$PATH:\$SCALA_HOME/bin
-
 ### change the following to specify a real cluster's Master host
 export STANDALONE_SPARK_MASTER_HOST=\`hostname\`
 
@@ -199,7 +211,7 @@ EOF
 ln -s /var/run/spark/work $PREFIX/$LIB_DIR/work
 
 cp -r ${BUILD_DIR}/python ${PREFIX}/${INSTALLED_LIB_DIR}/
-cp ${BUILD_DIR}/pyspark ${PREFIX}/${INSTALLED_LIB_DIR}/
+cp ${BUILD_DIR}/bin/pyspark ${PREFIX}/${INSTALLED_LIB_DIR}/bin/
 cat > $PREFIX/$BIN_DIR/pyspark <<EOF
 #!/bin/bash
 
@@ -208,7 +220,8 @@ cat > $PREFIX/$BIN_DIR/pyspark <<EOF
 
 export PYSPARK_PYTHON=${PYSPARK_PYTHON}
 
-exec $INSTALLED_LIB_DIR/pyspark "\$@"
+exec $INSTALLED_LIB_DIR/bin/pyspark "\$@"
 EOF
 chmod 755 $PREFIX/$BIN_DIR/pyspark
 
+cp ${BUILD_DIR}/{LICENSE,NOTICE} ${PREFIX}/${LIB_DIR}/
diff --git a/bigtop-packages/src/common/spark/spark-master.svc b/bigtop-packages/src/common/spark/spark-master.svc
index 42e89df..6d8f56a 100644
--- a/bigtop-packages/src/common/spark/spark-master.svc
+++ b/bigtop-packages/src/common/spark/spark-master.svc
@@ -16,7 +16,7 @@
 TYPE="master"
 DAEMON="spark-${TYPE}"
 DESC="Spark ${TYPE}"
-EXEC_PATH="/usr/lib/spark/spark-class"
+EXEC_PATH="/usr/lib/spark/bin/spark-class"
 SVC_USER="spark"
 WORKING_DIR="/var/lib/spark"
 DAEMON_FLAGS=""
diff --git a/bigtop-packages/src/common/spark/spark-worker.svc b/bigtop-packages/src/common/spark/spark-worker.svc
index 50c5487..af8a5b1 100644
--- a/bigtop-packages/src/common/spark/spark-worker.svc
+++ b/bigtop-packages/src/common/spark/spark-worker.svc
@@ -16,7 +16,7 @@
 TYPE="worker"
 DAEMON="spark-${TYPE}"
 DESC="Spark ${TYPE}"
-EXEC_PATH="/usr/lib/spark/spark-class"
+EXEC_PATH="/usr/lib/spark/bin/spark-class"
 SVC_USER="spark"
 WORKING_DIR="/var/lib/spark"
 DAEMON_FLAGS=""
diff --git a/bigtop-packages/src/deb/spark/control b/bigtop-packages/src/deb/spark/control
index 4e36ba8..ae2f07e 100644
--- a/bigtop-packages/src/deb/spark/control
+++ b/bigtop-packages/src/deb/spark/control
@@ -19,11 +19,11 @@ Priority: extra
 Maintainer: Bigtop <dev@bigtop.apache.org>
 Build-Depends: debhelper (>= 6)
 Standards-Version: 3.8.0
-Homepage: http://spark.incubator.apache.org/
+Homepage: http://spark.apache.org/
 
 Package: spark-core
 Architecture: all
-Depends: bigtop-utils (>= 0.7)
+Depends: bigtop-utils (>= 0.7), hadoop-client
 Description: Lightning-Fast Cluster Computing
  Spark is a MapReduce-like cluster computing framework designed to support
  low-latency iterative jobs and interactive use from an interpreter. It is
diff --git a/bigtop-packages/src/deb/spark/copyright b/bigtop-packages/src/deb/spark/copyright
index c15cf45..3255f3d 100644
--- a/bigtop-packages/src/deb/spark/copyright
+++ b/bigtop-packages/src/deb/spark/copyright
@@ -1,5 +1,5 @@
 Format: http://dep.debian.net/deps/dep5
-Source: http://spark.incubator.apache.org/
+Source: http://spark.apache.org/
 Upstream-Name: Spark Project
 
 Files: *
diff --git a/bigtop-packages/src/deb/spark/spark-core.install b/bigtop-packages/src/deb/spark/spark-core.install
index a48a222..bbbdb94 100644
--- a/bigtop-packages/src/deb/spark/spark-core.install
+++ b/bigtop-packages/src/deb/spark/spark-core.install
@@ -1,8 +1,12 @@
 /etc/spark
 /usr/bin/spark-executor
 /usr/bin/spark-shell
+/usr/lib/spark/assembly
 /usr/lib/spark/bagel
-/usr/lib/spark/bin
+/usr/lib/spark/bin/compute-classpath.sh
+/usr/lib/spark/bin/spark-class
+/usr/lib/spark/bin/spark-shell
+/usr/lib/spark/sbin
 /usr/lib/spark/core
 /usr/lib/spark/examples
 /usr/lib/spark/lib
@@ -11,10 +15,9 @@
 /usr/lib/spark/streaming
 /usr/lib/spark/ui-resources
 /usr/lib/spark/conf
+/usr/lib/spark/LICENSE
+/usr/lib/spark/NOTICE
 /usr/lib/spark/RELEASE
-/usr/lib/spark/spark-class
-/usr/lib/spark/spark-executor
-/usr/lib/spark/spark-shell
 /var/lib/spark/
 /var/log/spark/
 /var/run/spark/
diff --git a/bigtop-packages/src/deb/spark/spark-core.preinst b/bigtop-packages/src/deb/spark/spark-core.preinst
index c8950a4..ea83e68 100644
--- a/bigtop-packages/src/deb/spark/spark-core.preinst
+++ b/bigtop-packages/src/deb/spark/spark-core.preinst
@@ -59,3 +59,4 @@ esac
 #DEBHELPER#
 
 exit 0
+
diff --git a/bigtop-packages/src/deb/spark/spark-python.install b/bigtop-packages/src/deb/spark/spark-python.install
index b5461bb..8211931 100644
--- a/bigtop-packages/src/deb/spark/spark-python.install
+++ b/bigtop-packages/src/deb/spark/spark-python.install
@@ -1,4 +1,4 @@
 /usr/bin/pyspark
-/usr/lib/spark/pyspark
+/usr/lib/spark/bin/pyspark
 /usr/lib/spark/python
 
diff --git a/bigtop-packages/src/rpm/spark/SPECS/spark.spec b/bigtop-packages/src/rpm/spark/SPECS/spark.spec
index 63c4a63..ea12557 100644
--- a/bigtop-packages/src/rpm/spark/SPECS/spark.spec
+++ b/bigtop-packages/src/rpm/spark/SPECS/spark.spec
@@ -33,8 +33,6 @@
 %define alternatives_cmd alternatives
 %endif
 
-%define pyspark_python python
-
 # disable repacking jars
 %define __os_install_post %{nil}
 
@@ -42,7 +40,7 @@ Name: spark-core
 Version: %{spark_version}
 Release: %{spark_release}
 Summary: Lightning-Fast Cluster Computing
-URL: http://spark.incubator.apache.org/
+URL: http://spark.apache.org/
 Group: Development/Libraries
 BuildArch: noarch
 Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
@@ -52,7 +50,9 @@ Source1: do-component-build
 Source2: install_%{spark_name}.sh
 Source3: spark-master.svc
 Source4: spark-worker.svc
-Requires: bigtop-utils >= 0.7
+Source5: compute-classpath.sh
+Source6: init.d.tmpl
+Requires: bigtop-utils >= 0.7, hadoop-client
 Requires(preun): /sbin/service
 
 %global initd_dir %{_sysconfdir}/init.d
@@ -96,7 +96,7 @@ Server for Spark worker
 %package -n spark-python
 Summary: Python client for Spark
 Group: Development/Libraries
-Requires: spark-core = %{version}-%{release}, %{pyspark_python}
+Requires: spark-core = %{version}-%{release}, python
 
 %description -n spark-python
 Includes PySpark, an interactive Python shell for Spark, and related libraries
@@ -116,7 +116,7 @@ bash $RPM_SOURCE_DIR/install_spark.sh \
           --source-dir=$RPM_SOURCE_DIR \
           --prefix=$RPM_BUILD_ROOT  \
           --doc-dir=%{doc_spark} \
-          --pyspark-python=%{pyspark_python}
+          --pyspark-python=python
 
 for service in %{spark_services}
 do
@@ -152,7 +152,7 @@ done
 %config(noreplace) %{config_spark}.dist
 %doc %{doc_spark}
 %{lib_spark}
-%exclude %{lib_spark}/pyspark
+%exclude %{lib_spark}/bin/pyspark
 %exclude %{lib_spark}/python
 %{etc_spark}
 %attr(0755,spark,spark) %{var_lib_spark}
@@ -165,7 +165,7 @@ done
 %files -n spark-python
 %defattr(-,root,root,755)
 %attr(0755,root,root) %{bin}/pyspark
-%attr(0755,root,root) %{lib_spark}/pyspark
+%attr(0755,root,root) %{lib_spark}/bin/pyspark
 %{lib_spark}/python
 
 %define service_macro() \
diff --git a/bigtop.mk b/bigtop.mk
index 6e21ea6..96bf813 100644
--- a/bigtop.mk
+++ b/bigtop.mk
@@ -226,12 +226,12 @@ $(eval $(call PACKAGE,crunch,CRUNCH))
 SPARK_NAME=spark
 SPARK_RELNOTES_NAME=Spark
 SPARK_PKG_NAME=spark-core
-SPARK_BASE_VERSION=0.8.0-incubating
-SPARK_PKG_VERSION=0.8.0
+SPARK_BASE_VERSION=0.9.1
+SPARK_PKG_VERSION=0.9.1
 SPARK_RELEASE_VERSION=1
 SPARK_TARBALL_DST=spark-$(SPARK_BASE_VERSION).tar.gz
 SPARK_TARBALL_SRC=spark-$(SPARK_BASE_VERSION).tgz
-SPARK_DOWNLOAD_PATH=/incubator/spark/spark-$(SPARK_BASE_VERSION)
+SPARK_DOWNLOAD_PATH=/spark/spark-$(SPARK_BASE_VERSION)
 SPARK_SITE=$(APACHE_MIRROR)$(SPARK_DOWNLOAD_PATH)
 SPARK_ARCHIVE=$(APACHE_ARCHIVE)$(SPARK_DOWNLOAD_PATH)
 $(eval $(call PACKAGE,spark,SPARK))
