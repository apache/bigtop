#!/usr/bin/env bash

############################################################################
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
############################################################################

############################################################################
# This script is used to generate the hadoop-*-api.report.json files in the
# test/resources directory.  To use it, you will first need to download an
# Apache binary distribution of Hadoop and set APACHE_HADOOP_DIR to the
# directory where you untar that distribution.  You will then need to set
# BIGTTOP_HOME to the directory where your bigtop source is located.  Then
# run this script for each of the jars you want to generate a report for.
# The arguments passed to this script should be -p <outputdir> -j <jarfile>
# where outputdir is the directory you'd like to write the report to and
# jarfile is the full path of the jar to generate the report for.  Reports
# should be generated for the following jars: hadoop-common, hadoop-hdfs,
# hadoop-yarn-common, hadoop-yarn-client, hadoop-yarn-api, and
# hadoop-mapreduce-client-core
#
# Example usage:
# export APACHE_HADOOP_DIR=/tmp/hadoop-2.7.3
# export BIGTOP_HOME=/home/me/git/bigtop
# $BIGTOP_HOME/bigtop-tests/spec-tests/runtime/src/main/resources/api-examiner.sh -j $HADOOP_HOME/share/hadoop/common/hadoop-common-2.7.3.jar -p $BIGTOP_HOME/bigtop-tests/spec-tests/runtime/src/test/resources
#
# The resulting reports should be committed to git.  This script only needs
# to be run once per Bigtop release.
############################################################################


if [ "x${APACHE_HADOOP_DIR}" = "x" ]
then
    echo "You must set APACHE_HADOOP_DIR to the directory you have placed the Apache Hadoop binary distribution in"
    exit 1
fi

if [ "x${BIGTOP_HOME}" = "x" ]
then
    echo "You must set BIGTOP_HOME to the root directory for your bigtop source"
    exit 1
fi

for jar in `find $BIGTOP_HOME/bigtop-tests/spec-tests/runtime/build/libs/ -name \*.jar`
do
    CLASSPATH=$CLASSPATH:$jar
done

for jar in `find $APACHE_HADOOP_DIR -name \*.jar`
do
    CLASSPATH=$CLASSPATH:$jar
done

java -cp $CLASSPATH org.apache.bigtop.itest.hadoop.api.ApiExaminer $@

