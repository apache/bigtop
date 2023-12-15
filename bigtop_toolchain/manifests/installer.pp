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

class bigtop_toolchain::installer {
  include bigtop_toolchain::jdk11
  include bigtop_toolchain::jdk
  include bigtop_toolchain::maven
  include bigtop_toolchain::ant
  include bigtop_toolchain::gradle
  include bigtop_toolchain::protobuf
  include bigtop_toolchain::packages
  include bigtop_toolchain::env
  include bigtop_toolchain::user
  include bigtop_toolchain::renv
  include bigtop_toolchain::grpc
  include bigtop_toolchain::isal
  Class['bigtop_toolchain::jdk11']->Class['bigtop_toolchain::jdk']

  case $::operatingsystem {
    /Debian/: {
      exec { 'ensure java 8 is set as default':
        command => "update-java-alternatives --set temurin-8*",
        path    => ['/usr/sbin', '/usr/bin', '/bin'],
        require => Class['bigtop_toolchain::jdk'],
      }
    }
    /Ubuntu/: {
      exec { 'ensure java 8 is set as default':
        command => "update-java-alternatives --set java-1.8.0-openjdk-$(dpkg --print-architecture)",
        path    => ['/usr/sbin', '/usr/bin', '/bin'],
        require => Class['bigtop_toolchain::jdk'],
      }
    }
    /(CentOS|Fedora|RedHat|Rocky)/: {
      exec { 'ensure java 8 is set as default':
        command => "update-alternatives --set java java-1.8.0-openjdk.$(uname -m) \
                    && update-alternatives --set javac java-1.8.0-openjdk.$(uname -m)",
        path    => ['/usr/sbin', '/usr/bin', '/bin'],
        require => Class['bigtop_toolchain::jdk'],
      }
    }
  }

  stage { 'last':
    require => Stage['main'],
  }
  class { 'bigtop_toolchain::cleanup': stage => 'last' }
}
