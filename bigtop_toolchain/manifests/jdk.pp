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

class bigtop_toolchain::jdk {
  case $::lsbdistcodename{
    /(precise|trusty|lucid)/: {
      $apt_add_repo_name = $::lsbdistcodename ? {
        'trusty'          => 'software-properties-common',
        default           => 'python-software-properties',
      }

      package {$apt_add_repo_name:
        ensure => present,
      }

      exec {'add_webupd8team_ppa':
        command => $::lsbdistcodename ? { 
                      'lucid' => '/usr/bin/apt-add-repository    ppa:webupd8team/java',
                      default => '/usr/bin/apt-add-repository -y ppa:webupd8team/java'
                   },
        unless  => '/usr/bin/test -f /etc/apt/sources.list.d/webupd8team-java-precise.list',
        require => Package[$apt_add_repo_name],
      }

      exec {'/usr/bin/apt-get update':
        refreshonly => true,
        subscribe   => Exec['add_webupd8team_ppa'],
        require     => Exec['add_webupd8team_ppa'],
      }

      exec {"accept-license1":
        command     => "echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections",
        path        => ["/bin", "/usr/bin"],
        require     => Exec['/usr/bin/apt-get update'],
        refreshonly => true,
        subscribe   => Exec['/usr/bin/apt-get update'],
      }

      exec {"accept-license2":
        command     => "echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections",
        path        => ["/bin", "/usr/bin"],
        require     => Exec["accept-license1"],
        refreshonly => true,
        subscribe   => Exec["accept-license1"],
      }

      package {'oracle-java6-installer':
        ensure  => present,
        require => Exec["accept-license2"],
      }

      package {'oracle-java7-installer':
        ensure  => present,
        require => Exec["accept-license2"],
      }

    }
    default: {
      file { '/tmp/jdk-6u45-linux-amd64.rpm':
        source => 'puppet:///modules/bigtop_toolchain/jdk-6u45-linux-amd64.rpm',
        ensure => present,
        owner  => root,
        group  => root,
        mode   => 755
      }
  
      exec {'/bin/rpm -Uvh /tmp/jdk-6u45-linux-amd64.rpm':
        cwd         => '/tmp',
        refreshonly => true,
        subscribe   => File["/tmp/jdk-6u45-linux-amd64.rpm"],
      }

      file { '/tmp/jdk-7u60-linux-x64.gz':
        source => 'puppet:///modules/bigtop_toolchain/jdk-7u60-linux-x64.gz',
        ensure => present,
        owner  => root,
        group  => root,
        mode   => 755
      }

      exec {'/bin/tar -xzvf /tmp/jdk-7u60-linux-x64.gz; ln -s jdk1.7.0_60 jdk7-latest':
        cwd         => '/usr/lib',
        refreshonly => true,
        subscribe   => File["/tmp/jdk-7u60-linux-x64.gz"],
      }

    }
  }
}
