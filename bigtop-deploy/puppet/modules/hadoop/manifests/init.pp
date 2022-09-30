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

class hadoop ($hadoop_security_authentication = "simple",
  $kerberos_realm = undef,
  $zk = "",
  # Set from facter if available
  $hadoop_storage_dirs = split($::hadoop_storage_dirs, ";"),
  $proxyusers = {
    spark => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
    oozie => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
     hive => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
   httpfs => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" } },
  $generate_secrets = false,
  $kms_host = undef,
  $kms_port = undef,
) {

  include stdlib

  class deploy ($roles) {

    if ("datanode" in $roles) {
      include hadoop::datanode
    }

    if ("namenode" in $roles) {
      include hadoop::init_hdfs
      include hadoop::namenode

      if ("datanode" in $roles) {
        Class['Hadoop::Namenode'] -> Class['Hadoop::Datanode'] -> Class['Hadoop::Init_hdfs']
      } else {
        Class['Hadoop::Namenode'] -> Class['Hadoop::Init_hdfs']
      }
    }

    if ("standby-namenode" in $roles and $hadoop::common_hdfs::ha != "disabled") {
      include hadoop::namenode
    }

    if ("mapred-app" in $roles) {
      include hadoop::mapred_app
    }

    if ("nodemanager" in $roles) {
      include hadoop::nodemanager
    }

    if ("resourcemanager" in $roles) {
      include hadoop::resourcemanager
      include hadoop::historyserver
      include hadoop::proxyserver

      if ("nodemanager" in $roles) {
        Class['Hadoop::Resourcemanager'] -> Class['Hadoop::Nodemanager']
      }
    }

    if ("secondarynamenode" in $roles and $hadoop::common_hdfs::ha == "disabled") {
      include hadoop::secondarynamenode
    }

    if ("httpfs-server" in $roles) {
      include hadoop::httpfs
    }

    if ("kms" in $roles) {
      include hadoop::kms
    }

    if ("hadoop-client" in $roles) {
      include hadoop::client
    }
  }


  define create_storage_dir {
    exec { "mkdir $name":
      command => "/bin/mkdir -p $name",
      creates => $name,
      user =>"root",
    }
  }

  define namedir_copy ($source, $ssh_identity) {
    exec { "copy namedir $title from first namenode":
      command => "/usr/bin/rsync -avz -e '/usr/bin/ssh -oStrictHostKeyChecking=no -i $ssh_identity' '${source}:$title/' '$title/'",
      user    => "hdfs",
      tag     => "namenode-format",
      creates => "$title/current/VERSION",
    }
  }

}
