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

  $hadoop_namenode_host        = $hadoop_head_node
  $hadoop_namenode_port        = extlookup("hadoop_namenode_port", "17020")
  $hadoop_namenode_thrift_port = extlookup("hadoop_namenode_thrift_port", "10090")
  $hadoop_dfs_namenode_plugins = extlookup("hadoop_dfs_namenode_plugins", "")
  $hadoop_dfs_datanode_plugins = extlookup("hadoop_dfs_datanode_plugins", "")
  # $hadoop_dfs_namenode_plugins="org.apache.hadoop.thriftfs.NamenodePlugin"
  # $hadoop_dfs_datanode_plugins="org.apache.hadoop.thriftfs.DatanodePlugin"

  $hadoop_jobtracker_host            = $hadoop_head_node
  $hadoop_jobtracker_port            = extlookup("hadoop_jobtracker_port", "8021")
  $hadoop_jobtracker_thrift_port     = extlookup("hadoop_jobtracker_thrift_port", "9290")
  $hadoop_mapred_jobtracker_plugins  = extlookup("hadoop_mapred_jobtracker_plugins", "")
  $hadoop_mapred_tasktracker_plugins = extlookup("hadoop_mapred_tasktracker_plugins", "")
  # $hadoop_mapred_jobtracker_plugins="org.apache.hadoop.thriftfs.ThriftJobTrackerPlugin"
  # $hadoop_mapred_tasktracker_plugins="org.apache.hadoop.mapred.TaskTrackerCmonInst"

  $hadoop_core_proxyusers = { oozie => { groups => 'root,hadoop,jenkins,oozie,users', hosts => "${hadoop_head_node},localhost,127.0.0.1" } }

  $hbase_relative_rootdir        = extlookup("hadoop_hbase_rootdir", "/hbase")
  $hadoop_hbase_rootdir = "hdfs://$hadoop_namenode_host:$hadoop_namenode_port/$hbase_relative_rootdir"
  $hadoop_hbase_zookeeper_quorum = $hadoop_head_node

  $hadoop_zookeeper_ensemble = ["$hadoop_head_node:2888:3888"]

  # Set from facter if available
  $roots              = extlookup("hadoop_storage_dirs",       split($hadoop_storage_dirs, ";"))
  $namenode_data_dirs = extlookup("hadoop_namenode_data_dirs", append_each("/namenode", $roots))
  $hdfs_data_dirs     = extlookup("hadoop_hdfs_data_dirs",     append_each("/hdfs",     $roots))
  $mapred_data_dirs   = extlookup("hadoop_mapred_data_dirs",   append_each("/mapred",   $roots))

  $hadoop_security_authentication = extlookup("hadoop_security", "simple")
  if ($hadoop_security_authentication == "kerberos") {
    $kerberos_domain     = extlookup("hadoop_kerberos_domain")
    $kerberos_realm      = extlookup("hadoop_kerberos_realm")
    $kerberos_kdc_server = extlookup("hadoop_kerberos_kdc_server")

    include kerberos::client
    kerberos::client::host_keytab { ["hdfs", "mapred", "hbase", "oozie"]:
      princs_map => { hdfs   => [ "host", "hdfs" ],
                      mapred => [ "mapred" ],
                      hbase  => [ "hbase"  ],
                      oozie  => [ "oozie"  ], },
    }
  }
}

class hadoop_worker_node inherits hadoop_cluster_node {
  hadoop::datanode { "datanode":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        dirs => $hdfs_data_dirs,
        auth => $hadoop_security_authentication,
  }

  hadoop::tasktracker { "tasktracker":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        jobtracker_host => $hadoop_jobtracker_host,
        jobtracker_port => $hadoop_jobtracker_port,
        dirs => $mapred_data_dirs,
        auth => $hadoop_security_authentication,
  }

  hadoop-hbase::server { "hbase region server":
        rootdir => $hadoop_hbase_rootdir,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        kerberos_realm => $kerberos_realm, 
  }
}

class hadoop_head_node inherits hadoop_cluster_node {

  if ($hadoop_security_authentication == "kerberos") {
    include kerberos::kdc, kerberos::kdc::admin_server
  }

  hadoop::namenode { "namenode":
        port => $hadoop_namenode_port,
        jobtracker_host => $hadoop_jobtracker_host,
        jobtracker_port => $hadoop_jobtracker_port,
        dirs => $namenode_data_dirs,
        # thrift_port => $hadoop_namenode_thrift_port,
        auth => $hadoop_security_authentication,
  }

  hadoop::secondarynamenode { "secondary namenode":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        auth => $hadoop_security_authentication,
  }

  hadoop::jobtracker { "jobtracker":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        host => $hadoop_jobtracker_host,
        port => $hadoop_jobtracker_port,
        dirs => $mapred_data_dirs,
        # thrift_port => $hadoop_jobtracker_thrift_port,
        auth => $hadoop_security_authentication,
  }

  hadoop-hbase::master { "hbase master":
        rootdir => $hadoop_hbase_rootdir,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        kerberos_realm => $kerberos_realm, 
  }

  hadoop-oozie::server { "oozie server":
        kerberos_realm => $kerberos_realm, 
  }

  hadoop-zookeeper::server { "zookeeper":
        myid => "0",
        ensemble => $hadoop_zookeeper_ensemble,
  }

  hadoop::create_hdfs_dirs { [ "/mapred", "/tmp", "/system", "/user", "/hbase", "/benchmarks", "/user/jenkins", "/user/hive" ]:
    hdfs_dirs_meta => { "/tmp"          => { perm => "777", user => "hdfs"   },
                        "/mapred"       => { perm => "755", user => "mapred" },
                        "/system"       => { perm => "755", user => "hdfs"   },
                        "/user"         => { perm => "755", user => "hdfs"   },
                        "/hbase"        => { perm => "755", user => "hbase"  },
                        "/benchmarks"   => { perm => "777", user => "hdfs"   },
                        "/user/jenkins" => { perm => "777", user => "jenkins"},
                        "/user/hive"    => { perm => "777", user => "hive"   } },
  }
}

class hadoop_gateway_node inherits hadoop_head_node {
  # hadoop::client { "gateway":
  #   namenode_host => $hadoop_namenode_host,
  #   namenode_port => $hadoop_namenode_port,
  #   jobtracker_host => $hadoop_jobtracker_host,
  #   jobtracker_port => $hadoop_jobtracker_port,
  #   # auth => $hadoop_security_authentication,
  # }

  mahout::client { "mahout client":
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
  hadoop-flume::client { "flume client":
  }
}
