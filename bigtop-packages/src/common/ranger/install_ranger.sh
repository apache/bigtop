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

set -e
set -x

prefix=$1
version=$2

ranger_admin_dir="${prefix}/usr/lib/ranger-admin"
ranger_atlas_plugin_dir="${prefix}/usr/lib/ranger-atlas-plugin"

install -d -m 0755 "${prefix}/usr/lib/"
install -d -m 0755 "${prefix}/var/log/ranger"

tar xf "target/ranger-${version}-admin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-atlas-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-hbase-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-hdfs-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-hive-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-kafka-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-kms.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-knox-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-solr-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-storm-plugin.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-tagsync.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-usersync.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/ranger-${version}-yarn-plugin.tar.gz" -C "${prefix}/usr/lib/"


mv "${prefix}/usr/lib/ranger-${version}-admin" "${ranger_admin_dir}"

mkdir -p ${prefix}/usr/lib/atlas-server
cp -r ${prefix}/usr/lib/ranger-${version}-atlas-plugin/lib ${prefix}/usr/lib/atlas-server/libext
mv ${prefix}/usr/lib/ranger-${version}-atlas-plugin ${prefix}/usr/lib/ranger-atlas-plugin

mkdir -p ${prefix}/usr/lib/hbase
cp -r  ${prefix}/usr/lib/ranger-${version}-hbase-plugin/lib ${prefix}/usr/lib/hbase/lib
mv ${prefix}/usr/lib/ranger-${version}-hbase-plugin ${prefix}/usr/lib/ranger-hbase-plugin

mkdir -p ${prefix}/usr/lib/hadoop
cp -r ${prefix}/usr/lib/ranger-${version}-hdfs-plugin/lib ${prefix}/usr/lib/hadoop/lib
mv  ${prefix}/usr/lib/ranger-${version}-hdfs-plugin ${prefix}/usr/lib/ranger-hdfs-plugin 

mkdir -p  ${prefix}/usr/lib/hive
cp -r ${prefix}/usr/lib/ranger-${version}-hive-plugin/lib  ${prefix}/usr/lib/hive/lib
mkdir -p  ${prefix}/usr/lib/hive2
cp -r ${prefix}/usr/lib/ranger-${version}-hive-plugin/lib  ${prefix}/usr/lib/hive2/lib
mv ${prefix}/usr/lib/ranger-${version}-hive-plugin  ${prefix}/usr/lib/ranger-hive-plugin 

mkdir -p ${prefix}/usr/lib/kafka
cp -r ${prefix}/usr/lib/ranger-${version}-kafka-plugin/lib  ${prefix}/usr/lib/kafka/libs
mv  ${prefix}/usr/lib/ranger-${version}-kafka-plugin  ${prefix}/usr/lib/ranger-kafka-plugin

mv ${prefix}/usr/lib/ranger-${version}-kms ${prefix}/usr/lib/ranger-kms

mkdir -p ${prefix}/usr/lib/knox
cp -r ${prefix}/usr/lib/ranger-${version}-knox-plugin/lib ${prefix}/usr/lib/knox/ext
mv ${prefix}/usr/lib/ranger-${version}-knox-plugin ${prefix}/usr/lib/ranger-knox-plugin

mkdir -p ${prefix}/usr/lib/solr
cp -r ${prefix}/usr/lib/ranger-${version}-solr-plugin/lib ${prefix}/usr/lib/solr/lib
mv ${prefix}/usr/lib/ranger-${version}-solr-plugin ${prefix}/usr/lib/ranger-solr-plugin

mkdir -p ${prefix}/usr/lib/storm
cp -r  ${prefix}/usr/lib/ranger-${version}-storm-plugin/lib ${prefix}/usr/lib/storm/extlib-daemon
mv  ${prefix}/usr/lib/ranger-${version}-storm-plugin ${prefix}/usr/lib/ranger-storm-plugin

mv ${prefix}/usr/lib/ranger-${version}-tagsync ${prefix}/usr/lib/ranger-tagsync

mv ${prefix}/usr/lib/ranger-${version}-usersync ${prefix}/usr/lib/ranger-usersync

mv  ${prefix}/usr/lib/ranger-${version}-yarn-plugin ${prefix}/usr/lib/ranger-yarn-plugin 


