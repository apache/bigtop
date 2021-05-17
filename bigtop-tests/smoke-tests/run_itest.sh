#!/bin/bash
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

## Borrowed heavily from
##  https://github.com/jctanner/odp-scripts/blob/master/run_itest.sh
## Kudos to https://github.com/jctanner

# https://github.com/apache/bigtop/tree/master/bigtop-test-framework

# "ITEST" is an integration testing framework written for and by the
# apache bigtop project. It consists of typical java tools/libraries
# such as junit, gradle and maven.

# This script is a helper to run itest on any hadoop system without
# requiring intimate knowledge of bigtop. If running for the first
# time, simply execute ./run_itest.sh without any arguments. If you
# want more information, use these additional parameters:
#
#   --info          - turns on the log4j output
#   --debug         - turns up the log4j output to maximum
#   --traceback     - shows tracebacks from tests

usage ()
{
  echo "Usage : $0 [-options]"
  echo
  echo "   -j, --hivejdbc             hive jdbc url - default: e.g. jdbc:hive2://localhost:10000"
  echo "   -m, --hivemeta             hive metastore url - default: thrift://localhost:9083"
  echo "   -l, --hivelocation         location of hdfs for hive to write to - default: /tmp"
  echo "   -u, --hiveuser             hive user - default: hive"
  echo "   -p, --hivepassword         hive user password - default: hive"
  echo "   -t, --hivethrift           optional: true/false to test thrift, defaults to true"
  echo "   -c, --hivecatalog          optional: true/false to test HCatalog, default to true"
  echo "   -C, --hiveconf             hive conf dir - default: /etc/hive/conf"
  echo "   -F, --hadoopconf           hadoop user - default: /etc/hadoop/conf"
  echo "   -i, --info                 optional: info/debug"
  echo "   -h, --help                 display this help and exit"
  echo
  exit
}

while [ "$1" != "" ]; do
case $1 in
        -j | --hivejdbc )       shift
                                HIVE_JDBC_URL=$1
                                ;;
        -m | --hivemeta )       shift
                                HIVE_METASTORE_URL=$1
                                ;;
        -l | --hivelocation )   shift
                                HIVE_HDFS_LOCATION=$1
                                ;;
        -u | --hiveuser )       shift
                                HIVE_USER=$1
                                ;;
        -p | --hivepassword )   shift
                                HIVE_PASSWORD=$1
                                ;;
        -t | --hivethrift )     shift
                                TEST_THRIFT=$1
                                ;;
        -c | --hivecatalog )     shift
                                TEST_HCATALOG=$1
                                ;;
        -C | --hiveconf )       shift
                                HIVE_CONF_DIR=$1
                                ;;
        -F | --hadoopconf )     shift
                                HADOOP_CONF_DIR=$1
                                ;;
        -i | --info )           shift
                                LOGGING=$1
                                ;;
        -h | --help )
                                usage  # Call your function
                                exit 0
                                ;;
    esac
    shift
done

if [ "$LOGGING" == "info" ]; then
    LOGGING="--info --stacktrace"
elif [ "$LOGGING" == "debug" ]; then
    LOGGING="--debug --stacktrace"
else
    LOGGING=""
fi

export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )"

pre_reqcheck() {
    
    sudo -v > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "The current user $(whoami) is required to have passwordless sudo access."
        exit 1
    fi

    if [ ! $(which links) ]; then
        echo "PLEASE INSTALL LINKS: sudo yum|apt-get install -y links"
        exit 1
    fi

    hadoop fs -ls /user/`id -u -n` > /dev/null 2>&1
    if [ $? -ne 0 ]; then
      if ( [ -z "$HADOOP_CONF_DIR" ] && [ -d /etc/hadoop/conf ] ); then
        export HADOOP_CONF_DIR=/etc/hadoop/conf
        HDFS_USER=`sed -n '/dfs.cluster.administrators/ {n;p}' $HADOOP_CONF_DIR/hdfs-site.xml | grep -oPm1 "(?<=<value>)[^<]+"`
        if [ $? -eq 0 ]; then
 	  sudo -u $HDFS_USER hadoop fs -mkdir /user/`id -u -n`
          sudo -u $HDFS_USER hadoop fs -chown `id -u -n` /user/`id -u -n`
        else
          echo -e "Please create the following home directory in hdfs /user/"`id -u -n` 
	  exit 1
        fi
      else
         echo -e "Please create the following home directory in hdfs /user/"`id -u -n`
         exit 1
      fi
    fi 

}


