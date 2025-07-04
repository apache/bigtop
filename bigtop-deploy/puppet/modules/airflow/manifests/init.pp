# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class airflow {
  class deploy ($roles) {
    if ('airflow' in $roles) {
      include airflow::server
    }
  }

  class server($executor, $load_examples, $sql_alchemy_conn) {
    package { 'airflow':
      ensure => latest,
    }

    file { '/var/lib/airflow/airflow.cfg':
      content => template('airflow/airflow.cfg'),
      owner   => 'airflow',
      group   => 'airflow',
      require => Package['airflow'],
    }

    exec { 'airflow-db-init':
      command     => '/usr/lib/airflow/bin/airflow db init',
      environment => ['AIRFLOW_HOME=/var/lib/airflow'],
      user        => 'airflow',
      require     => File['/var/lib/airflow/airflow.cfg'],
    }

    exec { 'airflow-users-create':
      command     => '/usr/lib/airflow/bin/airflow users create -e admin@example.org -f John -l Doe -p admin -r Admin -u admin',
      environment => ['AIRFLOW_HOME=/var/lib/airflow'],
      user        => 'airflow',
      require     => Exec['airflow-db-init'],
    }

    service { 'airflow-scheduler':
      ensure  => running,
      require => Exec['airflow-db-init'],
    }

    service { 'airflow-webserver':
      ensure  => running,
      require => Exec['airflow-db-init'],
    }
  }
}
