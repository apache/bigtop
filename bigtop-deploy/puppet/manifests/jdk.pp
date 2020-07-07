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

$jdk_preinstalled = hiera("bigtop::jdk_preinstalled", false)

class jdk {
  case $::operatingsystem {
    /Debian/: {
      require apt

      if versioncmp($operatingsystemrelease, "10") < 0 {
        $jdk_pkg_name = 'openjdk-8-jdk'
      } else {
        $jdk_pkg_name = 'adoptopenjdk-8-hotspot'
      }
      package { 'jdk':
        name => $jdk_pkg_name,
        ensure => present,
        noop => $jdk_preinstalled,
      }

     }
    /Ubuntu/: {
       include apt

      package { 'jdk':
        name => 'openjdk-8-jdk',
        ensure  => present,
        noop => $jdk_preinstalled,
      }
    }
    /(CentOS|Amazon|Fedora)/: {
      package { 'jdk':
        name => 'java-1.8.0-openjdk-devel',
        ensure => present,
        noop => $jdk_preinstalled,
      }
    }
    /OpenSuSE/: {
      package { 'jdk':
        name => 'java-1_8_0-openjdk-devel',
        ensure => present,
        noop => $jdk_preinstalled,
      }
    }
  }
}
