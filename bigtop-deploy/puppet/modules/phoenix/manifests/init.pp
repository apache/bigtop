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

class phoenix {

  class deploy ($roles) {
    if ('phoenix-server' in $roles) {
      include phoenix::server
      if ('hbase-master' in $roles) {
        include phoenix::hbase_master_restart
      }
      if ('hbase-server' in $roles) {
        include phoenix::hbase_server_restart
      }
    }
  }

  class hbase_master_restart {
    if ($operatingsystem == 'openEuler') {
      exec { "hbase_master_restart":
        command => "/sbin/service hbase-master restart",
        path    => ['/usr/bin', '/bin','/sbin'],
        cwd     => '/tmp',
        timeout => 3000,
        require => [
          File['phoenix-server-lib'],
          Service['hbase-master'],
         ],
     }
    } else {
        exec { "hbase_master_restart":
          command => "systemctl restart hbase-master",
          path    => ['/usr/bin', '/bin'],
          cwd     => '/tmp',
          timeout => 3000,
          require => [
            File['phoenix-server-lib'],
            Service['hbase-master'],
          ],
        }
    }
  }

  class hbase_server_restart {
    if ($operatingsystem == 'openEuler') {
      exec { "hbase_server_restart":
        command => "/sbin/service hbase-regionserver restart",
        path    => ['/usr/bin', '/bin','/sbin'],
        cwd     => '/tmp',
        timeout => 3000,
        require => [
          File['phoenix-server-lib'],
          Service['hbase-regionserver'],
         ],
     }
    } else {
        exec { "hbase_server_restart":
          command => "systemctl restart hbase-regionserver",
          path    => ['/usr/bin', '/bin'],
          cwd     => '/tmp',
          timeout => 3000,
          require => [
            File['phoenix-server-lib'],
            Service['hbase-regionserver'],
          ],
        }
    }
  }

  class server {
    package { 'phoenix':
      ensure => latest,
      require => Package["hbase"],
    }
    file { 'phoenix-server-lib': 
      path    => '/usr/lib/hbase/lib/phoenix-server.jar',
      target  => '/usr/lib/phoenix/phoenix-server.jar',
      ensure  => link,
      require => Package['phoenix'],
    }
  }

}
