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

class ignite_hadoop {
  class deploy ($roles) {
    if ("ignite-server" in $roles) {
      ignite_hadoop::server { "ignite-hadoop-node": }
    }
  }

  define server() {
    $hadoop_head_node = lookup("bigtop::hadoop_head_node")
    $hadoop_namenode_port = lookup("hadoop::common_hdfs::hadoop_namenode_port", { 'default_value' => "8020" })

    package { "ignite-hadoop":
      ensure => latest,
    }

    package { "ignite-hadoop-service":
      ensure => latest,
    }

    file { "/etc/default/ignite-hadoop":
      content => template("ignite_hadoop/ignite-hadoop"),
      require => Package["ignite-hadoop"],
    }

    file { "/etc/hadoop/ignite.client.conf":
      ensure  => directory,
      owner   => 'root',
      group   => 'root',
      mode    => '0755',
      require => Package["ignite-hadoop-service"],
    }
    file { "/etc/ignite-hadoop/conf/default-config.xml":
        content => template('ignite_hadoop/default-config.xml'),
        require => [Package["ignite-hadoop"]],
    }
    file { "/etc/hadoop/ignite.client.conf/core-site.xml":
        content => template('ignite_hadoop/core-site.xml'),
        require => [File["/etc/hadoop/ignite.client.conf"]],
    }
    file {
      "/etc/hadoop/ignite.client.conf/mapred-site.xml":
        content => template('ignite_hadoop/mapred-site.xml'),
        require => [File["/etc/hadoop/ignite.client.conf"]],
    }
    file {
      "/etc/hadoop/ignite.client.conf/hive-site.xml":
        content => template('ignite_hadoop/hive-site.xml'),
        require => [File["/etc/hadoop/ignite.client.conf"]],
    }
## let's make sure that ignite-hadoop libs are linked properly
    file {'/usr/lib/hadoop/lib/ignite-core.jar':
      ensure  => link,
      target  => '/usr/lib/ignite-hadoop/libs/ignite-core.jar',
      require => [Package["ignite-hadoop-service"]],
    }
    file {'/usr/lib/hadoop/lib/ignite-hadoop.jar':
      ensure  => link,
      target  => '/usr/lib/ignite-hadoop/libs/ignite-hadoop/ignite-hadoop.jar',
      require => [Package["ignite-hadoop-service"]],
    }

    service { "ignite-hadoop-service":
      ensure  => running,
      require => [ Package["ignite-hadoop", "ignite-hadoop-service"], File["/etc/default/ignite-hadoop"] ],
      subscribe => [ Package["ignite-hadoop"], File["/etc/default/ignite-hadoop", "/etc/ignite-hadoop/conf/default-config.xml"] ]
    }
  }
}
