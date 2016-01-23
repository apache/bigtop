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

sysctl kernel.hostname=`hostname -f`

# Unmount device /etc/hosts and replace it by a shared hosts file
echo -e "`hostname -i`\t`hostname -f`" >> /vagrant/hosts
umount /etc/hosts
mv /etc/hosts /etc/hosts.bak
ln -s /vagrant/hosts /etc/hosts

# Prepare puppet configuration file
mkdir -p /etc/puppet/hieradata
cp /bigtop-home/bigtop-deploy/puppet/hiera.yaml /etc/puppet
cp -r /bigtop-home/bigtop-deploy/puppet/hieradata/bigtop/ /etc/puppet/hieradata/
cat > /etc/puppet/hieradata/site.yaml << EOF
bigtop::hadoop_head_node: $1
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: $2
hadoop_cluster_node::cluster_components: $3
bigtop::jdk_package_name: $4
hadoop::common_hdfs::testonly_hdfs_sshkeys: "yes"
EOF
