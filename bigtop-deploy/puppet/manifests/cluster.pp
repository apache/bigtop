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

class hadoop_cluster_node {
  require bigtop_util  

  $hadoop_head_node        = extlookup("hadoop_head_node") 
  $standby_head_node = extlookup("standby_head_node", "")
  $hadoop_gateway_node     = extlookup("hadoop_gateway_node", $hadoop_head_node)

  $hadoop_ha = $standby_head_node ? {
    ""      => disabled,
    default => extlookup("hadoop_ha", "manual"),
  }


  $hadoop_namenode_host        = $hadoop_ha ? {
    "disabled" => $hadoop_head_node,
    default    => [ $hadoop_head_node, $standby_head_node ],
  }
  $hadoop_namenode_port        = extlookup("hadoop_namenode_port", "17020")
  $hadoop_namenode_thrift_port = extlookup("hadoop_namenode_thrift_port", "10090")
  $hadoop_dfs_namenode_plugins = extlookup("hadoop_dfs_namenode_plugins", "")
  $hadoop_dfs_datanode_plugins = extlookup("hadoop_dfs_datanode_plugins", "")
  # $hadoop_dfs_namenode_plugins="org.apache.hadoop.thriftfs.NamenodePlugin"
  # $hadoop_dfs_datanode_plugins="org.apache.hadoop.thriftfs.DatanodePlugin"
  $hadoop_ha_nameservice_id    = extlookup("hadoop_ha_nameservice_id", "ha-nn-uri")
  $hadoop_namenode_uri   = $hadoop_ha ? {
    "disabled" => "hdfs://$hadoop_namenode_host:$hadoop_namenode_port",
    default    => "hdfs://${hadoop_ha_nameservice_id}:8020",
  }

  $hadoop_rm_host        = $hadoop_head_node
  $hadoop_rt_port        = extlookup("hadoop_rt_port", "8025")
  $hadoop_rm_port        = extlookup("hadoop_rm_port", "8032")
  $hadoop_sc_port        = extlookup("hadoop_sc_port", "8030")
  $hadoop_rt_thrift_port = extlookup("hadoop_rt_thrift_port", "9290")

  $hadoop_hs_host        = $hadoop_head_node
  $hadoop_hs_port        = extlookup("hadoop_hs_port", "10020")
  $hadoop_hs_webapp_port = extlookup("hadoop_hs_webapp_port", "19888")

  $hadoop_ps_host        = $hadoop_head_node
  $hadoop_ps_port        = extlookup("hadoop_ps_port", "20888")

  $hadoop_jobtracker_host            = $hadoop_head_node
  $hadoop_jobtracker_port            = extlookup("hadoop_jobtracker_port", "8021")
  $hadoop_jobtracker_thrift_port     = extlookup("hadoop_jobtracker_thrift_port", "9290")
  $hadoop_mapred_jobtracker_plugins  = extlookup("hadoop_mapred_jobtracker_plugins", "")
  $hadoop_mapred_tasktracker_plugins = extlookup("hadoop_mapred_tasktracker_plugins", "")

  $hadoop_zookeeper_port             = extlookup("hadoop_zookeeper_port", "2181")
  $solrcloud_port                    = extlookup("solrcloud_port", "1978")
  $solrcloud_admin_port              = extlookup("solrcloud_admin_port", "1979")
  $hadoop_oozie_port                 = extlookup("hadoop_oozie_port", "11000")
  $hadoop_httpfs_port                = extlookup("hadoop_httpfs_port", "14000")
  $hadoop_rm_http_port               = extlookup("hadoop_rm_http_port", "8088")
  $hadoop_rm_proxy_port              = extlookup("hadoop_rm_proxy_port", "8088")
  $hadoop_history_server_port        = extlookup("hadoop_history_server_port", "19888")
  $hbase_thrift_port                 = extlookup("hbase_thrift_port", "9090")
  $spark_master_port                 = extlookup("spark_master_port", "7077")
  $spark_master_ui_port              = extlookup("spark_master_ui_port", "18080")

  # Lookup comma separated components (i.e. hadoop,spark,hbase ).
  $components_str                    = extlookup("components")
  # Ensure (even if a single value) that the type is an array.
  $components                        = any2array($components_str,",")

  $all = ($components[0] == undef)

