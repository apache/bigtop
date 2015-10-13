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

class bigtop_toolchain::protobuf {

  include bigtop_toolchain::deps

  case $operatingsystem{
    /Ubuntu|Debian/: {
      case $architecture {
        'amd64' : { $url = "https://launchpad.net/ubuntu/+archive/primary/+files"
                    $arch= "amd64" }
        'ppc64le' : { $url = "https://bintray.com/artifact/download/oflebbe/bigtop-protobuf"
                    $arch= "ppc64el" }
      }
    }
  }
 
  case $operatingsystem{
    /Ubuntu|Debian/: {
      exec { "/usr/bin/wget $url/libprotobuf8_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        creates  => "/usr/src/libprotobuf8_2.5.0-9ubuntu1_$arch.deb",
      }
      exec { "/usr/bin/wget $url/libprotoc8_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        creates  => "/usr/src/libprotoc8_2.5.0-9ubuntu1_$arch.deb",
      }
      exec { "/usr/bin/wget $url/protobuf-compiler_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        creates  => "/usr/src/protobuf-compiler_2.5.0-9ubuntu1_$arch.deb",
      }
      exec {"/usr/bin/dpkg -i protobuf-compiler_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        require => [ EXEC["/usr/bin/dpkg -i libprotoc8_2.5.0-9ubuntu1_$arch.deb"],EXEC["/usr/bin/wget $url/protobuf-compiler_2.5.0-9ubuntu1_$arch.deb"] ]
      }
      exec {"/usr/bin/dpkg -i libprotoc8_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        require => [ EXEC["/usr/bin/dpkg -i libprotobuf8_2.5.0-9ubuntu1_$arch.deb"],EXEC["/usr/bin/wget $url/libprotoc8_2.5.0-9ubuntu1_$arch.deb"] ]
      }
      exec {"/usr/bin/dpkg -i libprotobuf8_2.5.0-9ubuntu1_$arch.deb":
        cwd     => "/usr/src",
        require => EXEC["/usr/bin/wget $url/libprotobuf8_2.5.0-9ubuntu1_$arch.deb"],
      }
    }
    default: {
      case $operatingsystem {
         /(?i:(centos|fedora|amazon))/: {
           yumrepo { "protobuf":
             baseurl => "http://download.opensuse.org/repositories/home:/mrdocs:/protobuf-rpm/CentOS_CentOS-6/",
             descr => "Bigtop protobuf repo",
             enabled => 1,
             priority => 1,
             gpgcheck => 0
           }
           exec { 'install_mrdocs_repo':
             command => '/bin/true',
             require => Yumrepo['protobuf'],
           }
         }
         /(?i:(SLES|opensuse))/:{
           exec { 'install_mrdocs_repo':
              command => '/usr/bin/zypper ar --no-gpgcheck http://download.opensuse.org/repositories/home:/mrdocs:/protobuf-rpm/openSUSE_12.3/ protobuf',
              unless => "/usr/bin/zypper lr | grep -q protobuf",
           }
         }
      }
      package { 'protobuf-devel':
        ensure => present,
        require => Exec['install_mrdocs_repo'],
      }
    }
  }
}
