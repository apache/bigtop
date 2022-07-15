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

class hadoop_hive {

  class deploy ($roles) {
    if ("hive-client" in $roles) {
      include hadoop_hive::client
    }

    if ("hive-metastore" in $roles) {
      include hadoop_hive::metastore
    }

    if ("hive-server2" in $roles) {
      include hadoop_hive::server2

      # include hadoop::init_hdfs
      # Class['Hadoop::Init_hdfs'] -> Class['Hadoop_hive::Server2']
      # if ("mapred-app" in $roles) {
      #  Class['Hadoop::Mapred_app'] -> Class['Hadoop_hive::Server2']
      # }
    }

    if ('hive-hbase' in $roles) {
      include hadoop_hive::hbase
    }
  }

  class client_package {
    package { "hive":
      ensure => latest,
    } 
  }

  class common_config ($hive_tez_container_size = undef,
                       $hive_tez_cpu_vcores = undef,
                       $hbase_master = "",
                       $hbase_zookeeper_quorum = "",
                       $hive_zookeeper_quorum = "",
                       $hive_support_concurrency = false,
                       $kerberos_realm = "",
                       $metastore_uris = "",
                       $metastore_schema_verification = true,
                       $server2_thrift_port = "10000",
                       $server2_thrift_http_port = "10001",
                       $hive_execution_engine = "mr") {
    include hadoop_hive::client_package
    if ($kerberos_realm and $kerberos_realm != "") {
      require kerberos::client
      kerberos::host_keytab { "hive": 
        spnego => true,
        require => Package["hive"],
      }
    }

    file { "/etc/hive/conf/hive-site.xml":
      content => template('hadoop_hive/hive-site.xml'),
      require => Package["hive"],
    }
  }

  class client($hbase_master = "",
      $hbase_zookeeper_quorum = "",
      $hive_execution_engine = "mr") {

      include hadoop_hive::common_config
  }

  class server2 {
    include hadoop_hive::common_config

    package { "hive-server2":
      ensure => latest,
    }

    service { "hive-server2":
      ensure => running,
      require => Package["hive-server2"],
      subscribe => File["/etc/hive/conf/hive-site.xml"],
      hasrestart => true,
      hasstatus => true,
    } 
    Kerberos::Host_keytab <| title == "hive" |> -> Service["hive-server2"]
    Service <| title == "hive-metastore" |> -> Service["hive-server2"]
  }

  class metastore {
    include hadoop_hive::common_config

    package { "hive-metastore":
      ensure => latest,
    }

    service { "hive-metastore":
      ensure => running,
      require => Package["hive-metastore"],
      subscribe => File["/etc/hive/conf/hive-site.xml"],
      hasrestart => true,
      hasstatus => true,
    }
    Kerberos::Host_keytab <| title == "hive" |> -> Service["hive-metastore"]
    File <| title == "/etc/hadoop/conf/core-site.xml" |> -> Service["hive-metastore"]
  }

  class hbase {
    package { 'hive-hbase':
      ensure => latest,
    }
  }
}
