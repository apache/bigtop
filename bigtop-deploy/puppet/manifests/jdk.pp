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
    /(CentOS|Amazon|Fedora|RedHat)/: {
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

define jdk::add($version) {
  case $::operatingsystem {
    /Debian/: {
      include apt

      package { "openjdk-${version}-jdk" :
        ensure  => present,
      }
    }
    /Ubuntu/: {
      include apt

      package { "openjdk-${version}-jdk" :
        ensure  => present,
      }
    }
    /(CentOS|Fedora|RedHat)/: {
      package { "java-${version}-openjdk-devel" :
        ensure => present
      }
    }
  }
}

define jdk::use($version) {
  case $::operatingsystem {
    /Debian/: {
      if versioncmp($version, "9") < 0 {
        exec { "use java ${version}":
          command => "update-java-alternatives --set adoptopenjdk-${version}-$(dpkg --print-architecture)",
          path    => ['/usr/sbin', '/usr/bin', '/bin'],
        }
      } else {
        exec { "use java ${version}":
          command => "update-java-alternatives --set java-1.${version}.0-openjdk-$(dpkg --print-architecture)",
          path    => ['/usr/sbin', '/usr/bin', '/bin'],
        }
      }
    }
    /Ubuntu/: {
      exec { "use java ${version}":
        command => "update-java-alternatives --set java-1.${version}.0-openjdk-$(dpkg --print-architecture)",
        path    => ['/usr/sbin', '/usr/bin', '/bin'],
      }
    }
    /(CentOS|Fedora|RedHat)/: {
      if versioncmp($version, "9") < 0 {
        $version_id = "1.$version.0"
      } else {
        $version_id = "$version"
      }
      exec { "use java ${version}":
        command => "update-alternatives --set java java-${version_id}-openjdk.$(uname -m) \
                    && update-alternatives --set javac java-${version_id}-openjdk.$(uname -m)",
        path    => ['/usr/sbin', '/usr/bin', '/bin'],
      }
    }
  }
}

define jdk::set_profile($component, $version) {
  $upcased_component = upcase($component)
  file { "/etc/profile.d/bigtop_${component}_javaversion.sh":
    content => "export BIGTOP_${upcased_component}_JAVA_VERSION=${version}\n",
    owner  => root,
    group  => root,
    mode   => "644",
  }
}