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
      # We need JDK 8, but Debian 10+ only provides the openjdk-11-jdk package (or greater) in the official repo.
      # So we use Eclipse Temurin instead, following the steps described on:
      # https://adoptium.net/installation/linux/#_deb_installation_on_debian_or_ubuntu
      include apt

      apt::source { 'adoptium':
        location => 'https://packages.adoptium.net/artifactory/deb/',
        key      => {
          id     => '3B04D753C9050D9A5D343F39843C48A565F8F04B',
          source => 'https://packages.adoptium.net/artifactory/api/gpg/key/public',
        },
      } ->
      package { 'temurin-8-jdk' :
        ensure => present,
      }
    }
    /Ubuntu/: {
      include apt

      package { 'openjdk-8-jdk' :
        ensure  => present,
      }
    }
    /(CentOS|Amazon|Fedora|RedHat|Rocky|openEuler)/: {
      package { 'java-1.8.0-openjdk-devel' :
        ensure => present
      }
      if ($::operatingsystem == "Fedora") {
        file { '/usr/lib/jvm/java-1.8.0-openjdk/jre/lib/security/cacerts':
          ensure => 'link',
          target => '/etc/pki/java/cacerts'
        }
      }
    }
    /OpenSuSE/: {
      package { 'java-1_8_0-openjdk-devel' :
        ensure => present
      }
    }
  }
}
