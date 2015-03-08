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

class hadoop_cluster_node (
  $hadoop_security_authentication = hiera("hadoop::hadoop_security_authentication", "simple"),

  # Lookup component array or comma separated components (i.e.
  # hadoop,spark,hbase ) as a default via facter.
  $cluster_components = "$::components"
  ) {
  # Ensure (even if a single value) that the type is an array.
  if is_array($cluster_components) {
    $components = $cluster_components
  } else {
    $components = any2array($cluster_components, ",")
  }

  $all = ($components[0] == undef)

  if ($hadoop_security_authentication == "kerberos") {
    include kerberos::client
  }

  # Flume agent is the only component that goes on EVERY node in the cluster
  if ($all or "flume" in $components) {
    include hadoop-flume::agent
  }
}



class hadoop_worker_node (
  $bigtop_real_users = [ 'jenkins', 'testuser', 'hudson' ]
  ) inherits hadoop_cluster_node {
  user { $bigtop_real_users:
    ensure     => present,
    system     => false,
    managehome => true,
  }

  if ($hadoop_security_authentication == "kerberos") {
    kerberos::host_keytab { $bigtop_real_users: }
    User<||> -> Kerberos::Host_keytab<||>
  }

  include hadoop::datanode
  if ($all or "yarn" in $components) {
    include hadoop::nodemanager
  }
  if ($all or "hbase" in $components) {
    include hadoop-hbase::server
  }

  if ($all or "gridgain-hadoop" in $components) {
    gridgain-hadoop::server { "gridgain-hadoop-node": }
  }

  ### If mapred is not installed, yarn can fail.
  ### So, when we install yarn, we also need mapred for now.
  ### This dependency should be cleaned up eventually.
  if ($all or "mapred-app" or "yarn" in $components) {
    include hadoop::mapred-app
  }

  ## Tolerate solr or solrcloud as the component name, since build
  ## bom uses solr but solrcloud has been used for some time.
  if ($all or "solr" in $components or "solrcloud" in $components) {
    include solr::server
  }

  if ($all or "spark" in $components) {
    include spark::worker
  }

  if ($all or "tachyon" in $components) {
    include tachyon::worker
  }

}

class hadoop_head_node inherits hadoop_worker_node {
  exec { "init hdfs":
    path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
    command => 'bash -x /usr/lib/hadoop/libexec/init-hdfs.sh',
    require => Package['hadoop-hdfs'],
    timeout => 0
  }
  Class['Hadoop::Namenode'] -> Class['Hadoop::Datanode'] -> Exec<| title == "init hdfs" |>

if ($hadoop_security_authentication == "kerberos") {
    include kerberos::server
    include kerberos::kdc
    include kerberos::kdc::admin_server
  }

  include hadoop::namenode

  if ($hadoop::common_hdfs::ha == "disabled") {
    include hadoop::secondarynamenode
  }

  if ($all or "yarn" in $components) {
    include hadoop::resourcemanager
    include hadoop::historyserver
    include hadoop::proxyserver
    Exec<| title == "init hdfs" |> -> Class['Hadoop::Resourcemanager'] -> Class['Hadoop::Nodemanager']
    Exec<| title == "init hdfs" |> -> Class['Hadoop::Historyserver']
  }

  if ($all or "hbase" in $components) {
    include hadoop-hbase::master
    Exec<| title == "init hdfs" |> -> Class['Hadoop-hbase::Master']
  }

  if ($all or "oozie" in $components) {
    include hadoop-oozie::server
    if ($all or "mapred-app" in $components) {
      Class['Hadoop::Mapred-app'] -> Class['Hadoop-oozie::Server']
    }
    Exec<| title == "init hdfs" |> -> Class['Hadoop-oozie::Server']
  }

  if ($all or "hcat" in $components) {
    include hcatalog::server
    include hcatalog::webhcat::server
  }

  if ($all or "spark" in $components) {
    include spark::master
  }

  if ($all or "tachyon" in $components) {
   include tachyon::master
  }

  if ($all or "hbase" in $components) {
    include hadoop-zookeeper::server
  }

  # class hadoop::rsync_hdfs isn't used anywhere
  #Exec<| title == "init hdfs" |> -> Class['Hadoop::Rsync_hdfs']
}

class standby_head_node inherits hadoop_cluster_node {
  include hadoop::namenode
}

class hadoop_gateway_node inherits hadoop_cluster_node {
  if ($all or "sqoop" in $components) {
    include hadoop-sqoop::server
  }

  if ($all or "httpfs" in $components) {
    include hadoop::httpfs
    if ($all or "hue" in $components) {
      Class['Hadoop::Httpfs'] -> Class['Hue::Server']
    }
  }

  if ($all or "hue" in $components) {
    include hue::server
    if ($all or "hbase" in $components) {
      Class['Hadoop-hbase::Client'] -> Class['Hue::Server']
    }
  }

  include hadoop::client

  if ($all or "mahout" in $components) {
    include mahout::client
  }
  if ($all or "giraph" in $components) {
    include giraph::client
  }
  if ($all or "crunch" in $components) {
    include crunch::client
  }
  if ($all or "pig" in $components) {
    include hadoop-pig::client
  }
  if ($all or "hive" in $components) {
    include hadoop-hive::client
  }
  if ($all or "sqoop" in $components) {
    include hadoop-sqoop::client
  }
  if ($all or "oozie" in $components) {
    include hadoop-oozie::client
  }
  if ($all or "hbase" in $components) {
    include hadoop-hbase::client
  }
  if ($all or "zookeeper" in $components) {
    include hadoop-zookeeper::client
  }
}
