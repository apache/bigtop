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

java -cp $CLASSPATH org.odpi.specs.runtime.hadoop.ApiExaminer $@

