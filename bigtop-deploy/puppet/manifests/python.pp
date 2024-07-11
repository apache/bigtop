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

class python {
    case $operatingsystem {
        /(?i:(centos|fedora|redhat|rocky))/: {
            require yumrepo
            package { 'python3-devel':
              ensure => 'present',
            }
            if ($operatingsystem != 'Fedora') {
                case $operatingsystemmajrelease {
                    '9': {
                        package { 'python-unversioned-command':
                          ensure => 'present',
                        }
                    }
                    '8': {
                        exec { 'set-python3':
                          command => '/usr/sbin/update-alternatives --set python /usr/bin/python3',
                          unless  => "/usr/sbin/update-alternatives --display python | grep  'link currently points to /usr/bin/python3'",
                          path    => ['/bin', '/usr/bin', '/sbin', '/usr/sbin'],
                        }
                    }
                }
            }else {
                package { 'python-unversioned-command':
                  ensure => 'present',
                }
            }
        }

        /(Ubuntu|Debian)/: {
            package { 'python3-dev':
              ensure => 'present',
            }
            package { 'python-is-python3':
              ensure => 'present',
            }
        }

        /openEuler/: {
            package { 'python3-devel':
              ensure => 'present',
            }
            package { 'python3-unversioned-command':
              ensure => 'present',
            }
        }
    }
}

