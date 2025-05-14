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

  if ($architecture in ['aarch64', 'ppc64le']) {
    exec { "download_python2.7":
      cwd => "/usr/src",
      command => "/usr/bin/wget https://www.python.org/ftp/python/2.7.18/Python-2.7.18.tgz --no-check-certificate && /usr/bin/mkdir Python-2.7.18 && /bin/tar -xvzf Python-2.7.18.tgz -C Python-2.7.18 --strip-components=1 && cd Python-2.7.18",
      creates => "/usr/src/Python-2.7.18",
    }

    exec { "install_python2.7":
      cwd => "/usr/src/Python-2.7.18",
      command => "/usr/src/Python-2.7.18/configure --prefix=/usr/local/python2.7.18 && /usr/bin/make -j8 && /usr/bin/make install -j8",
      require => [Exec["download_python2.7"]],
      timeout => 3000
    }

    exec { "ln python2.7":
      cwd => "/usr/bin",
      command => "/usr/bin/ln -s /usr/local/python2.7.18/bin/python2.7 python2.7 && /usr/bin/ln -snf python2.7 python2",
      require => Exec["install_python2.7"],
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
