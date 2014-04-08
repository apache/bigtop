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

# Install puppet agent
yum -y install http://yum.puppetlabs.com/puppetlabs-release-el-6.noarch.rpm
yum -y install puppet-2.7.23-1.el6.noarch

service iptables stop
chkconfig iptables off
cat /dev/null > /etc/hosts

# Prepare puppet configuration file
cat > /bigtop-puppet/config/site.csv << EOF
hadoop_head_node,$1
hadoop_storage_dirs,/data/1,/data/2
bigtop_yumrepo_uri,http://bigtop.s3.amazonaws.com/releases/0.7.0/redhat/6/x86_64
jdk_package_name,java-1.7.0-openjdk-devel.x86_64
components,hadoop,hbase
EOF

mkdir -p /data/{1,2}
