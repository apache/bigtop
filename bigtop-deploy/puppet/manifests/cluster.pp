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
  $hadoop_ha_zookeeper_quorum        = "${hadoop_head_node}:2181"
  # $hadoop_mapred_jobtracker_plugins="org.apache.hadoop.thriftfs.ThriftJobTrackerPlugin"
  # $hadoop_mapred_tasktracker_plugins="org.apache.hadoop.mapred.TaskTrackerCmonInst"

  $bigtop_real_users = [ 'jenkins', 'testuser', 'hudson' ]

  $hadoop_core_proxyusers = { oozie => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "${hadoop_head_node},localhost,127.0.0.1" },
                                hue => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "${hadoop_head_node},localhost,127.0.0.1" },
                             httpfs => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,httpfs,hue,users', hosts => "${hadoop_head_node},localhost,127.0.0.1" } }

  $hbase_relative_rootdir        = extlookup("hadoop_hbase_rootdir", "/hbase")
  $hadoop_hbase_rootdir = "$hadoop_namenode_uri$hbase_relative_rootdir"
  $hadoop_hbase_zookeeper_quorum = $hadoop_head_node
  $hbase_heap_size               = extlookup("hbase_heap_size", "1024")

  $giraph_zookeeper_quorum       = $hadoop_head_node

  $hadoop_zookeeper_ensemble = ["$hadoop_head_node:2888:3888"]

  $hadoop_oozie_url  = "http://${hadoop_head_node}:11000/oozie"
  $hadoop_httpfs_url = "http://${hadoop_head_node}:14000/webhdfs/v1"
  $hadoop_rm_url             = "http://${hadoop_head_node}:8088"
  $hadoop_rm_proxy_url       = "http://${hadoop_head_node}:8088"
  $hadoop_history_server_url = "http://${hadoop_head_node}:19888"

  $solrcloud_collections = ["collection1"]
  $solrcloud_port        = "1978"
  $solrcloud_port_admin  = "1979"
  $solrcloud_zk          = "${hadoop_head_node}:2181"

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
  hadoop-flume::agent { "flume agent":
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

  hadoop::nodemanager { "nodemanager":
        rm_host => $hadoop_rm_host,
        rm_port => $hadoop_rm_port,
        rt_port => $hadoop_rt_port,
        dirs => $yarn_data_dirs,
        auth => $hadoop_security_authentication,
  }

  hadoop-hbase::server { "hbase region server":
        rootdir => $hadoop_hbase_rootdir,
        heap_size => $hbase_heap_size,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        kerberos_realm => $kerberos_realm, 
  }

  hadoop::mapred-app { "mapred-app":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        jobtracker_host => $hadoop_jobtracker_host,
        jobtracker_port => $hadoop_jobtracker_port,
        auth => $hadoop_security_authentication,
        dirs => $mapred_data_dirs,
  }

  solr::server { "solrcloud server":
       collections => $solrcloud_collections,
       port        => $solrcloud_port,
       port_admin  => $solrcloud_port_admin,
       zk          => $solrcloud_zk,
  }
}

class hadoop_head_node inherits hadoop_worker_node {

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

  hadoop::httpfs { "httpfs":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        auth => $hadoop_security_authentication,
  }

  hadoop-hbase::master { "hbase master":
        rootdir => $hadoop_hbase_rootdir,
        heap_size => $hbase_heap_size,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        kerberos_realm => $kerberos_realm, 
  }

  hadoop-oozie::server { "oozie server":
        kerberos_realm => $kerberos_realm, 
  }

  hadoop-sqoop::server { "sqoop server":
  }
  hadoop-sqoop::client { "sqoop client":
  }

  hcatalog::server { "hcatalog server":
        kerberos_realm => $kerberos_realm,
  }
  hcatalog::webhcat::server { "webhcat server":
        kerberos_realm => $kerberos_realm,
  }

  hue::server { "hue server":
        rm_url      => $hadoop_rm_url,
        rm_proxy_url => $hadoop_rm_proxy_url,
        history_server_url => $hadoop_history_server_url,
        webhdfs_url => $hadoop_httpfs_url,
        rm_host     => $hadoop_rm_host,
        rm_port     => $hadoop_rm_port,
        oozie_url   => $hadoop_oozie_url,
        default_fs  => $hadoop_namenode_uri,
        kerberos_realm => $kerberos_realm,
  }
  Hadoop::Httpfs<||> -> Hue::Server<||>
  Hadoop-sqoop::Client<||> -> Hue::Server<||>

  hadoop-zookeeper::server { "zookeeper":
        myid => "0",
        ensemble => $hadoop_zookeeper_ensemble,
        kerberos_realm => $kerberos_realm, 
  }

  exec { "init hdfs":
        path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
        command => 'bash -x /usr/lib/hadoop/libexec/init-hdfs.sh',
        require => Package['hadoop-hdfs']
  }

  Exec<| title == "init hdfs" |> -> Hadoop-hbase::Master<||>
  Exec<| title == "init hdfs" |> -> Hadoop::Resourcemanager<||>
  Exec<| title == "init hdfs" |> -> Hadoop::Historyserver<||>
  Exec<| title == "init hdfs" |> -> Hadoop::Httpfs<||>
  Exec<| title == "init hdfs" |> -> Hadoop::Rsync_hdfs<||>
  Exec<| title == "init hdfs" |> -> Hadoop-oozie::Server<||>
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
  hadoop::client { "hadoop client":
    namenode_host => $hadoop_namenode_host,
    namenode_port => $hadoop_namenode_port,
    jobtracker_host => $hadoop_jobtracker_host,
    jobtracker_port => $hadoop_jobtracker_port,
    # auth => $hadoop_security_authentication,
  }
  mahout::client { "mahout client":
  }
  giraph::client { "giraph client":
     zookeeper_quorum => $giraph_zookeeper_quorum,
  }
  crunch::client { "crunch client":
  }
  hadoop-pig::client { "pig client":
  }
  hadoop-hive::client { "hive client":
     hbase_zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
  }
  hadoop-sqoop::client { "sqoop client":
  }
  hadoop-oozie::client { "oozie client":
  }
  hadoop-hbase::client { "hbase client":
  }
  hadoop-zookeeper::client { "zookeeper client":
  }
}
