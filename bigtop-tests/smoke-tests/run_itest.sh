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

set_java_home() {

    #####################################################################
    # Use bigtop's bigtop-detect-javahome if JAVA_HOME is not already set
    #####################################################################

    if [ -z "$JAVA_HOME" ]; then
        source bin/bigtop-detect-javahome
    fi

    echo "# DEBUG: JAVA_HOME=$JAVA_HOME"
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
    fi
    if ( [ -z "$HADOOP_MAPRED_HOME" ] && [ -d /usr/lib/hadoop-mapreduce-client ] ); then
      export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce-client
    fi

    echo "# DEBUG: HADOOP_CONF_DIR=$HADOOP_CONF_DIR"
    echo "# DEBUG: HADOOP_MAPRED_HOME=$HADOOP_MAPRED_HOME"
}


print_tests() {
  echo "######################################################"
  echo "#                     RESULTS                        #"
  echo "######################################################"

  for TEST in $(echo $ITESTS | tr ',' '\n'); do
    TESTDIR=bigtop-tests/smoke-tests/$TEST/build

    if [ -d $TESTDIR ]; then
      cd $TESTDIR

      for FILE in $(find -L reports/tests/classes -type f -name "*.html"); do
        echo "## $TESTDIR/$FILE"
        if [ $(which links) ]; then
            links $FILE -dump
        else
            echo "PLEASE INSTALL LINKS: sudo yum -y install links"
        fi
        echo ""
      done
    fi
  done
}

# SET JAVA_HOME
set_java_home

# SET HADOOP SERVICE HOMES
set_hadoop_vars

echo "######################################################"
echo "#                 STARTING ITEST                     #"
echo "######################################################"
echo "# Use --debug/--info/--stacktrace for more details"

# SET THE DEFAULT TESTS
if [ -z "$ITESTS" ]; then
  export ITESTS="hcfs,hdfs,yarn,mapreduce"
fi
for s in `echo $ITESTS | sed -e 's#,# #g'`; do
  ALL_SMOKE_TASKS="$ALL_SMOKE_TASKS bigtop-tests:smoke-tests:$s:test"
done

# CALL THE GRADLE WRAPPER TO RUN THE FRAMEWORK
./gradlew -q clean test -Psmoke.tests $ALL_SMOKE_TASKS $@

# SHOW RESULTS (HTML)
print_tests
