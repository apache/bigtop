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

class bigtop_repo {
  case $::operatingsystem {
    /(OracleLinux|Amazon|CentOS|Fedora|RedHat)/: {
      $default_repo = "http://repos.bigtop.apache.org/releases/1.2.1/centos/7/x86_64"
      $baseurls_array = any2array(hiera("bigtop::bigtop_repo_uri", $default_repo))
      each ($baseurls_array) |$count, $baseurl| {
        yumrepo { "Bigtop_$count":
          baseurl  => $baseurl,
          descr    => "Bigtop packages",
          enabled  => 1,
          gpgcheck => 0,
        }
        Yumrepo<||> -> Package<||>
      }
    }

    /(Ubuntu|Debian)/: {
       include stdlib
       include apt

       $lower_os = downcase($operatingsystem)
       # We use code name such as trusty for Ubuntu instead of release version in bigtop's binary convenience repos
       if ($operatingsystem == "Ubuntu") { $release = $lsbdistcodename } else { $release = $operatingsystemmajrelease }
       $default_repo = "http://repos.bigtop.apache.org/releases/1.2.1/${lower_os}/${release}/x86_64"
       $baseurls_array = any2array(hiera("bigtop::bigtop_repo_uri", $default_repo))

      # I couldn't enforce the sequence -> anymore because of this
      # https://twitter.com/c0sin/status/875869783979196416
       apt::conf { "disable_keys":
          content => "APT::Get::AllowUnauthenticated 1;",
          ensure => present
       }
       each ($baseurls_array) |$count, $baseurl| {
         notify {"Baseurl: $baseurl" :}
         apt::source { "Bigtop_$count":
            location => $baseurl,
            release => "bigtop",
            repos => "contrib",
            # BIGTOP-2796. Give Bigtop repo higher priority to solve zookeeper package conflict probem on Ubuntu
            pin => "900",
            ensure => present,
         }
       }
      # It seems that calling update explicitely isn't needed because as far I can see
      # it is getting called automatically. Perhaps this was needed for older versions?
       exec {'bigtop-apt-update':
          command => '/usr/bin/apt-get update'
       }
       Apt::Source<||> -> Exec['bigtop-apt-update'] -> Package<||>
    }
    default: {
      notify{"WARNING: running on a neither yum nor apt platform -- make sure Bigtop repo is setup": }
    }
  }
}
