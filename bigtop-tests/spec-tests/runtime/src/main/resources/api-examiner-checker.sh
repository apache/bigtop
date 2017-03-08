#!/usr/bin/env bash

############################################################################
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# <p>
# http://www.apache.org/licenses/LICENSE-2.0
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
############################################################################

function usage() {
    echo "You must set the following variables:  HADOOP_COMMON_HOME HADOOP_COMMON_DIR HADOOP_COMMON_LIB_JARS_DIR "
    echo "HADOOP_HDFS_HOME HDFS_DIR HDFS_LIB_JARS_DIR HADOOP_YARN_HOME YARN_DIR YARN_LIB_JARS_DIR "
    echo "HADOOP_MAPRED_HOME MAPRED_DIR MAPRED_LIB_JARS_DIR BIGTOP_HOME (location of bigtop source)"
    echo "You can get the Hadoop environment variables by using hadoop envvars, hdfs envvars, yarn envvars, and mapred envvars"
}

for envar in x$HADOOP_COMMON_HOME x$HADOOP_COMMON_DIR x$HADOOP_COMMON_LIB_JARS_DIR x$HADOOP_HDFS_HOME x$HDFS_DIR \
             x$HDFS_LIB_JARS_DIR x$HADOOP_YARN_HOME x$YARN_DIR x$YARN_LIB_JARS_DIR x$HADOOP_MAPRED_HOME x$MAPRED_DIR \
             x$MAPRED_LIB_JARS_DIR
do
    if [ "${envar}" = "x" ]
    then
        usage
        exit 1
    fi
done


for dir in $BIGTOP_HOME/bigtop-tests/spec-tests/runtime/build/libs/ $HADOOP_COMMON_HOME/$HADOOP_COMMON_DIR \
            $HADOOP_COMMON_HOME/$HADOOP_COMMON_LIB_JARS_DIR $HADOOP_HDFS_HOME/$HDFS_DIR \
            $HADOOP_HDFS_HOME/$HDFS_LIB_JARS_DIR $HADOOP_YARN_HOME/$YARN_DIR $HADOOP_YARN_HOME/$YARN_LIB_JARS_DIR \
            $HADOOP_MAPRED_HOME/$MAPRED_DIR $HADOOP_MAPRED_HOME/$MAPRED_LIB_JARS_DIR
do
    for jar in `find $dir -name \*.jar`
    do
        CLASSPATH=$CLASSPATH:$jar
    done
done

java -cp $CLASSPATH org.odpi.specs.runtime.hadoop.ApiExaminer $@

