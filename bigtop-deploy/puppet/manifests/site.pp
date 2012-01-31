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
$default_yumrepo = "http://bigtop01.cloudera.org:8080/view/RCs/job/Bigtop-trunk-rc-zookeeper-3.4.0/label=centos5/lastSuccessfulBuild/artifact/output/"
$extlookup_datadir="$puppet_confdir/config"
$extlookup_precedence = ["site", "default"]

stage {"pre": before => Stage["main"]}

yumrepo { "Bigtop":
    baseurl => extlookup("bigtop_yumrepo_uri", $default_yumrepo),
    descr => "Bigtop packages",
    enabled => 1,
    gpgcheck => 0,
}

package { "jdk":
   ensure => "installed",
}

# $hadoop_head_node="beefy.my.org"
# $hadoop_security_authentication="kerberos"

import "cluster.pp"

node default {
  # Fails if hadoop_head_node is unset
  if (!$::hadoop_head_node) {
    $hadoop_head_node = extlookup("hadoop_head_node") 
  }

  if ($hadoop_head_node == $fqdn) {
    include hadoop_gateway_node
  } else {
    include hadoop_worker_node
  }
}

Yumrepo<||> -> Package<||>
