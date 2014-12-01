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
class bigtop_toolchain::scala {

  case $operatingsystem{
    Debian: {
      package { 'scala' :
        ensure => present
      }
    }
    default: {
      $install_scala_cmd = $operatingsystem ? {
        'Ubuntu'               => '/bin/bash -c "wget http://www.scala-lang.org/files/archive/scala-2.10.3.deb ; dpkg -x ./scala-2.10.3.deb /"',
        /(?i:(SLES|opensuse))/ => '/usr/bin/zypper install -y http://www.scala-lang.org/files/archive/scala-2.10.3.rpm',
        default                => '/bin/rpm -U http://www.scala-lang.org/files/archive/scala-2.10.3.rpm'
      }
      exec { "install scala":
        cwd      => '/tmp',
        command  => $install_scala_cmd,
        unless   => "/usr/bin/test -f /usr/bin/scala",
        require  => $requires
      }
    }
  }
}
