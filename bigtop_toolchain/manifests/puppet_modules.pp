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

class bigtop_toolchain::puppet_modules {

  exec { 'install-puppet-stdlib':
    path    => '/usr/bin:/bin',
    command => 'puppet module install puppetlabs-stdlib --version 4.12.0',
    creates => '/etc/puppet/modules/stdlib',
  }

  case $operatingsystem{
    /Ubuntu|Debian/: {
      if versioncmp($::puppetversion, '4') < 0 {
        $version = '--version 2.4.0'
      } else {
        $version = ''
      }
      exec { 'install-puppet-apt':
        path    => '/usr/bin:/bin',
        command => "puppet module install puppetlabs-apt ${version}",
        creates => '/etc/puppet/modules/apt',
      }
    }
  }

  stage { 'first':
    before => Stage['main'],
  }
  class { 'bigtop_toolchain::puppet_modules_prereq': stage => 'first' }
}
