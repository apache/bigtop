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
  case $::operatingsystem {
    /Debian/: {
      require apt
      require apt::backports

      package { 'openjdk-7-jdk' :
        ensure => present,
      }

      package { 'openjdk-8-jdk' :
        ensure => present,
      }

      exec { '/usr/sbin/update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac':
        require => Package['openjdk-7-jdk', 'openjdk-8-jdk']
      }
      exec { '/usr/sbin/update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java':
        require => Package['openjdk-7-jdk', 'openjdk-8-jdk']
      }
    }
    /Ubuntu/: {
      include apt

      package { 'openjdk-7-jdk' :
        ensure  => present,
        # needed for 16.04
        require => [ Apt::Ppa[ 'http://ppa.launchpad.net/openjdk-r/ppa/ubuntu'], Class['apt::update'] ]
      }
      package { 'openjdk-8-jdk' :
        ensure  => present,
        # needed for 14.04 
        require => [ Apt::Ppa[ 'http://ppa.launchpad.net/openjdk-r/ppa/ubuntu'], Class['apt::update'] ]
      }

      apt::key { 'openjdk-ppa':
        id     => 'eb9b1d8886f44e2a',
        server => 'keyserver.ubuntu.com'
      }  ->
      apt::ppa { 'http://ppa.launchpad.net/openjdk-r/ppa/ubuntu':  }

      exec { '/usr/bin/update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac':
        require => Package['openjdk-7-jdk', 'openjdk-8-jdk']
      }
      exec { '/usr/bin/update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java':
        require => Package['openjdk-7-jdk', 'openjdk-8-jdk']
      }
    }
    /(CentOS|Amazon)/: {
      package { 'java-1.7.0-openjdk-devel' :
        ensure => present
      }
      package { 'java-1.8.0-openjdk-devel' :
        ensure => present
      }
      # java 1.8 
    }
    /Fedora/: {
      if 0 + $::operatingsystemrelease < 21 {
        package { 'java-1.7.0-openjdk-devel' :
          ensure => present
        }
      }
      package { 'java-1.8.0-openjdk-devel' :
        ensure => present
      }
    }
    /OpenSuSE/: {
      package { 'java-1_7_0-openjdk-devel' :
        ensure => present
      }
      package { 'java-1_8_0-openjdk-devel' :
        ensure => present
      }
    }
  }
}
