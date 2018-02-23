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

class kafka {

  class deploy ($roles) {
    if ('kafka-server' in $roles) {
      include kafka::server
    }
  }

  class server(
      $broker_id = undef,
      $log_dirs = undef,
      $bind_addr = undef,
      $port = "9092",
      $zookeeper_connection_string = "localhost:2181",
    ) {

    package { 'kafka':
      ensure => latest,
    }

    package { 'kafka-server':
      ensure => latest,
    }

    file { '/etc/kafka/conf/server.properties':
      content => template('kafka/server.properties'),
      require => [ Package['kafka'], Package['kafka-server'] ],
      owner   => 'kafka',
      group   => 'kafka',
    }

    service { 'kafka-server':
      ensure     => running,
      subscribe  => [
          Package['kafka'],
          Package['kafka-server'],
          File['/etc/kafka/conf/server.properties'],
       ],
      hasrestart => true,
      hasstatus  => true,
    }
  }
}