set_java_home() {

    #####################################################################
    # Use bigtop's bigtop-detect-javahome if JAVA_HOME is not already set
    #####################################################################

    if [ -z "$JAVA_HOME" ]; then
        hadoop envvars | grep JAVA_HOME > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            export `hadoop envvars | grep JAVA_HOME | sed "s/'//g"`
	elif [ -f $DIR/bin/bigtop-detect-javahome ]; then
            source $DIR/bin/bigtop-detect-javahome
        elif [ -f $DIR/../../bigtop-packages/src/common/bigtop-utils/bigtop-detect-javahome ]; then
            source $DIR/../../bigtop-packages/src/common/bigtop-utils/bigtop-detect-javahome
        fi
    fi
}

set_hadoop_vars() {

    #####################################################################
    # Set the HADOOP_MAPRED_HOME and HADOOP_CONF vars
    #####################################################################

    # ITEST wants the MR dir with the examples jar ...
    # java.lang.AssertionError: Can't find hadoop-examples.jar file

    if ( [ -z "$HADOOP_HOME" ] && [ -d /usr/lib/hadoop ] ); then
      export HADOOP_HOME=/usr/lib/hadoop
    fi
    if ( [ -z "$HADOOP_CONF_DIR" ] && [ -d /etc/hadoop/conf ] ); then
      export HADOOP_CONF_DIR=/etc/hadoop/conf
      YARN_ADMIN_USERS=`sed -n '/yarn.admin.acl/ {n;p}' $HADOOP_CONF_DIR/yarn-site.xml | grep -oPm1 "(?<=<value>)[^<]+"`
      if [ $? -eq 0 ]; then
	CURRENT_USER=`id -u -n`
	if ! ( [ "$YARN_ADMIN_USERS" = "*" ] || [[ "$YARN_ADMIN_USERS" == *"$CURRENT_USER"* ]] ); then
          echo -e "\n**** Current user $CURRENT_USER is not part of yarn.admin.acl in $HADOOP_CONF_DIR/yarn-site.xml. Please add to acl or run with proper user '$YARN_ADMIN_USERS'. ****\n"
	  exit 1
        fi
      fi
    elif [ -n "$HADOOP_CONF_DIR" ]; then
      YARN_ADMIN_USERS=`sed -n '/yarn.admin.acl/ {n;p}' $HADOOP_CONF_DIR/yarn-site.xml | grep -oPm1 "(?<=<value>)[^<]+"`
      if [ $? -eq 0 ]; then
	CURRENT_USER=`id -u -n`
        if ! ( [ "$YARN_ADMIN_USERS" = "*" ] || [[ "$YARN_ADMIN_USERS" == *"$CURRENT_USER"* ]] ); then
          echo -e "\n**** Current user $CURRENT_USER is not part of yarn.admin.acl in $HADOOP_CONF_DIR/yarn-site.xml. Please add to acl or run with proper user '$YARN_ADMIN_USERS'. ****\n"
	  exit 1
        fi
      fi
    fi
    if ( [ -z "$HADOOP_MAPRED_HOME" ] && [ -d /usr/lib/hadoop-mapreduce-client ] ); then
      export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce-client
    elif ( [ -z "$HADOOP_MAPRED_HOME" ] && [ -d /usr/lib/hadoop-mapreduce ] ); then
      export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce
    elif ( [ -z $"$HADOOP_MAPRED_HOME" ] && [[ -d `sudo find /usr/ -name hadoop-mapreduce-client | head -n 1` ]] ); then
      export HADOOP_MAPRED_HOME=`sudo find /usr/ -name hadoop-mapreduce-client`
    elif ( [ -z $"$HADOOP_MAPRED_HOME" ] && [[ -d `sudo find /usr/ -name hadoop-mapreduce | head -n 1` ]] ); then
      export HADOOP_MAPRED_HOME=`sudo find /usr/ -name hadoop-mapreduce`
    fi
    if ( [ -z "$HIVE_HOME" ] && [ -d /usr/lib/hive ] ); then
      export HIVE_HOME=/usr/lib/hive
    fi
}

