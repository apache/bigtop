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

class bigtop_toolchain::python {
  case $operatingsystem{
    /(?i:(centos|fedora|redhat|rocky|sles|opensuse|openeuler))/: {
      package { 'python3-devel' :
        ensure => present
      }
    }
    /(Ubuntu|Debian)/: {
      package { 'dh-python' :
        ensure => present
      }
      package { 'python3-dev' :
        ensure => present
      }
      package { 'python3-full' :
        ensure => present
      }
    }
  }

  package { 'python3-setuptools' :
    ensure => present
  }
  package { 'python3-wheel' :
    ensure => present
  }
  package { 'python3-flake8' :
    ensure => present
  }

  if ($architecture in ['aarch64']) {
    case $operatingsystem{
      /(?i:(fedora|ubuntu|debian))/: {
        package { 'python2' :
          ensure => present
        }
      }
      /(?i:(centos|redhat|rocky))/: {
        if (versioncmp($operatingsystemmajrelease, '8') == 0) {
          package { 'python2' :
            ensure => present
          }
        }
      }
    }
  }


  # The rpm-build package had installed brp-python-bytecompile
  # just under /usr/lib/rpm until Fedora 34,
  # but it seems to have been removed in Fedora 35.
  # So we manually create a symlink instead.
  if ($operatingsystem == 'Fedora' and versioncmp($operatingsystemmajrelease, '35') >= 0) {
    file { '/usr/lib/rpm/brp-python-bytecompile':
      ensure => 'link',
      target => '/usr/lib/rpm/redhat/brp-python-bytecompile',
    }
  }
}
