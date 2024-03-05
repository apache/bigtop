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

class ranger {

  class deploy($roles) {
    if ('ranger-server' in $roles) {
      include ranger::admin
    }
  }


  class admin($admin_password) {
    # Before Facter 3.14.17, Rocky Linux 8 is detected as 'RedHat'.
    # https://puppet.com/docs/pe/2019.8/osp/release_notes_facter.html#enhancements-3-14-17
    if ( $operatingsystem =~ /^(?i:(redhat|rocky))$/ and 0 <= versioncmp($operatingsystemmajrelease, '8')) {
      $python = 'python36'
    } else {
      $python = 'python3'
    }

    package { ['ranger-admin', $python]:
          ensure => latest,
    }

    if ($operatingsystem =~ /^(?i:(ubuntu|debian))$/) {
      service { 'postgresql':
        ensure  => running,
        require => Package[$python, 'ranger-admin'],
      }

      file { '/usr/share/java/postgresql-jdbc.jar':
        ensure => link,
        target => '/usr/share/java/postgresql.jar',
        require => Package[$python, 'ranger-admin'],
      }
    } else {
      exec { 'initdb':
        command => '/usr/bin/pg_ctl initdb -D /var/lib/pgsql/data',
        user    => 'postgres',
        require => Package[$python, 'ranger-admin'],
      }

      service { 'postgresql':
        ensure  => running,
        require => Exec['initdb'],
      }
    }

    exec { 'change_postgres_password':
      path    => ['/usr/bin', '/bin','/sbin'],
      command => "sudo -u postgres psql -c \"ALTER USER postgres WITH PASSWORD 'admin';\"",
      require => Service['postgresql'],
    }

    file { '/usr/lib/ranger-admin/install.properties':
      content => template('ranger/ranger-admin/install.properties'),
      require => [Package['ranger-admin'], Exec['change_postgres_password']]
    }

    exec { 'setup.sh':
      command => '/bin/bash -c "source /usr/lib/bigtop-utils/bigtop-detect-javahome && /usr/lib/ranger-admin/setup.sh"',
      cwd     => '/usr/lib/ranger-admin',
      require => File['/usr/lib/ranger-admin/install.properties'],
    }

    exec { 'set_globals.sh':
      command => '/bin/bash -c "source /usr/lib/bigtop-utils/bigtop-detect-javahome && /usr/lib/ranger-admin/set_globals.sh"',
      cwd     => '/usr/lib/ranger-admin',
      require => Exec['setup.sh'],
    }

    exec { 'systemctl daemon-reload':
      path    => ["/bin", "/usr/bin"],
      command => "systemctl daemon-reload",
      require => Exec['set_globals.sh'],
    }
    
    if ($operatingsystem == 'openEuler') {
      exec { 'ranger-admin':
        command => "/usr/sbin/usermod -G root ranger && /sbin/service ranger-admin start",
        require => Exec['systemctl daemon-reload'],
      }
    } else {
      service { 'ranger-admin':
        ensure  => running,
        require => Exec['systemctl daemon-reload'],
      }
    }
  }
}