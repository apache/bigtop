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

# Prepare default repo by detecting the environment automatically
case $operatingsystem {
    # Use CentOS 7 repo for other CentOS compatible OSs
    /(OracleLinux|Amazon|RedHat)/: {
      $default_repo = "http://bigtop-repos.s3.amazonaws.com/releases/1.0.0/centos/7/x86_64"
    }
    # Detect env to pick up default repo for other Bigtop supported OSs
    default: {
      $lower_os = downcase($operatingsystem)
      # We use code name such as trusty for Ubuntu instead of release version in bigtop's binary convenience repos
      if ($operatingsystem == "Ubuntu") { $release = $lsbdistcodename } else { $release = $operatingsystemmajrelease }
      $default_repo = "http://bigtop-repos.s3.amazonaws.com/releases/1.0.0/${lower_os}/${release}/x86_64"
    }
}

$jdk_preinstalled = hiera("bigtop::jdk_preinstalled", false)
$jdk_package_name = hiera("bigtop::jdk_package_name", "jdk")

stage {"pre": before => Stage["main"]}

case $operatingsystem {
    /(OracleLinux|Amazon|CentOS|Fedora|RedHat)/: {
       yumrepo { "Bigtop":
          baseurl => hiera("bigtop::bigtop_repo_uri", $default_repo),
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
          location => hiera("bigtop::bigtop_repo_uri", $default_repo),
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
  noop => $jdk_preinstalled,
}

import "cluster.pp"

node default {

  $roles_enabled = hiera("bigtop::roles_enabled", false)

  if (!is_bool($roles_enabled)) {
    fail("bigtop::roles hiera conf is not of type boolean. It should be set to either true or false")
  }

  if ($roles_enabled) {
    include node_with_roles
  } else {
    include node_with_components
  }
}

if versioncmp($::puppetversion,'3.6.1') >= 0 {
  $allow_virtual_packages = hiera('bigtop::allow_virtual_packages',false)
  Package {
    allow_virtual => $allow_virtual_packages,
  }
}
