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

class hadoop_hbase {

  class deploy ($roles, $auxiliary = true) {
    if ("hbase-server" in $roles) {
      include hadoop_hbase::server
    }

    if ("hbase-master" in $roles) {
      if ($auxiliary == true) {
        include hadoop_zookeeper::server
      }

      include hadoop::common_hdfs
      include hadoop::init_hdfs
      include hadoop_hbase::master
      Class['Hadoop::Init_hdfs'] -> Class['Hadoop_hbase::Master']
    }

    if ("hbase-client" in $roles) {
      include hadoop_hbase::client
    }
  }

  class client_package  {
    package { "hbase":
      ensure => latest,
    }
  }

  class common_config ($rootdir, $zookeeper_quorum, $kerberos_realm = "", $heap_size="1024") {
    include hadoop_hbase::client_package
    if ($kerberos_realm and $kerberos_realm != "") {
      require kerberos::client
      kerberos::host_keytab { "hbase": 
        spnego => true,
        require => Package["hbase"],
      }

      file { "/etc/hbase/conf/jaas.conf":
        content => template("hadoop_hbase/jaas.conf"),
        require => Package["hbase"],
      }
    }

    file { "/etc/hbase/conf/hbase-site.xml":
      content => template("hadoop_hbase/hbase-site.xml"),
      require => Package["hbase"],
    }
    file { "/etc/hbase/conf/hbase-env.sh":
      content => template("hadoop_hbase/hbase-env.sh"),
      require => Package["hbase"],
    }
  }

  class client($thrift = false) {
    include hadoop_hbase::common_config

    if ($thrift) {
      package { "hbase-thrift":
        ensure => latest,
      }

      service { "hbase-thrift":
        ensure => running,
        require => Package["hbase-thrift"],
        subscribe => File["/etc/hbase/conf/hbase-site.xml", "/etc/hbase/conf/hbase-env.sh"],
        hasrestart => true,
        hasstatus => true,
      }
      Kerberos::Host_keytab <| title == "hbase" |> -> Service["hbase-thrift"]
    }
  }

  class server {
    include hadoop_hbase::common_config

    package { "hbase-regionserver":
      ensure => latest,
    }

    service { "hbase-regionserver":
      ensure => running,
      require => Package["hbase-regionserver"],
      subscribe => File["/etc/hbase/conf/hbase-site.xml", "/etc/hbase/conf/hbase-env.sh"],
      hasrestart => true,
      hasstatus => true,
    } 
    Kerberos::Host_keytab <| title == "hbase" |> -> Service["hbase-regionserver"]
  }

  class master {
    include hadoop_hbase::common_config

    package { "hbase-master":
      ensure => latest,
    }

    service { "hbase-master":
      ensure => running,
      require => Package["hbase-master"],
      subscribe => File["/etc/hbase/conf/hbase-site.xml", "/etc/hbase/conf/hbase-env.sh"],
      hasrestart => true,
      hasstatus => true,
    } 
    Kerberos::Host_keytab <| title == "hbase" |> -> Service["hbase-master"]
  }
}