set_hive_vars() {
    if [ -z "$HIVE_JDBC_URL" ]; then
        HIVE_PORT=`sed -n '/hive.server2.thrift.port/{n;p}' /etc/hive/conf/hive-site.xml | sed -n 's:.*<value>\(.*\)</value>.*:\1:p'`
        sudo netstat -nltp | grep $HIVE_PORT > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            HIVE_JDBC_URL=jdbc:hive2://localhost:$HIVE_PORT
        else
            echo -e "\n**** Could not find hive server 2 service, please specify --hivejdbc argument. ****\n"
            usage
        fi
    fi
    if [ -z "$HIVE_METASTORE_URL" ]; then
        HIVE_METASTORE_URL=`sed -n '/hive.metastore.uris/{n;p}' /etc/hive/conf/hive-site.xml | sed -n 's:.*<value>\(.*\)</value>.*:\1:p'`
    fi
    if [ -z "$HIVE_HDFS_LOCATION" ]; then
        HIVE_HDFS_LOCATION=/tmp/`id -u -n`
    fi
    if [ -z "$HIVE_USER" ]; then
        HIVE_USER=hive
    fi
    if [ -z "$HIVE_PASSWORD" ]; then
        HIVE_PASSWORD=hive
    fi
    if [ -z "$HIVE_CONF_DIR" ]; then
        export HIVE_CONF_DIR=/etc/hive/conf
    fi
    if [ -z "$HADOOP_CONF_DIR" ]; then
        export HADOOP_CONF_DIR=/etc/hadoop/conf
    fi
    if [ -z "$TEST_THRIFT" ]; then
        TEST_THRIFT=true
    fi
    if [ -z "$TEST_HCATALOG" ]; then
        TEST_HCATALOG=true
    fi

    TEST_SETTINGS="$TEST_SETTINGS -Dbigtop.test.hive.jdbc.url=$HIVE_JDBC_URL -Dbigtop.test.hive.metastore.url=$HIVE_METASTORE_URL -Dbigtop.test.hive.location=$HIVE_HDFS_LOCATION -Dbigtop.test.hive.jdbc.user=$HIVE_USER -Dbigtop.test.hive.jdbc.password=$HIVE_PASSWORD -Dbigtop.test.hive.conf.dir=$HIVE_CONF_DIR -Dbigtop.test.hadoop.conf.dir=$HADOOP_CONF_DIR -Dbigtop.test.hive.thrift.test=$TEST_THRIFT -Dbigtop.test.hive.hcatalog.test=$TEST_HCATALOG"
}

set_odpi_runtime_vars() {
    TEST_SETTINGS="$TEST_SETTINGS -DHCFS_IMPLEMENTATION=HCFS"
}

