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
    case $operatingsystem {
      /(?i:(ubuntu|debian))/: {
            $postgres_packages = ['postgresql']
            $python = 'python3'
            $java_home_env = 'JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64'
        }
        default: {
            $postgres_packages = ['postgresql-jdbc', 'postgresql-server']
            $python = 'python36'
            $java_home_env = 'JAVA_HOME=/usr/lib/jvm/java-1.8.0'
        }
    }

    package { ['ranger-admin', $python]:
          ensure => latest,
    }

    if ($operatingsystem =~ /^(?i:(ubuntu|debian))$/) {
      notice("Matched ubuntu or debian")
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
      notice("Did not match ubuntu or debian")
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
      command => "/bin/sudo -u postgres /usr/bin/psql -c \"ALTER USER postgres WITH PASSWORD 'admin';\"",
      require => Service['postgresql'],
    }

    notice("Before defining file resource")
    file { '/usr/lib/ranger-admin/install.properties':
      content => template('ranger/ranger-admin/install.properties'),
      require => [Package['ranger-admin'], Exec['change_postgres_password']]
    }

    exec { '/usr/lib/ranger-admin/setup.sh':
      cwd         => '/usr/lib/ranger-admin',
      environment => $java_home_env,
      require     => File['/usr/lib/ranger-admin/install.properties'],
    }

    exec { '/usr/lib/ranger-admin/set_globals.sh':
      cwd         => '/usr/lib/ranger-admin',
      environment => $java_home_env,
      require     => Exec['/usr/lib/ranger-admin/setup.sh'],
    }

    exec { 'systemctl daemon-reload':
      path    => ["/bin", "/usr/bin"],
      require => Exec['/usr/lib/ranger-admin/set_globals.sh'],
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
