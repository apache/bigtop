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

class ycsb {

  class deploy ($roles) {
    if ("ycsb-client" in $roles) {
      include ycsb::client
    }
  }

  class client {
    # install python2 in openEuler
    if ($operatingsystem == 'openEuler') {
       package { ['gcc-c++', 'make', 'cmake']:
         ensure => latest,
         before => Exec['download_python2.7'],
       }
       exec { "download_python2.7":
         cwd     => "/usr/src",
         command => "/usr/bin/wget https://www.python.org/ftp/python/2.7.14/Python-2.7.14.tgz --no-check-certificate && /usr/bin/mkdir Python-2.7.14 && /bin/tar -xvzf Python-2.7.14.tgz -C Python-2.7.14 --strip-components=1 && cd Python-2.7.14",
         creates => "/usr/src/Python-2.7.14",
       }

       exec { "install_python2.7":
         cwd     => "/usr/src/Python-2.7.14",
         command => "/usr/src/Python-2.7.14/configure --prefix=/usr/local/python2.7.14 --enable-optimizations && /usr/bin/make -j8 && /usr/bin/make install -j8",
         require => [Exec["download_python2.7"]],
         timeout => 3000,
       }

       exec { "ln python2.7":
         cwd     => "/usr/bin",
         command => "/usr/bin/ln -s /usr/local/python2.7.14/bin/python2.7 python2.7 && /usr/bin/ln -snf python2.7 python2 && /usr/bin/ln -snf python2 python",
         require => Exec["install_python2.7"],
       }
    }

    package { ["ycsb"]:
      ensure => latest,
    }
  }
}
