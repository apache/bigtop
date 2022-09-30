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

# The following is a bit of a tricky map. The idea here is that the keys
# correspond to anything that could be specified in
#   hadoop_cluster_node::cluster_components:
# The values are maps from each role that a node can have in a cluster:
#   client, gateway_server, library, master, worker, standby
# to a role recognized by each puppet module's deploy class.
#
# Note that the code here will pass all these roles to all the deploy
# classes defined in every single Bigtop's puppet module. This is similar
# to how a visitor pattern works in OOP. One subtle ramification of this
# approach is that you should make sure that deploy classes from different
# modules do NOT accept same strings for role types.
#
# And if that wasn't enough of a head scratcher -- you also need to keep
# in mind that there's no hdfs key in the following map, even though it
# is a perfectly legal value for hadoop_cluster_node::cluster_components:
# The reason for this is that hdfs is treated as an alias for either
# hdfs-non-ha or hdfs-ha depending on whether HA for HDFS is either enabled
# or disabled.

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
  mapreduce => {
    library => ["mapred-app"],
  },
  kms => {
    master => ["kms"],
  },
  hbase => {
    master => ["hbase-master"],
    worker => ["hbase-server"],
    client => ["hbase-client"],
  },
  solrcloud => {
    worker => ["solr-server"],
  },
  spark => {
    worker => ["spark-on-yarn"],
    client => ["spark-client"],
    library => ["spark-yarn-slave"],
    gateway_server => ["spark-thriftserver"],
  },
  spark-standalone => {
    master => ["spark-master"],
    worker => ["spark-worker"],
  },
  alluxio => {
    master => ["alluxio-master"],
    worker => ["alluxio-worker"],
  },
  flink => {
    master => ["flink-jobmanager"],
    worker => ["flink-taskmanager"],
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
  httpfs => {
    gateway_server => ["httpfs-server"],
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
  gpdb => {
    master => ["gpdb-master"],
    worker => ["gpdb-segment"],
  },
  kafka => {
    worker => ["kafka-server"],
  },
  ambari => {
    master => ["ambari-server"],
    worker => ["ambari-agent"],
  },
  bigtop-utils => {
    client => ["bigtop-utils"],
  },
  livy => {
    master => ["livy-server"],
  },
  phoenix => {
    library => ["phoenix-server"],
  },
  knox => {
    master => ["knox-gateway"],
  }
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
    "alluxio",
    "flink",
    "hadoop",
    "hadoop_hbase",
    "hadoop_hive",
    "hadoop_oozie",
    "hadoop_zookeeper",
    "hcatalog",
    "livy",
    "solr",
    "spark",
    "tez",
    "ycsb",
    "kerberos",
    "zeppelin",
    "kafka",
    "gpdb",
    "ambari",
    "bigtop_utils",
    "phoenix",
    "knox",
  ]

  node_with_roles::deploy_module { $modules:
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
    "all"   => delete(keys($roles_map), ["hdfs-non-ha", "hdfs-ha"]) << "hdfs",
    default => $components_array,
  }
  $ha_dependent_components = $ha_enabled ? {
    true    => "hdfs-ha",
    default => "hdfs-non-ha",
  }
  $components = member($given_components, "hdfs") ? {
    true    => delete($given_components, "hdfs") << $ha_dependent_components,
    default => $given_components
  }

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

  notice("Roles to deploy: ${roles}")
}
