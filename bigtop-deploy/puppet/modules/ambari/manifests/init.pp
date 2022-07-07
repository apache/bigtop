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

    package { 'bigtop-ambari-mpack':
      ensure => latest,
    }

    package { "ambari-server":
      ensure => latest,
    }

    exec {
        "mpack install":
           command => "/bin/bash -c 'echo yes | /usr/sbin/ambari-server install-mpack --purge --verbose --mpack=/usr/lib/bigtop-ambari-mpack/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz'",
           require => [ Package["ambari-server"], Package['bigtop-ambari-mpack'] ]
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
