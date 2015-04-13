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

class hadoop-hive {

  class deploy ($roles) {

    if ('hive-client' in $roles) {
      include hadoop-hive::client
    }
    if ('hive-metastore-server' in $roles) {
      include hadoop-hive::metastore-server
    }
    if ('hive-server' in $roles) {
      include hadoop-hive::server
      if ('hive-metastore-server' in $roles) {
        Class['Hadoop-hive::Metastore-server'] -> Class['Hadoop-hive::Server']
      }
    }

    # Need to make sure local mysql server is setup correctly (in case hive is
    # using it) before initializing the schema
    if ('hive-client' or 'hive-metastore-server' or 'hive-server' in $roles) {
      if ('mysql-server' in $roles) {
        Class['Bigtop-mysql::Server'] -> Exec<| title == 'init hive-metastore schema' |>
      }
    }
  }

  class common (
    $metastore_server_uris = [],
    $metastore_database_type = 'derby',
    $metastore_database_host = $fqdn,
    $metastore_database_port = '3306',
    $metastore_database_name = 'hive',
    $metastore_database_user = 'hive',
    $metastore_database_password = 'hive',
    $hbase_master = undef,
    $hbase_zookeeper_quorum = undef,
    $hdfs_uri = undef,
    $hive_site_overrides = {},
    $hive_execution_engine = "mr"
    ) {

    $metastore_database_configs = {
      derby => {
        url          => "jdbc:derby:;databaseName=/var/lib/hive/metastore/metastore_db;create=true",
        driver_class => 'org.apache.derby.jdbc.EmbeddedDriver',
      },
      mysql => {
        url          => "jdbc:mysql://${metastore_database_host}:${metastore_database_port}/${metastore_database_name}?createDatabaseIfNotExist=true",
        driver_class => 'com.mysql.jdbc.Driver',
      }
    }
    $supported_database_types = keys($metastore_database_configs)

    if (! ($metastore_database_type in $supported_database_types)) {
      fail("hadoop-hive::metastore_database_type: $database_type is not supported. Supported database types are ", $supported_database_types)
    }

    $metastore_database_url = $metastore_database_configs[$metastore_database_type][url]
    $metastore_database_driver_class = $metastore_database_configs[$metastore_database_type][driver_class]

    package { 'hive':
      ensure => latest,
    }

    file { "/etc/hive/conf/hive-site.xml":
      content => template('hadoop-hive/hive-site.xml'),
      require => Package["hive"],
    }

    include init-metastore-schema
  }

  class client {

    include common
  }

  class server {

    include common

    package { 'hive-server2':
      ensure => latest,
    }

    service { 'hive-server2':
      ensure    => running,
      hasstatus => true,
      subscribe => File['/etc/hive/conf/hive-site.xml'],
      require   => [Package['hive'], Package['hive-server2'], Class['Hadoop-hive::Init-metastore-schema']],
    }
  }

  class metastore-server {

    include common

    package { 'hive-metastore':
      ensure => latest,
    }

    service { 'hive-metastore':
      ensure    => running,
      hasstatus => true,
      subscribe => File['/etc/hive/conf/hive-site.xml'],
      require   => [Package['hive'], Package['hive-metastore'], Class['Hadoop-hive::Init-metastore-schema']],
    }
  }

  class database-connector {

    include common

    case $common::metastore_database_type {
      'mysql': {
        contain mysql-connector
      }
      'derby': {
        # do nothing
      }
      default: {
        fail("$common::metastore_database_type is not supported. Supported database types are ", $common::supported_database_types)
      }
    }
  }

  class mysql-connector {

    include common

    package { 'mysql-connector-java':
      ensure => latest
    }

    file { '/usr/lib/hive/lib/mysql-connector-java.jar':
      ensure  => 'link',
      target  => '/usr/share/java/mysql-connector-java.jar',
      require => [Package['mysql-connector-java'], Package['hive']]
    }
  }

  class init-metastore-schema {

    include common
    include database-connector

    exec { 'init hive-metastore schema':
      command   => "/usr/lib/hive/bin/schematool -dbType $common::metastore_database_type -initSchema -verbose",
      require   => [Package['hive'], Class['Hadoop-hive::Database-connector'], File['/etc/hive/conf/hive-site.xml']],
      subscribe => File['/etc/hive/conf/hive-site.xml'],
      logoutput => true,
      unless    => "/usr/lib/hive/bin/schematool -dbType $common::metastore_database_type -info"
    }
  }
}