  $hadoop_ha_zookeeper_quorum        = "${hadoop_head_node}:${hadoop_zookeeper_port}"
  $solrcloud_zk                      = "${hadoop_head_node}:${hadoop_zookeeper_port}"
  $hbase_thrift_address              = "${hadoop_head_node}:${hbase_thrift_port}"
  $hadoop_oozie_url                  = "http://${hadoop_head_node}:${hadoop_oozie_port}/oozie"
  $hadoop_httpfs_url                 = "http://${hadoop_head_node}:${hadoop_httpfs_port}/webhdfs/v1"
  $sqoop_server_url                  = "http://${hadoop_head_node}:${sqoop_server_port}/sqoop"
  $solrcloud_url                     = "http://${hadoop_head_node}:${solrcloud_port}/solr/"
  $hadoop_rm_url                     = "http://${hadoop_head_node}:${hadoop_rm_http_port}"
  $hadoop_rm_proxy_url               = "http://${hadoop_head_node}:${hadoop_rm_proxy_port}"
  $hadoop_history_server_url         = "http://${hadoop_head_node}:${hadoop_history_server_port}"

  $bigtop_real_users = [ 'jenkins', 'testuser', 'hudson' ]

  $hadoop_core_proxyusers = { oozie => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "*" },
                                hue => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "*" },
                             httpfs => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "*" } }

  $hbase_relative_rootdir        = extlookup("hadoop_hbase_rootdir", "/hbase")
  $hadoop_hbase_rootdir = "$hadoop_namenode_uri$hbase_relative_rootdir"
  $hadoop_hbase_zookeeper_quorum = $hadoop_head_node
  $hbase_heap_size               = extlookup("hbase_heap_size", "1024")
  $hbase_thrift_server           = $hadoop_head_node

  $giraph_zookeeper_quorum       = $hadoop_head_node

  $spark_master_host             = $hadoop_head_node
  $tachyon_master_host            = $hadoop_head_node

  $hadoop_zookeeper_ensemble = ["$hadoop_head_node:2888:3888"]

  # Set from facter if available
  $roots              = extlookup("hadoop_storage_dirs",       split($hadoop_storage_dirs, ";"))
  $namenode_data_dirs = extlookup("hadoop_namenode_data_dirs", append_each("/namenode", $roots))
  $hdfs_data_dirs     = extlookup("hadoop_hdfs_data_dirs",     append_each("/hdfs",     $roots))
  $mapred_data_dirs   = extlookup("hadoop_mapred_data_dirs",   append_each("/mapred",   $roots))
  $yarn_data_dirs     = extlookup("hadoop_yarn_data_dirs",     append_each("/yarn",     $roots))

  $hadoop_security_authentication = extlookup("hadoop_security", "simple")
  if ($hadoop_security_authentication == "kerberos") {
    $kerberos_domain     = extlookup("hadoop_kerberos_domain")
    $kerberos_realm      = extlookup("hadoop_kerberos_realm")
    $kerberos_kdc_server = extlookup("hadoop_kerberos_kdc_server")

    include kerberos::client
  }

  # Flume agent is the only component that goes on EVERY node in the cluster
  if ($all or "flume" in $components) {
    hadoop-flume::agent { "flume agent":
    }
  }
}



class hadoop_worker_node inherits hadoop_cluster_node {
  user { $bigtop_real_users:
    ensure     => present,
    system     => false,
    managehome => true,
    groups     => 'wheel',
  }

  if ($hadoop_security_authentication == "kerberos") {
    kerberos::host_keytab { $bigtop_real_users: }
    User<||> -> Kerberos::Host_keytab<||>
  }

  hadoop::datanode { "datanode":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        dirs => $hdfs_data_dirs,
        auth => $hadoop_security_authentication,
        ha   => $hadoop_ha,
  }

  if ($all or "yarn" in $components) {
    hadoop::nodemanager { "nodemanager":
          rm_host => $hadoop_rm_host,
          rm_port => $hadoop_rm_port,
          rt_port => $hadoop_rt_port,
          dirs => $yarn_data_dirs,
          auth => $hadoop_security_authentication,
    }
  }
  if ($all or "hbase" in $components) {
    hadoop-hbase::server { "hbase region server":
          rootdir => $hadoop_hbase_rootdir,
          heap_size => $hbase_heap_size,
          zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
          kerberos_realm => $kerberos_realm,
    }
  }

