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
yum -y install puppet-2.7.24-1.el6.noarch

sysctl kernel.hostname=`hostname -f`

# Unmount device /etc/hosts and replace it by a shared hosts file
echo -e "`hostname -i`\t`hostname -f`" >> /vagrant/hosts
umount /etc/hosts
mv /etc/hosts /etc/hosts.bak
ln -s /vagrant/hosts /etc/hosts

# Prepare puppet configuration file
cat > /bigtop-puppet/config/site.csv << EOF
hadoop_head_node,$1
hadoop_storage_dirs,/data/1,/data/2
bigtop_yumrepo_uri,http://bigtop01.cloudera.org:8080/view/Releases/job/Bigtop-0.8.0/label=centos6/6/artifact/output/
jdk_package_name,java-1.7.0-openjdk-devel.x86_64
components,hadoop,hbase,yarn,mapred-app
EOF

mkdir -p /data/{1,2}

# Setup rng-tools to improve virtual machine entropy performance.
# The poor entropy performance will cause kerberos provisioning failed.
yum -y install rng-tools
sed -i.bak 's/EXTRAOPTIONS=\"\"/EXTRAOPTIONS=\"-r \/dev\/urandom\"/' /etc/sysconfig/rngd
service rngd start
