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

  case $operatingsystem{
    /Ubuntu|Debian/: {
      case $architecture {
        'amd64' : { $url = "https://launchpad.net/ubuntu/+source/protobuf/2.5.0-9ubuntu1/+build/5585371/+files/"
                    $arch= "amd64" }
        'ppc64le' : { $url = "https://launchpad.net/ubuntu/+source/protobuf/2.5.0-9ubuntu1/+build/5604345/+files"
                    $arch= "ppc64el" }
        'aarch64' : { $url = "https://launchpad.net/ubuntu/+source/protobuf/2.5.0-9ubuntu1/+build/5585372/+files"
                    $arch= "arm64" }
      }
    }
  }
 
  case $operatingsystem{
    /Ubuntu|Debian/: {
      $libprotobuf8 = "libprotobuf8_2.5.0-9ubuntu1_$arch.deb"
      $libprotoc8 = "libprotoc8_2.5.0-9ubuntu1_$arch.deb"
      $protobuf_compiler = "protobuf-compiler_2.5.0-9ubuntu1_$arch.deb"

      exec { "download protobuf":
        cwd     => "/usr/src",
        command => "/usr/bin/curl -L $url/$libprotobuf8 -o $libprotobuf8; /usr/bin/curl -L $url/$libprotoc8 -o $libprotoc8; /usr/bin/curl -L $url/$protobuf_compiler -o $protobuf_compiler",
        creates  => [ "/usr/src/$libprotobuf8", "/usr/src/$libprotoc8", "/usr/src/$protobuf_compiler" ]
      }
      exec { "install protobuf":
        cwd     => "/usr/src",
        command => "/usr/bin/dpkg -i $libprotobuf8 $libprotoc8 $protobuf_compiler",
        require => EXEC["download protobuf"],
      }
    }
    default: {
      case $operatingsystem {
         /(?i:(centos|fedora|amazon))/: {
          case $architecture {
           'ppc64le' : { 
            exec { 'install_mrdocs_repo':
              command => "/usr/bin/rpm -ivh https://dl.fedoraproject.org/pub/fedora-secondary/releases/22/Everything/ppc64le/os/Packages/p/protobuf-2.5.0-11.fc22.ppc64le.rpm;/usr/bin/rpm -ivh https://dl.fedoraproject.org/pub/fedora-secondary/releases/22/Everything/ppc64le/os/Packages/p/protobuf-compiler-2.5.0-11.fc22.ppc64le.rpm;/usr/bin/rpm -ivh https://dl.fedoraproject.org/pub/fedora-secondary/releases/22/Everything/ppc64le/os/Packages/p/protobuf-devel-2.5.0-11.fc22.ppc64le.rpm",
            }
            $package_name = 'protobuf-devel'
           }
             
           default: { yumrepo { "protobuf":
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
           $package_name = 'protobuf-devel'
           }
          }
         }
         /(?i:(SLES|opensuse))/:{
           exec { 'install_mrdocs_repo':
              command => '/usr/bin/zypper ar --no-gpgcheck http://download.opensuse.org/repositories/home:/mrdocs:/protobuf-rpm/openSUSE_Leap_42.1/ protobuf',
              unless => "/usr/bin/zypper lr | grep -q protobuf",
           }
           $package_name = 'protobuf-devel-2.5.0-4.1'
         }
      }
      package { $package_name:
        ensure => present,
        require => Exec['install_mrdocs_repo'],
      }
    }
  }
}
