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

class spark {

  class deploy ($roles) {
    if ('spark-client' in $roles) {
      include spark::client
    }

    if ('spark-on-yarn' in $roles) {
      include spark::yarn
    }

    if ('spark-yarn-slave' in $roles) {
      include spark::yarn_slave
    }

    if ('spark-master' in $roles) {
      include spark::master
    }

    if ('spark-worker' in $roles) {
      include spark::worker
    }

    if ('spark-history-server' in $roles) {
      include spark::history_server
    }
  }

  class client {
    include spark::common

    package { 'spark-python':
      ensure  => latest,
      require => Package['spark-core'],
    }

    package { 'spark-external':
      ensure  => latest,
      require => Package['spark-core'],
    }
  }

  class master {
    include spark::common

    package { "spark-master":
      ensure => latest,
    }

    service { 'spark-master':
      ensure     => running,
      subscribe  => [
        Package['spark-master'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus  => true,
    }
  }

  class worker {
    include spark::common

    package { "spark-worker":
      ensure => latest,
    }

    service { 'spark-worker':
      ensure     => running,
      subscribe  => [
        Package['spark-worker'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus  => true,
    }
  }

  class history_server {
    include spark::common

    package { 'spark-history-server':
      ensure => latest,
    }

    service { 'spark-history-server':
      ensure     => running,
      subscribe  => [
        Package['spark-history-server'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus => true,
    }
  }

  class yarn {
    include spark::common
    include spark::datanucleus
  }

  class yarn_slave {
    include spark::yarn_shuffle
    include spark::datanucleus
  }

  class yarn_shuffle {
    package { 'spark-yarn-shuffle':
      ensure => latest,
    }
  }

  class datanucleus {
    package { 'spark-datanucleus':
      ensure => latest,
    }
  }

  class common(
      $master_url = 'yarn',
      $master_host = $fqdn,
      $zookeeper_connection_string = undef,
      $master_port = 7077,
      $worker_port = 7078,
      $master_ui_port = 8080,
      $worker_ui_port = 8081,
      $history_ui_port = 18080,
      $use_yarn_shuffle_service = false,
      $event_log_dir =  "hdfs:///var/log/spark/apps",
      $history_log_dir = "hdfs:///var/log/spark/apps",
  ) {

    package { 'spark-core':
      ensure => latest,
    }
### This is an ungodly hack to deal with the consequence of adding
### unconditional hive-support into Spark
### The addition is tracked by BIGTOP-2154
### The real fix will come in BIGTOP-2268
    package { 'spark-datanucleus':
      ensure => latest,
    }

    if $zookeeper_connection_string == undef {
      $spark_daemon_java_opts = "\"-Dspark.deploy.recoveryMode=NONE\""
    } else {
      $spark_daemon_java_opts = "\"-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=${zookeeper_connection_string}\""
    }

    file { '/etc/spark/conf/spark-env.sh':
      content => template('spark/spark-env.sh'),
      require => Package['spark-core'],
    }

    file { '/etc/spark/conf/spark-defaults.conf':
      content => template('spark/spark-defaults.conf'),
      require => Package['spark-core'],
    }

    file { '/etc/spark/conf/log4j.properties':
      source  => '/etc/spark/conf/log4j.properties.template',
      require => Package['spark-core'],
    }
  }
}
