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

class livy {

  class deploy ($roles) {
    if ('livy-server' in $roles) {
      include livy::server
    }
  }

  class server (
    $server_port = 8998,
    $master_url  = 'yarn',
  ) {
    package { 'livy':
      ensure => latest,
    }

    file { '/etc/livy/conf/livy-env.sh':
      content   => template('livy/livy-env.sh'),
      require   => Package['livy'],
    }

    file { '/etc/livy/conf/livy.conf':
      content => template('livy/livy.conf'),
      require => Package['livy'],
    }

    file { '/etc/livy/conf/log4j.properties':
        content => template('livy/log4j.properties'),
        require => Package['livy'],
    }

    service { 'livy-server':
      ensure     => running,
      require    => [
        Package['spark-core'],
        Package['livy'],
      ],
      hasrestart => true,
      hasstatus  => true,
      subscribe  => [
        File['/etc/livy/conf/livy-env.sh'],
        File['/etc/livy/conf/livy.conf']
      ]
    }
  }
}