print_cluster_info() {

  echo "######################################################"
  echo "#               CLUSTER INFORMATION                  #"
  echo "######################################################"

  which facter >/dev/null 2>&1
  RC=$?
  if [[ $RC == 0 ]]; then
    echo "# OS: $(facter lsbdistdescription)"
    echo "# ARCH: $(facter architecture)"
    echo "# KERNEL: $(facter kernelrelease)"
    echo "# MEMORY: $(facter memorysize)"
  else
    echo "# OS: $(cat /etc/issue | tr '\n' ' ')"
    echo "# ARCH: $(uname -i)"
    echo "# KERNEL: $(uname -a)"
    echo "# MEMORY: $(head -n1 /proc/meminfo)"
  fi

  YARN_NODES=$(yarn node -list 2>/dev/null | egrep ^Total | sed 's/Total Nodes:/TOTAL YARN NODES: /g')
  echo "# $YARN_NODES"

  HADOOP_VERSION=$(hadoop version 2>/dev/null | head -n1)
  echo "# HADOOP_VERSION: $HADOOP_VERSION"
  echo "# HADOOP_CONF_DIR: $HADOOP_CONF_DIR"
  echo "# HADOOP_MAPRED_HOME: $HADOOP_MAPRED_HOME"

  echo "# BASH_VERSION: $BASH_VERSION"
  echo "# SH_VERSION: $(/bin/sh -c 'echo $BASH_VERSION')"

  echo "# JAVA_HOME: $JAVA_HOME"
  JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
  echo "# JAVA_VERSION: $JAVA_VERSION"
  
  if [ ! -f $JAVA_HOME/lib/tools.jar ]; then
    echo -e "\n**** Could not find Java tools.jar, please set JAVA_HOME to a JDK and make sure the java devel package is installed! ****\n"
    exit 1
  fi
}

print_tests() {
  echo "######################################################"
  echo "#                     RESULTS                        #"
  echo "######################################################"

  for TEST in $(echo $ITESTS | tr ',' '\n'); do
    if [ -f $DIR/gradlew ]; then
      TESTDIR=$DIR/bigtop-tests/smoke-tests/$TEST/build
    else
      TESTDIR=$DIR/$TEST/build   
    fi

    if [ -d $TESTDIR ]; then
      cd $TESTDIR

      for FILE in $(find -L reports/tests/classes -type f -name "*.html"); do
        echo "## $TESTDIR/$FILE"
        links $FILE -dump | awk '/^Standard error$/ { stop_stdin=1 } (!stop_stdin) { print $0; } { print $0 > "/dev/stderr" }'
        echo ""
      done
    fi
  done
}

# PREREQ CHECK
pre_reqcheck

# SET JAVA_HOME
set_java_home

# SET HADOOP SERVICE HOMES
set_hadoop_vars

# Diagnostic output
print_cluster_info

echo "######################################################"
echo "#                 STARTING ITEST                     #"
echo "######################################################"
echo "# Use --debug/--info for more details"

# SET THE DEFAULT TESTS
if [ -z "$ITESTS" ]; then
  export ITESTS="hive,hcfs,hdfs,yarn,mapreduce,odpi-runtime"
fi

case "$ITESTS" in
  *odpi-runtime*) set_hive_vars
                  set_odpi_runtime_vars
                  ;;
  *hive*)         set_hive_vars
                  ;;
esac

# CALL THE GRADLE WRAPPER TO RUN THE FRAMEWORK

if [ -f $DIR/gradlew ]; then
  for s in `echo $ITESTS | sed -e 's#,# #g'`; do
    ALL_SMOKE_TASKS="$ALL_SMOKE_TASKS bigtop-tests:smoke-tests:$s:test"
  done
  $DIR/gradlew -q --continue clean -Psmoke.tests $TEST_SETTINGS $ALL_SMOKE_TASKS $LOGGING
elif [ -f $DIR/../../gradlew ]; then
  for s in `echo $ITESTS | sed -e 's#,# #g'`; do
    ALL_SMOKE_TASKS="$ALL_SMOKE_TASKS $s:test"
  done
  $DIR/../../gradlew -q --continue clean -Psmoke.tests $TEST_SETTINGS $ALL_SMOKE_TASKS $LOGGING
fi

# SHOW RESULTS (HTML)
print_tests
