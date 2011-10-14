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

node hadoop_cluster_node {
  $hadoop_head_node="ip-10-114-221-125.ec2.internal"

  $hadoop_namenode_host="$hadoop_head_node"
  $hadoop_namenode_port="17020"
  $hadoop_namenode_thrift_port="10090"
  # $hadoop_dfs_namenode_plugins="org.apache.hadoop.thriftfs.NamenodePlugin"
  # $hadoop_dfs_datanode_plugins="org.apache.hadoop.thriftfs.DatanodePlugin"

  $hadoop_jobtracker_host="$hadoop_head_node"
  $hadoop_jobtracker_port="8021"
  $hadoop_jobtracker_thrift_port="9290"
  # $hadoop_mapred_jobtracker_plugins="org.apache.hadoop.thriftfs.ThriftJobTrackerPlugin"
  # $hadoop_mapred_tasktracker_plugins="org.apache.hadoop.mapred.TaskTrackerCmonInst"

  $hadoop_core_proxyusers = { oozie => { groups => '*', hosts => '*' } }

  $hadoop_hbase_rootdir = "hdfs://$hadoop_namenode_host:$hadoop_namenode_port/hbase"
  $hadoop_hbase_zookeeper_quorum = "$hadoop_head_node"

  $hadoop_zookeeper_ensemble = ["$hadoop_head_node:2888:3888"]

  # $hadoop_security_authentication="kerberos"

  # $kerberos_domain = "example.com"
  # $kerberos_realm = "EXAMPLE.COM"
  # $kerberos_kdc_server = "c0405"

  # include kerberos::client
  # kerberos::client::host_keytab { ["hdfs", "mapred", "hbase", "oozie"]:
  #  princs_map => { hdfs   => [ "host", "hdfs" ],
  #                  mapred => [ "mapred" ],
  #                  hbase  => [ "hbase"  ],
  #                  oozie  => [ "oozie"  ], },
  # }
}

node hadoop_worker_node inherits hadoop_cluster_node {
  hadoop::datanode { "datanode":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        # auth => $hadoop_security_authentication,
  }

  hadoop::tasktracker { "tasktracker":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        jobtracker_host => $hadoop_jobtracker_host,
        jobtracker_port => $hadoop_jobtracker_port,
        # auth => $hadoop_security_authentication,
  }

  hadoop-hbase::server { "hbase region server":
        rootdir => $hadoop_hbase_rootdir,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        # kerberos_realm => $kerberos_realm, 
  }

  $hdfs_data_dir = ["/mnt/data"]
  $mapred_data_dir = [ "/mnt/scratch" ]

  file {
      $mapred_data_dir:
          ensure => directory,
          owner => mapred,
          group => mapred,
          mode => 755,
  }

  file {
      $hdfs_data_dir:
          ensure => directory,
          owner => hdfs,
          group => hdfs,
          mode => 700,
  }
}


node hadoop_head_node inherits hadoop_cluster_node {

  # include kerberos::kdc, kerberos::kdc::admin_server

  hadoop::namenode { "namenode":
        port => $hadoop_namenode_port,
        jobtracker_host => $hadoop_jobtracker_host,
        jobtracker_port => $hadoop_jobtracker_port,
        # thrift_port => $hadoop_namenode_thrift_port,
        # auth => $hadoop_security_authentication,
  }

  hadoop::secondarynamenode { "secondary namenode":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        # auth => $hadoop_security_authentication,
  }

  hadoop::jobtracker { "jobtracker":
        namenode_host => $hadoop_namenode_host,
        namenode_port => $hadoop_namenode_port,
        host => $hadoop_jobtracker_host,
        port => $hadoop_jobtracker_port,
        # thrift_port => $hadoop_jobtracker_thrift_port,
        # auth => $hadoop_security_authentication,
  }

  hadoop-hbase::master { "hbase master":
        rootdir => $hadoop_hbase_rootdir,
        zookeeper_quorum => $hadoop_hbase_zookeeper_quorum,
        # kerberos_realm => $kerberos_realm, 
  }

  hadoop-oozie::server { "oozie server":
        # kerberos_realm => $kerberos_realm, 
  }

  hadoop-zookeeper::server { "zookeeper":
        myid => "0",
        ensemble => $hadoop_zookeeper_ensemble,
  }

  $namenode_data_dir = ["/mnt/namenode"]

  file { $namenode_data_dir:
    ensure => directory,
    owner => hdfs,
    group => hdfs,
    mode => 700,
  }

  $mapred_data_dir = ["/mnt/scratch"]

  file { $mapred_data_dir:
    ensure => directory,
    owner => mapred,
    group => mapred,
    mode => 755,
  }
}

node hadoop_gateway_node inherits hadoop_head_node {
  # hadoop::client { "gateway":
  #   namenode_host => $hadoop_namenode_host,
  #   namenode_port => $hadoop_namenode_port,
  #   jobtracker_host => $hadoop_jobtracker_host,
  #   jobtracker_port => $hadoop_jobtracker_port,
  #   # auth => $hadoop_security_authentication,
  # }

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