  ### If mapred is not installed, yarn can fail.
  ### So, when we install yarn, we also need mapred for now.
  ### This dependency should be cleaned up eventually.
  if ($all or "mapred-app" or "yarn" in $components) {
    hadoop::mapred-app { "mapred-app":
          namenode_host => $hadoop_namenode_host,
          namenode_port => $hadoop_namenode_port,
          jobtracker_host => $hadoop_jobtracker_host,
          jobtracker_port => $hadoop_jobtracker_port,
          auth => $hadoop_security_authentication,
          dirs => $mapred_data_dirs,
    }
  }

  if ($all or "solrcloud" in $components) {
    solr::server { "solrcloud server":
         port        => $solrcloud_port,
         port_admin  => $solrcloud_admin_port,
         zk          => $solrcloud_zk,
         root_url    => $hadoop_namenode_uri,
         kerberos_realm => $kerberos_realm,
    }
  }

  if ($all or "spark" in $components) {
    spark::worker { "spark worker":
         master_host    => $spark_master_host,
         master_port    => $spark_master_port,
         master_ui_port => $spark_master_ui_port,
    }
  }

  if ($components[0] == undef or "tachyon" in $components) {
    tachyon::worker { "tachyon worker":
         master_host => $tachyon_master_host
    }
  }

}

class hadoop_head_node inherits hadoop_worker_node {

  exec { "init hdfs":
    path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
    command => 'bash -x /usr/lib/hadoop/libexec/init-hdfs.sh',
    require => Package['hadoop-hdfs']
  }
  Hadoop::Namenode<||> -> Hadoop::Datanode<||> -> Exec<| title == "init hdfs" |>

if ($hadoop_security_authentication == "kerberos") {
    include kerberos::server
    include kerberos::kdc
    include kerberos::kdc::admin_server
  }

  hadoop::namenode { "namenode":
        host => $hadoop_namenode_host,
        port => $hadoop_namenode_port,
        dirs => $namenode_data_dirs,
        # thrift_port => $hadoop_namenode_thrift_port,
        auth => $hadoop_security_authentication,
        ha   => $hadoop_ha,
        zk   => $hadoop_ha_zookeeper_quorum,
  }

  if ($hadoop_ha == "disabled") {
    hadoop::secondarynamenode { "secondary namenode":
          namenode_host => $hadoop_namenode_host,
          namenode_port => $hadoop_namenode_port,
          auth => $hadoop_security_authentication,
    }
  }

  if ($all or "yarn" in $components) {
    hadoop::resourcemanager { "resourcemanager":
          host => $hadoop_rm_host,
          port => $hadoop_rm_port,
          rt_port => $hadoop_rt_port,
          sc_port => $hadoop_sc_port,
          # thrift_port => $hadoop_jobtracker_thrift_port,
          auth => $hadoop_security_authentication,
    }

    hadoop::historyserver { "historyserver":
          host => $hadoop_hs_host,
          port => $hadoop_hs_port,
          webapp_port => $hadoop_hs_webapp_port,
          auth => $hadoop_security_authentication,
    }

    hadoop::proxyserver { "proxyserver":
          host => $hadoop_ps_host,
          port => $hadoop_ps_port,
          auth => $hadoop_security_authentication,
    }
    Exec<| title == "init hdfs" |> -> Hadoop::Resourcemanager<||> -> Hadoop::Nodemanager<||>
    Exec<| title == "init hdfs" |> -> Hadoop::Historyserver<||>
  }

  if ($all or "hbase" in $components) {
    hadoop-hbase::master { "hbase master":
          rootdir => $hadoop_hbase_rootdir,
          heap_size => $hbase_heap_size,
          zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
          kerberos_realm => $kerberos_realm,
    }
    Exec<| title == "init hdfs" |> -> Hadoop-hbase::Master<||>
  }

  if ($all or "oozie" in $components) {
    hadoop-oozie::server { "oozie server":
          kerberos_realm => $kerberos_realm,
    }
    Hadoop::Mapred-app<||> -> Hadoop-oozie::Server<||>
    Exec<| title == "init hdfs" |> -> Hadoop-oozie::Server<||>
  }

  if ($all or "hcat" in $components) {
  hcatalog::server { "hcatalog server":
        kerberos_realm => $kerberos_realm,
  }
  hcatalog::webhcat::server { "webhcat server":
        kerberos_realm => $kerberos_realm,
  }
  }

