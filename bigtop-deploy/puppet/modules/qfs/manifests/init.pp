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

class qfs {
  class deploy($roles) {
    if ("qfs-metaserver" in $roles) {
      include qfs::metaserver
    }

    if ("qfs-chunkserver" in $roles) {
      include qfs::chunkserver
    }

    if ("qfs-client" in $roles) {
      include qfs::client
    }
  }

  class common($metaserver_host, $metaserver_port, $chunkserver_port,
      $metaserver_client_port, $chunkserver_client_port) {
    $cluster_key = sha1($metaserver_host)
    $storage_dirs = suffix($hadoop::hadoop_storage_dirs, "/qfs")

    hadoop::create_storage_dir { $qfs::common::storage_dirs: } ->
    file { $qfs::common::storage_dirs:
      ensure => directory,
      owner => root,
      group => root,
      mode => '0755',
    }

    file { "/etc/qfs":
      ensure => directory,
      mode => '0755',
    }
  }

  class metaserver {
    include qfs::common

    package { "qfs-metaserver":
      ensure => latest,
    }

    $metaserver_conf = "/etc/qfs/MetaServer.prp"
    file { $metaserver_conf:
      content => template("qfs/MetaServer.prp"),
      require => Package["qfs-metaserver"],
    }

    file { [
      "${qfs::common::storage_dirs[0]}/metaserver",
      "${qfs::common::storage_dirs[0]}/metaserver/transaction_logs",
      "${qfs::common::storage_dirs[0]}/metaserver/checkpoint",
    ]:
      ensure => directory,
      owner => qfs,
      group => qfs,
      mode => '0755',
      before => Service['qfs-metaserver'],
      require => [
        File[$qfs::common::storage_dirs[0]],
        Package['qfs-metaserver']
      ],
    }

    exec { "mkfs":
      command => "/usr/bin/metaserver -c $metaserver_conf",
      creates => "${qfs::common::storage_dirs[0]}/metaserver/checkpoint/latest",
      # BIGTOP-3126: qfs init script requires to run under a permitted directory
      cwd => "/tmp",
      user => qfs,
      group => qfs,
      require => [
        Package["qfs-metaserver"],
        File[$metaserver_conf],
        File["${qfs::common::storage_dirs[0]}/metaserver/checkpoint"],
      ],
    }

    if ($fqdn == $qfs::common::metaserver_host) {
      service { "qfs-metaserver":
        ensure => running,
        require => [
          Package["qfs-metaserver"],
          File[$metaserver_conf],
          Exec["mkfs"],
        ],
        hasrestart => true,
        hasstatus => true,
      }
    }
  }

  class chunkserver {
    include qfs::common

    package { "qfs-chunkserver":
      ensure => latest,
    }

    $chunkserver_conf = "/etc/qfs/ChunkServer.prp"
    file { $chunkserver_conf:
      content => template("qfs/ChunkServer.prp"),
      require => [Package["qfs-chunkserver"], File["/etc/qfs"]],
    }

    $cs_dirs = suffix($hadoop::hadoop_storage_dirs, "/qfs/chunkserver")
    $cs_chunks_dirs = suffix($hadoop::hadoop_storage_dirs, "/qfs/chunkserver/chunks")
    $storage_dirs = concat($cs_dirs, $cs_chunks_dirs)

    file { $storage_dirs:
      ensure => directory,
      owner => qfs,
      group => qfs,
      mode => '0755',
      before => Service['qfs-chunkserver'],
      require => [
        File[$qfs::common::storage_dirs],
        Package['qfs-chunkserver']
      ],
    }

    service { "qfs-chunkserver":
      ensure => running,
      require => [
        Package["qfs-chunkserver"],
        File[$chunkserver_conf]
      ],
      hasrestart => true,
      hasstatus => true,
    }
  }

  class client {
    include qfs::common

    package { [
      "qfs-client",
      "qfs-hadoop",
      "qfs-java",
    ]:
      ensure => latest,
    }

    file { "/etc/qfs/QfsClient.prp":
      content => template("qfs/QfsClient.prp"),
      require => [Package["qfs-client"], File["/etc/qfs"]],
    }

    file { "/usr/bin/hadoop-qfs":
      content => template("qfs/hadoop-qfs"),
      mode => '0755',
    }

    # Add QFS native lib into Hadoop native lib dir
    exec { "add_qfs_native_lib":
      path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
      command => 'find /usr/lib/qfs/ -name "lib*" -exec ln -s {} /usr/lib/hadoop/lib/native \;',
      require => [ Package["hadoop-yarn-nodemanager"], Package["qfs-client"] ],
      notify  => [ Service["hadoop-yarn-nodemanager"] ],
    }
  }
}
