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

  class server($executor="SequentialExecutor",
    $load_examples=true,
    $sql_alchemy_conn="sqlite:////var/lib/airflow/airflow.db",
    $install_bigpetstore_example=false,
  ) {
    package { 'airflow':
      ensure => latest,
    }

    file { '/var/lib/airflow/airflow.cfg':
      content => template('airflow/airflow.cfg'),
      owner   => 'airflow',
      group   => 'airflow',
      require => Package['airflow'],
    }

    # The built-in scanf function (https://www.puppet.com/docs/puppet/7/function.html#scanf) doesn't work
    # on Debian and Ubuntu for some reason, so we define a function for parsing SQLAlchemy's database URL
    # (https://docs.sqlalchemy.org/en/20/core/engines.html#database-urls) ourselves here.
    $parsed_url = parse_database_url($sql_alchemy_conn)
    $dialect = $parsed_url[0]
    $username = $parsed_url[1]
    $password = $parsed_url[2]
    $database = $parsed_url[3]

    if $dialect =~ /^postgres/ {
      # Install Airflow's Postgres Provider
      exec { 'install-postgres-provider':
        command     => ['/usr/lib/airflow/bin/python3', '-m', 'pip', 'install', 'apache-airflow-providers-postgres'],
        environment => ['AIRFLOW_HOME=/var/lib/airflow'],
        require     => File['/var/lib/airflow/airflow.cfg'],
        user        => 'root',
      }

      # Install and enable PostgreSQL
      if $operatingsystem =~ /^(?i:(ubuntu|debian))$/ {
        package { 'postgresql':
          ensure => latest,
        }
        service { 'postgresql':
          ensure  => running,
          require => Package['postgresql'],
        }
      } else {
        package { 'postgresql-server':
          ensure => latest
        }
        exec { 'initdb':
          command => '/usr/bin/pg_ctl initdb -D /var/lib/pgsql/data',
          user    => 'postgres',
          require => Package['postgresql-server']
        }
        service { 'postgresql':
          ensure  => running,
          require => Exec['initdb']
        }
      }

      # Set up Airflow's database backend in accordance with
      # https://airflow.apache.org/docs/apache-airflow/2.10.5/howto/set-up-database.html#setting-up-a-postgresql-database
      exec { 'create-airflow-database':
        command => ["/usr/bin/psql", "-c", "CREATE DATABASE ${database}"],
        user    => 'postgres',
        require => Service['postgresql']
      }
      exec { 'create-airflow-user':
        command => ["/usr/bin/psql", "-c", "CREATE USER ${username} WITH PASSWORD '${password}'"],
        user    => 'postgres',
        require => Exec['create-airflow-database']
      }
      exec { 'grant-airflow-privileges':
        command => ["/usr/bin/psql", "-c", "GRANT ALL PRIVILEGES ON DATABASE ${database} TO ${username}"],
        user    => 'postgres',
        require => Exec['create-airflow-user'],
      }
      exec { 'alter-database-owner':
        command => ["/usr/bin/psql", "-c", "ALTER DATABASE ${database} OWNER TO ${username}"],
        user    => 'postgres',
        require => Exec['grant-airflow-privileges'],
      }
      exec { 'airflow-db-init':
        command     => '/usr/lib/airflow/bin/airflow db init',
        environment => ['AIRFLOW_HOME=/var/lib/airflow'],
        user        => 'airflow',
        require     => Exec['alter-database-owner'],
      }
    } else {
      exec { 'airflow-db-init':
        command     => '/usr/lib/airflow/bin/airflow db init',
        environment => ['AIRFLOW_HOME=/var/lib/airflow'],
        user        => 'airflow',
        require     => File['/var/lib/airflow/airflow.cfg'],
      }
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

    if $install_bigpetstore_example {
      exec { 'install-spark-provider':
        command     => "/usr/lib/airflow/bin/python3 -m pip install apache-airflow-providers-apache-spark 'pyspark<4'",
        environment => ['AIRFLOW_HOME=/var/lib/airflow'],
        user        => 'root',
        require     => Package['airflow'],
      }

      file { '/var/lib/airflow/dags':
        ensure  => 'directory',
        owner   => 'airflow',
        group   => 'airflow',
        require => Package['airflow'],
      }

      file { '/var/lib/airflow/dags/example_bigpetstore.py':
        content => template('airflow/example_bigpetstore.py'),
        owner   => 'airflow',
        group   => 'airflow',
        require => File['/var/lib/airflow/dags'],
      }
    }
  }
}
