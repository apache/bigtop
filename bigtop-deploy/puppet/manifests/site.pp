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

$default_yumrepo = "http://bigtop01.cloudera.org:8080/view/Releases/job/Bigtop-1.0.0-rpm/BUILD_ENVIRONMENTS=centos-6%2clabel=docker-slave-06/lastSuccessfulBuild/artifact/output/"
$default_debrepo = "http://bigtop01.cloudera.org:8080/view/Releases/job/Bigtop-1.0.0-deb/BUILD_ENVIRONMENTS=debian-8%2clabel=docker-slave-07/lastSuccessfulBuild/artifact/output/apt/"
$jdk_package_name = hiera("bigtop::jdk_package_name", "jdk")

stage {"pre": before => Stage["main"]}

case $operatingsystem {
    /(OracleLinux|Amazon|CentOS|Fedora|RedHat)/: {
       yumrepo { "Bigtop":
          baseurl => hiera("bigtop::bigtop_repo_uri", $default_yumrepo),
          descr => "Bigtop packages",
          enabled => 1,
          gpgcheck => 0,
       }
       Yumrepo<||> -> Package<||>
    }
    /(Ubuntu|Debian)/: {
       include apt
       apt::conf { "disable_keys":
          content => "APT::Get::AllowUnauthenticated 1;",
	  ensure => present
       }
       apt::source { "Bigtop":
          location => hiera("bigtop::bigtop_repo_uri", $default_debrepo),
          release => "bigtop",
          repos => "contrib",
          ensure => present,
        }
       Apt::Source<||> -> Exec['apt_update'] -> Package<||>
    }
    default: {
      notify{"WARNING: running on a neither yum nor apt platform -- make sure Bigtop repo is setup": }
    }
}

package { $jdk_package_name:
  ensure => "installed",
  alias => "jdk",
}

import "cluster.pp"

node default {
  $hadoop_head_node = hiera("bigtop::hadoop_head_node")
  $standby_head_node = hiera("bigtop::standby_head_node", "")
  $hadoop_gateway_node = hiera("bigtop::hadoop_gateway_node", $hadoop_head_node)

  # look into alternate hiera datasources configured using this path in
  # hiera.yaml
  $hadoop_hiera_ha_path = $standby_head_node ? {
    ""      => "noha",
    default => "ha",
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

if versioncmp($::puppetversion,'3.6.1') >= 0 {
  $allow_virtual_packages = hiera('bigtop::allow_virtual_packages',false)
  Package {
    allow_virtual => $allow_virtual_packages,
  }
}
