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

    define set_python_alternatives($command) {
        exec { $name:
          command => $command,
          unless  => "/usr/sbin/update-alternatives --display python | grep '/usr/bin/python3'",
          path    => ['/bin', '/usr/bin', '/sbin', '/usr/sbin'],
        }
    }

    define change_python_interpreter($file, $interpreter) {
        file_line { "change_interpreter_in_${name}":
          path  => $file,
          line  => "#!${interpreter}",
          match => '^#!.*python.*$',
        }
    }

    case $operatingsystem {
        /(?i:(centos|fedora|redhat|rocky))/: {
            if ($operatingsystem != 'Fedora') {
                case $operatingsystemmajrelease {
                    '9': {
                        package { 'python3-unversioned-command':
                          ensure => 'present',
                        }
                    }
                    '8': {
                        set_python_alternatives { 'set-python3':
                          command => '/usr/sbin/update-alternatives --set python /usr/bin/python3',
                        }
                    }
                    '7': {
                        change_python_interpreter { 'yum':
                          file        => '/usr/bin/yum',
                          interpreter => '/usr/bin/python2.7',
                        }
                        change_python_interpreter { 'urlgrabber-ext-down':
                          file        => '/usr/libexec/urlgrabber-ext-down',
                          interpreter => '/usr/bin/python2.7',
                        }

                        set_python_alternatives { 'install-python3':
                          command => '/usr/sbin/update-alternatives --install /usr/bin/python python /usr/bin/python3 1',
                        }
                    }
                }
            }else {
                set_python_alternatives { 'install-python3':
                  command => '/usr/sbin/update-alternatives --install /usr/bin/python python /usr/bin/python3 1',
                }
            }
        }

        /(Ubuntu|Debian)/: {
            set_python_alternatives { 'install-python3-debian':
              command => '/usr/sbin/update-alternatives --install /usr/bin/python python /usr/bin/python3 1',
            }
        }

        /openEuler/: {
            package { 'python3-unversioned-command':
              ensure => 'present',
            }
        }
    }
}

