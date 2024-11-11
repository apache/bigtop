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

class hadoop_zookeeper (
  $kerberos_realm = "",
) {

  $package_suffix = hiera("bigtop::package_suffix", "")
  $parent_dir = hiera("bigtop::parent_dir", "")
  $bigtop_base_version = hiera("bigtop::base_version", "")

  if $package_suffix and $package_suffix != '' and $parent_dir and $parent_dir != '' {
    $bin_home = "${parent_dir}/${bigtop_base_version}/usr/bin"
  } else {
    $bin_home = '/usr/bin'
  }

  $zookeeper_package = "zookeeper${package_suffix}"
  $zookeeper_server_package = "zookeeper${package_suffix}-server"
  $zookeeper_service_name = "zookeeper${package_suffix}-server"

  class deploy ($roles) {
    if ("zookeeper-client" in $roles) {
      include hadoop_zookeeper::client
    }

    if ("zookeeper-server" in $roles) {
      include hadoop_zookeeper::server
    }
  }

  class common (
    $kerberos_realm = $hadoop_zookeeper::kerberos_realm,
  ) inherits hadoop_zookeeper {
    if ($kerberos_realm and $kerberos_realm != "") {
      file { '/etc/zookeeper/conf/java.env':
        source => 'puppet:///modules/hadoop_zookeeper/java.env',
      }
      Package<| title == 'zookeeper' |> -> File['/etc/zookeeper/conf/java.env']
      Package<| title == 'zookeeper-server' |> -> File['/etc/zookeeper/conf/java.env']
      File['/etc/zookeeper/conf/java.env'] ~> Service<| title == 'zookeeper-server' |>
    }
  }

  class client (
    $kerberos_realm = $hadoop_zookeeper::kerberos_realm,
  ) inherits hadoop_zookeeper {
    include hadoop_zookeeper::common



    package { $zookeeper_package:
      ensure => latest,
      require => Package["jdk"],
    }

    if ($kerberos_realm and $kerberos_realm != "") {
      file { '/etc/zookeeper/conf/client-jaas.conf':
        content => template('hadoop_zookeeper/client-jaas.conf'),
        require => Package[$zookeeper_package],
      }
    }
  }

  class server($myid,
                $port = "2181",
                $datadir = "/var/lib/zookeeper",
                $ensemble = [$myid, "localhost:2888:3888"],
                $kerberos_realm = $hadoop_zookeeper::kerberos_realm,
                $client_bind_addr = "",
                $autopurge_purge_interval = "24",
                $autopurge_snap_retain_count = "3",
  ) inherits hadoop_zookeeper {
    include hadoop_zookeeper::common


    package { $zookeeper_server_package:
      ensure => latest,
      require => Package["jdk"],
    }

    file { $datadir:
      ensure => directory,
      owner  => 'zookeeper',
      group  => 'zookeeper',
      mode   => '0755',
      require => Package[$zookeeper_server_package],
    }

    service { $zookeeper_service_name:
      ensure => running,
      require => [ Package[$zookeeper_server_package],
                   Exec["zookeeper-server-initialize"] ],
      subscribe => [ File["/etc/zookeeper/conf/zoo.cfg"],
                     File["${datadir}/myid"] ],
      hasrestart => true,
      hasstatus => true,
      provider => systemd,
    }

    file { "/etc/zookeeper/conf/zoo.cfg":
      content => template("hadoop_zookeeper/zoo.cfg"),
      require => Package[$zookeeper_server_package],
    }

    file { "${datadir}/myid":
      content => inline_template("<%= @myid %>"),
      require => [Package[$zookeeper_server_package], File[$datadir]],
    }

    exec { "zookeeper-server-initialize":
      command => "$bin_home/zookeeper-server-initialize",
      user    => "zookeeper",
      creates => "${datadir}/version-2",
      require => [Package[$zookeeper_server_package], File[$datadir]],
    }

    if ($kerberos_realm and $kerberos_realm != "") {
      require kerberos::client

      kerberos::host_keytab { "zookeeper":
        spnego  => true,
        require => Package[$zookeeper_server_package],
        before  => Service[$zookeeper_service_name],
      }

      file { "/etc/zookeeper/conf/server-jaas.conf":
        content => template("hadoop_zookeeper/server-jaas.conf"),
        require => Package[$zookeeper_server_package],
        notify  => Service[$zookeeper_service_name],
      }
    }
  }
}
