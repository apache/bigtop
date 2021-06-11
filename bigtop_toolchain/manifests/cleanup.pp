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

class bigtop_toolchain::cleanup {
  $packager_cleanup = $operatingsystem ? {
    /(?i:(centos|fedora|redhat|amazon))/ => 'yum clean all',
    /(?i:(SLES|opensuse))/ => 'zypper clean -a',
    /Ubuntu|Debian/        => 'apt-get clean',
  } 

  if ($operatingsystem == 'fedora' and versioncmp($operatingsystemmajrelease, '33')) {
    exec { "Restore JDK to 8":
      command => "/usr/sbin/alternatives --set java /usr/lib/jvm/java-1.8.0-openjdk-1.8.*/jre/bin/java",
    }
  }
  
  exec { 'remove archives':
    cwd         => '/usr/src',
    command     => '/bin/rm -f *.deb *.zip *.tar.gz'
  }

  exec { 'clean packages':
    cwd         => '/tmp',
    command     => $packager_cleanup,
    path        => ['/bin', '/sbin', '/usr/bin', '/usr/sbin'],
  }
}
