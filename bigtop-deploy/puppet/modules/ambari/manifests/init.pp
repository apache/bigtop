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

class ambari {

  class deploy ($roles) {
    if ("ambari-server" in $roles) {
      include ambari::server
    }

    if ("ambari-agent" in $roles) {
      include ambari::agent
    }
  }

  class server {
    #install python2 and init postgresql for openEuler
    if $operatingsystem == 'openEuler' {
      exec { "download_python2.7":
        cwd     => "/usr/src",
        command => "/usr/bin/wget https://www.python.org/ftp/python/2.7.14/Python-2.7.14.tgz && /usr/bin/mkdir Python-2.7.14 && /bin/tar -xvzf Python-2.7.14.tgz -C Python-2.7.14 --strip-components=1 && cd Python-2.7.14 && /usr/bin/yum install -y make gcc-c++",
        creates => "/usr/src/Python-2.7.14",
      }
      exec { "install_python2.7":
        cwd     => "/usr/src/Python-2.7.14",
        command => "/usr/src/Python-2.7.14/configure --prefix=/usr/local/python2.7.14 --enable-optimizations && /usr/bin/make -j8 && /usr/bin/make install -j8",
        require => [Exec["download_python2.7"]],
        timeout => 3000
      }
      exec { "ln python2.7":
        cwd     => "/usr/bin",
        command => "/usr/bin/ln -s /usr/local/python2.7.14/bin/python2.7 python2.7 && /usr/bin/ln -snf python2.7 python2",
        require => Exec["install_python2.7"],
      }

      package { ['postgresql-jdbc', 'postgresql-server']:
        ensure => latest,
      }
      exec {'initdb':
        command => '/usr/bin/pg_ctl initdb -D /var/lib/pgsql/data',
        user    => 'postgres',
        require => Package['postgresql-jdbc', 'postgresql-server'],
      }
      service { 'postgresql':
        ensure  => running,
        require => Exec['initdb'],
      }
    }

    package { "ambari-server":
      ensure => latest,
    }

    exec {
        "mpack install":
           command => "/bin/bash -c 'echo yes | /usr/sbin/ambari-server install-mpack --purge --verbose --mpack=/var/lib/ambari-server/resources/odpi-ambari-mpack-2.7.5.0.0.tar.gz'",
           require => [ Package["ambari-server"] ]
    }

    exec {
        "server setup":
           command => "/usr/sbin/ambari-server setup -j $(readlink -f /usr/bin/java | sed -e 's@jre/bin/java@@' -e 's@bin/java@@') -s",
           require => [ Package["ambari-server"], Package["jdk"], Exec["mpack install"] ],
           # The default timeout is 300 seconds, but it's sometimes too short to setup Ambari Server.
           # In most of the successful cases, applying puppet manifest finishes within 600 seconds on CI,
           # so extend the timeout to that value.
           timeout => 600,
    }

    service { "ambari-server":
        ensure => running,
        require => [ Package["ambari-server"], Exec["server setup"] ],
        hasrestart => true,
        hasstatus => true,
    }
  }

  class agent($server_host = "localhost") {
    #install python2 for openEuler
    if $operatingsystem == 'openEuler' {
      exec { "agent_download_python2.7":
        cwd     => "/usr/src",
        command => "/usr/bin/wget  https://www.python.org/ftp/python/2.7.14/Python-2.7.14.tgz --no-check-certificate && /usr/bin/mkdir Python-2.7.14 && /bin/tar -xvzf Python-2.7.14.tgz -C Python-2.7.14 --strip-components=1 && cd Python-2.7.14 && /usr/bin/yum install -y make gcc-c++",
        creates => "/usr/src/Python-2.7.14",
      }
      exec { "agent_install_python2.7":
        cwd     => "/usr/src/Python-2.7.14",
        command => "/usr/src/Python-2.7.14/configure --prefix=/usr/local/python2.7.14 --enable-optimizations && /usr/bin/make -j8 && /usr/bin/make install -j8",
        require => [Exec["agent_download_python2.7"]],
        timeout => 3000
      }
      exec { "agent ln python2.7":
        cwd     => "/usr/bin",
        command => "/usr/bin/ln -s /usr/local/python2.7.14/bin/python2.7 python2.7 && /usr/bin/ln -snf python2.7 python2",
        require => Exec["agent_install_python2.7"],
      }
    }

    package { "ambari-agent":
      ensure => latest,
    }

    file {
      "/etc/ambari-agent/conf/ambari-agent.ini":
        content => template('ambari/ambari-agent.ini'),
        require => [Package["ambari-agent"]],
    }

    service { "ambari-agent":
        ensure => running,
        require => [ Package["ambari-agent"], File["/etc/ambari-agent/conf/ambari-agent.ini"] ],
        hasrestart => true,
        hasstatus => true,
    }
  }
}
