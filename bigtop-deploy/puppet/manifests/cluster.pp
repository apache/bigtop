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

$roles_map = {
  hdfs-non-ha => {
    master => ["namenode"],
    worker => ["datanode"],
    standby => ["secondarynamenode"],
  },
  hdfs-ha => {
    master => ["namenode"],
    worker => ["datanode"],
    standby => ["standby-namenode"],
  },
  yarn => {
    master => ["resourcemanager"],
    worker => ["nodemanager"],
    client => ["hadoop-client"],
    # mapred is the default app which runs on yarn.
    library => ["mapred-app"],
  },
  mapred => {
    library => ["mapred-app"],
  },
  hbase => {
    master => ["hbase-master"],
    worker => ["hbase-server"],
    client => ["hbase-client"],
  },
  ignite_hadoop => {
    worker => ["ignite-server"],
  },
  solrcloud => {
    worker => ["solr-server"],
  },
  spark => {
    master => ["spark-master"],
    worker => ["spark-worker"],
  },
  tachyon => {
    master => ["tachyon-master"],
    worker => ["tachyon-worker"],
  },
  flume => {
    worker => ["flume-agent"],
  },
  kerberos => {
    master => ["kerberos-server"],
  },
  oozie => {
    master => ["oozie-server"],
    client => ["oozie-client"],
  },
  hcat => {
    master => ["hcatalog-server"],
    gateway_server => ["webhcat-server"],
  },
  sqoop => {
    gateway_server => ["sqoop-server"],
    client => ["sqoop-client"],
  },
  httpfs => {
    gateway_server => ["httpfs-server"],
  },
  hue => {
    gateway_server => ["hue-server"],
  },
  mahout => {
    client => ["mahout-client"],
  },
  giraph => {
    client => ["giraph-client"],
  },
  crunch => {
    client => ["crunch-client"],
  },
  pig => {
    client => ["pig-client"],
  },
  hive => {
    master => ["hive-server2", "hive-metastore"],
    client => ["hive-client"],
  },
  tez => {
    client => ["tez-client"],
  },
  zookeeper => {
    worker => ["zookeeper-server"],
    client => ["zookeeper-client"],
  },
  ycsb => {
    client => ["ycsb-client"],
  },
  zeppelin => {
    master => ["zeppelin-server"],
  },
}

class hadoop_cluster_node (
  $hadoop_security_authentication = hiera("hadoop::hadoop_security_authentication", "simple"),
  $bigtop_real_users = [ 'jenkins', 'testuser', 'hudson' ],
  $cluster_components = ["all"]
  ) {

  user { $bigtop_real_users:
    ensure     => present,
    system     => false,
    managehome => true,
  }

  if ($hadoop_security_authentication == "kerberos") {
    kerberos::host_keytab { $bigtop_real_users: }
    User<||> -> Kerberos::Host_keytab<||>
    include kerberos::client
  }

  $hadoop_head_node = hiera("bigtop::hadoop_head_node")
  $standby_head_node = hiera("bigtop::standby_head_node", "")
  $hadoop_gateway_node = hiera("bigtop::hadoop_gateway_node", $hadoop_head_node)

  $ha_enabled = $standby_head_node ? {
    ""      => false,
    default => true,
  }

  # look into alternate hiera datasources configured using this path in
  # hiera.yaml
  $hadoop_hiera_ha_path = $ha_enabled ? {
    false => "noha",
    true  => "ha",
  }
}

class node_with_roles ($roles = hiera("bigtop::roles")) inherits hadoop_cluster_node {
  define deploy_module($roles) {
    class { "${name}::deploy":
    roles => $roles,
    }
  }

  $modules = [
    "crunch",
    "giraph",
    "hadoop",
    "hadoop_hbase",
    "ignite_hadoop",
    "hadoop_flume",
    "hadoop_hive",
    "hadoop_oozie",
    "hadoop_pig",
    "sqoop2",
    "hadoop_zookeeper",
    "hcatalog",
    "hue",
    "mahout",
    "solr",
    "spark",
    "tachyon",
    "tez",
    "ycsb",
    "kerberos",
    "zeppelin"
  ]

  deploy_module { $modules:
    roles => $roles,
  }
}

class node_with_components inherits hadoop_cluster_node {

  # Ensure (even if a single value) that the type is an array.
  if (is_array($cluster_components)) {
    $components_array = $cluster_components
  } else {
    if ($cluster_components == undef) {
      $components_array = ["all"]
    } else {
      $components_array = [$cluster_components]
    }
  }

  $given_components = $components_array[0] ? {
    "all"   => delete(keys($roles_map), ["hdfs-non-ha", "hdfs-ha"]),
    default => $components_array,
  }
  $ha_dependent_components = $ha_enabled ? {
    true    => ["hdfs-ha"],
    default => ["hdfs-non-ha"],
  }
  $components = concat($given_components, $ha_dependent_components)

  $master_role_types = ["master", "worker", "library"]
  $standby_role_types = ["standby", "library"]
  $worker_role_types = ["worker", "library"]
  $gateway_role_types = ["client", "gateway_server"]

  if ($::fqdn == $hadoop_head_node or $::fqdn == $hadoop_gateway_node) {
    if ($hadoop_gateway_node == $hadoop_head_node) {
      $role_types = concat($master_role_types, $gateway_role_types)
    } elsif ($::fqdn == $hadoop_head_node) {
      $role_types = $master_role_types
    } else {
      $role_types = $gateway_role_types
    }
  } elsif ($::fqdn == $standby_head_node) {
    $role_types = $standby_role_types
  } else {
    $role_types = $worker_role_types
  }

  $roles = get_roles($components, $role_types, $roles_map)

  class { 'node_with_roles':
    roles => $roles,
  }
}
