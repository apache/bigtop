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
  $bigtop_repo_default_version = hiera("bigtop::bigtop_repo_default_version")
  $bigtop_repo_gpg_check = hiera("bigtop::bigtop_repo_gpg_check", true)
  $lower_os = downcase($operatingsystem)
  $default_repo = "http://repos.bigtop.apache.org/releases/${bigtop_repo_default_version}/${lower_os}/${operatingsystemmajrelease}/${architecture}"

  case $::operatingsystem {
    /(OracleLinux|Amazon|CentOS|Fedora|RedHat)/: {
      $baseurls_array = any2array(hiera("bigtop::bigtop_repo_uri", $default_repo))
      each($baseurls_array) |$count, $baseurl| {
        notify { "Baseurl: $baseurl": }

        if ($bigtop_repo_gpg_check) {
          yumrepo { "Bigtop_$count":
            baseurl  => $baseurl,
            descr    => "Bigtop packages",
            enabled  => 1,
            gpgcheck => 1,
            gpgkey   => hiera("bigtop::bigtop_repo_yum_key_url"),
            priority => 10,
            ensure  => present,
          }
        } else {
          yumrepo { "Bigtop_$count":
            baseurl  => $baseurl,
            descr    => "Bigtop packages",
            enabled  => 1,
            gpgcheck => 0,
            priority => 10,
            ensure  => present,
          }
        }
      }
      Yumrepo<||> -> Package<||>
    }

    /(Ubuntu|Debian)/: {
      include stdlib
      include apt
      $baseurls_array = any2array(hiera("bigtop::bigtop_repo_uri", $default_repo))

      each($baseurls_array) |$count, $baseurl| {
        notify { "Baseurl: $baseurl": }

        apt::source { "Bigtop_$count":
          location => $baseurl,
          release  => "bigtop",
          repos    => "contrib",
          # BIGTOP-2796. Give Bigtop repo higher priority to solve zookeeper package conflict probem on Ubuntu
          pin      => "900",
          ensure   => present,
        }
      }

      # BIGTOP-3343. This is a JDK-related stuff, so it should be in jdk.pp ordinarily.
      # But it looks like that this definition must be here to avoid cyclic resource dependencies.
      if ($operatingsystem == 'Debian' and 0 <= versioncmp($operatingsystemrelease, "10")) {
        apt::source { 'adoptopenjdk':
          location => 'https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/',
        }
      }

      # It seems that calling update explicitely isn't needed because as far I can see
      # it is getting called automatically. Perhaps this was needed for older versions?
      exec { 'bigtop-apt-update':
        command => '/usr/bin/apt-get update'
      }

      if ($bigtop_repo_gpg_check) {
        apt::conf { "remove_disable_keys":
          content => "APT::Get::AllowUnauthenticated 1;\nAcquire::AllowInsecureRepositories \"true\";",
          ensure  => absent
        }
        apt::key { "add_key":
          id => hiera("bigtop::bigtop_repo_apt_key"),
        }

        # BIGTOP-3343. This is a JDK-related stuff, but it's here for the same reason described above.
        if ($operatingsystem == 'Debian' and 0 <= versioncmp($operatingsystemrelease, "10")) {
          apt::key { "add_adoptopenjdk_key":
            id => "8ED17AF5D7E675EB3EE3BCE98AC3B29174885C03",
            source => "https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public",
          }
        }
      } else {
        apt::conf { "disable_keys":
          content => "APT::Get::AllowUnauthenticated 1;\nAcquire::AllowInsecureRepositories \"true\";",
          ensure  => present
        }
      }
      Apt::Conf<||> -> Apt::Key<||> -> Apt::Source<||> -> Exec['bigtop-apt-update'] -> Package<||>
    }
    default: {
      notify { "WARNING: running on a neither yum nor apt platform -- make sure Bigtop repo is setup": }
    }
  }
}