  if ($all or "spark" in $components) {
  spark::master { "spark master":
       master_host    => $spark_master_host,
       master_port    => $spark_master_port,
       master_ui_port => $spark_master_ui_port,
  }
  }

  if ($all == undef or "tachyon" in $components) {
   tachyon::master { "tachyon-master":
       master_host => $tachyon_master_host
   }
  }

  if ($all or "hbase" in $components) {
    hadoop-zookeeper::server { "zookeeper":
          myid => "0",
          ensemble => $hadoop_zookeeper_ensemble,
          kerberos_realm => $kerberos_realm,
    }
  }

  Exec<| title == "init hdfs" |> -> Hadoop::Rsync_hdfs<||>

}

class standby_head_node inherits hadoop_cluster_node {
  hadoop::namenode { "namenode":
        host => $hadoop_namenode_host,
        port => $hadoop_namenode_port,
        dirs => $namenode_data_dirs,
        # thrift_port => $hadoop_namenode_thrift_port,
        auth => $hadoop_security_authentication,
        ha   => $hadoop_ha,
        zk   => $hadoop_ha_zookeeper_quorum,
  }
}

class hadoop_gateway_node inherits hadoop_cluster_node {
  $hbase_thrift_address              = "${fqdn}:${hbase_thrift_port}"
  $hadoop_httpfs_url                 = "http://${fqdn}:${hadoop_httpfs_port}/webhdfs/v1"
  $sqoop_server_url                  = "http://${fqdn}:${sqoop_server_port}/sqoop"
  $solrcloud_url                     = "http://${fqdn}:${solrcloud_port}/solr/"

  if ($all or "sqoop" in $components) {
    hadoop-sqoop::server { "sqoop server":
    }
  }

  if ($all or "httpfs" in $components) {
    hadoop::httpfs { "httpfs":
          namenode_host => $hadoop_namenode_host,
          namenode_port => $hadoop_namenode_port,
          auth => $hadoop_security_authentication,
    }
    Hadoop::Httpfs<||> -> Hue::Server<||>
  }

  if ($all or "hue" in $components) {
    hue::server { "hue server":
          rm_url      => $hadoop_rm_url,
          rm_proxy_url => $hadoop_rm_proxy_url,
          history_server_url => $hadoop_history_server_url,
          webhdfs_url => $hadoop_httpfs_url,
          sqoop_url   => $sqoop_server_url,
          solr_url    => $solrcloud_url,
          hbase_thrift_url => $hbase_thrift_address,
          rm_host     => $hadoop_rm_host,
          rm_port     => $hadoop_rm_port,
          oozie_url   => $hadoop_oozie_url,
          default_fs  => $hadoop_namenode_uri,
          kerberos_realm => $kerberos_realm,
    }
  }
  Hadoop-hbase::Client<||> -> Hue::Server<||>

  hadoop::client { "hadoop client":
    namenode_host => $hadoop_namenode_host,
    namenode_port => $hadoop_namenode_port,
    jobtracker_host => $hadoop_jobtracker_host,
    jobtracker_port => $hadoop_jobtracker_port,
    # auth => $hadoop_security_authentication,
  }

  if ($all or "mahout" in $components) {
    mahout::client { "mahout client":
    }
  }
  if ($all or "giraph" in $components) {
    giraph::client { "giraph client":
       zookeeper_quorum => $giraph_zookeeper_quorum,
    }
  }
  if ($all or "crunch" in $components) {
    crunch::client { "crunch client":
    }
  }
  if ($all or "pig" in $components) {
    hadoop-pig::client { "pig client":
    }
  }
  if ($all or "hive" in $components) {
    hadoop-hive::client { "hive client":
       hbase_zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
    }
  }
  if ($all or "sqoop" in $components) {
    hadoop-sqoop::client { "sqoop client":
    }
  }
  if ($all or "oozie" in $components) {
    hadoop-oozie::client { "oozie client":
    }
  }
  if ($all or "hbase" in $components) {
    hadoop-hbase::client { "hbase thrift client":
      thrift => true,
      kerberos_realm => $kerberos_realm,
    }
  }
  if ($all or "zookeeper" in $components) {
    hadoop-zookeeper::client { "zookeeper client":
    }
  }
}
