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

require bigtop_util
$puppet_confdir = get_setting("confdir")
$default_yumrepo = "http://bigtop01.cloudera.org:8080/view/Hadoop%200.23/job/Bigtop-23-matrix/label=centos5/lastSuccessfulBuild/artifact/output/"
$extlookup_datadir="$puppet_confdir/config"
$extlookup_precedence = ["site", "default"]
$jdk_package_name = extlookup("jdk_package_name", "jdk")

stage {"pre": before => Stage["main"]}

case $operatingsystem {
    /(OracleLinux|CentOS|Fedora|RedHat)/: {
       yumrepo { "Bigtop":
          baseurl => extlookup("bigtop_yumrepo_uri", $default_yumrepo),
          descr => "Bigtop packages",
          enabled => 1,
          gpgcheck => 0,
       }
    }
    default: {
      notify{"WARNING: running on a non-yum platform -- make sure Bigtop repo is setup": }
    }
}

package { $jdk_package_name:
  ensure => "installed",
  alias => "jdk",
}

import "cluster.pp"

node default {
  $hadoop_head_node = extlookup("hadoop_head_node") 
  $standby_head_node = extlookup("standby_head_node", "")
  $hadoop_gateway_node = extlookup("hadoop_gateway_node", $hadoop_head_node)

  exec { "rm HBase protobuf":
      command => "/bin/bash -c 'rm /usr/lib/hbase/lib/protobuf* ; grep -q components bigtop-deploy/puppet/config/site.csv || echo components,hdfs,hbase,yarn,flume,oozie,sqoop,mapred-app,zookeeper,spark,hive,pig,crunch,mahout,hue,httpfs >> bigtop-deploy/puppet/config/site.csv'",
  }

  case $::fqdn {
    $hadoop_head_node: {
      include hadoop_head_node
    }
    $standby_head_node: {
      include standby_head_node
    }
    default: {
      include hadoop_worker_node
    }
  }

  if ($hadoop_gateway_node == $::fqdn) {
    include hadoop_gateway_node
  }
}

Yumrepo<||> -> Package<||>
