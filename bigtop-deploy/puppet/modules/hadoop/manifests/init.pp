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

class hadoop {

  /**
   * Common definitions for hadoop nodes.
   * They all need these files so we can access hdfs/jobs from any node
   */
  class common {
    file {
      "/etc/hadoop/conf/core-site.xml":
        content => template('hadoop/core-site.xml'),
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/mapred-site.xml":
        content => template('hadoop/mapred-site.xml'),
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/hdfs-site.xml":
        content => template('hadoop/hdfs-site.xml'),
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/hadoop-env.sh":
        content => template('hadoop/hadoop-env.sh'),
        require => [Package["hadoop"]],
    }

    file {
      "/etc/default/hadoop":
        content => template('hadoop/hadoop'),
        require => [Package["hadoop"]],
    }

    package { "hadoop":
      ensure => latest,
    }

    package { "hadoop-native":
      ensure => latest,
      require => [Package["hadoop"]],
    }
  }


  define datanode ($namenode_host, $namenode_port, $port = "50075", $auth = "simple", $dirs = ["/tmp/data"]) {

    $hadoop_namenode_host = $namenode_host
    $hadoop_namenode_port = $namenode_port
    $hadoop_datanode_port = $port
    $hadoop_security_authentication = $auth

    include common

    package { "hadoop-datanode":
      ensure => latest,
      require => Package["jdk"],
    }

    if ($hadoop_security_authentication == "kerberos") {
      package { "hadoop-sbin":
        ensure => latest,
        require => [Package["hadoop"]],
      }
    }

    service { "hadoop-datanode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-datanode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [ Package["hadoop-datanode"], File[$dirs] ],
    }

    file { $dirs:
      ensure => directory,
      owner => hdfs,
      group => hdfs,
      mode => 755,
      require => [Package["hadoop"]],
    }
  }

  define create_hdfs_dirs($hdfs_dirs_meta) {
    $user = $hdfs_dirs_meta[$title][user]
    $perm = $hdfs_dirs_meta[$title][perm]

    exec { "HDFS init $title":
      user => "hdfs",
      command => "/bin/bash -c 'hadoop fs -mkdir $title && hadoop fs -chmod $perm $title && hadoop fs -chown $user $title'",
      unless => "/bin/bash -c 'hadoop fs -ls $name >/dev/null 2>&1'",
      require => [ Service["hadoop-namenode"], Exec["namenode format"] ],
    }
  }

  define namenode ($jobtracker_host, $jobtracker_port, $host = $fqdn , $port = "8020", $thrift_port= "10090", $auth = "simple", $dirs = ["/tmp/nn"]) {

    $hadoop_namenode_host = $host
    $hadoop_namenode_port = $port
    $hadoop_namenode_thrift_port = $thrift_port
    $hadoop_jobtracker_host = $jobtracker_host
    $hadoop_jobtracker_port = $jobtracker_port
    $hadoop_security_authentication = $auth

    include common

    package { "hadoop-namenode":
      ensure => latest,
      require => Package["jdk"],
    }

    service { "hadoop-namenode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-namenode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [Package["hadoop-namenode"], Exec["namenode format"]],
    } 

    exec { "namenode format":
      user => "hdfs",
      command => "/bin/bash -c 'yes Y | hadoop namenode -format >> /tmp/nn.format.log 2>&1'",
      creates => inline_template("<%= hadoop_storage_locations.split(';')[0] %>/namenode/image"),
      require => [ Package["hadoop-namenode"], File[$dirs] ],
    } 
    
    file { $dirs:
      ensure => directory,
      owner => hdfs,
      group => hdfs,
      mode => 700,
      require => [Package["hadoop"]], 
    }
  }


  define jobtracker ($namenode_host, $namenode_port, $host = $fqdn, $port = "8021", $thrift_port = "9290", $auth = "simple", $dirs = ["/tmp/mr"]) {

    $hadoop_namenode_host = $namenode_host
      $hadoop_namenode_port = $namenode_port
      $hadoop_jobtracker_thrift_port = $thrift_port
      $hadoop_jobtracker_host = $host
      $hadoop_jobtracker_port = $port
      $hadoop_security_authentication = $auth

      include common

      package { "hadoop-jobtracker":
        ensure => latest,
               require => Package["jdk"],
      }

    service { "hadoop-jobtracker":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-jobtracker"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/mapred-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [ Package["hadoop-jobtracker"], File[$dirs] ],
    }
   
    file { $dirs:
      ensure => directory,
      owner => mapred,
      group => mapred,
      mode => 755,
      require => [Package["hadoop"]],
    }
  }


  define tasktracker ($namenode_host, $namenode_port, $jobtracker_host, $jobtracker_port, $auth = "simple", $dirs = ["/tmp/mr"]){

    $hadoop_namenode_host = $namenode_host
      $hadoop_namenode_port = $namenode_port
      $hadoop_jobtracker_host = $jobtracker_host
      $hadoop_jobtracker_port = $jobtracker_port
      $hadoop_security_authentication = $auth

      include common

      package { "hadoop-tasktracker":
        ensure => latest,
        require => Package["jdk"],
      }
 
    file { "/etc/hadoop/conf/taskcontroller.cfg":
      content => template('hadoop/taskcontroller.cfg'), 
    }

    service { "hadoop-tasktracker":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-tasktracker"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/mapred-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [ Package["hadoop-tasktracker"], File["/etc/hadoop/conf/taskcontroller.cfg"], File[$dirs] ],
    }
 
    file { $dirs:
      ensure => directory,
      owner => mapred,
      group => mapred,
      mode => 755,
      require => [Package["hadoop"]],
    }
  }


  define secondarynamenode ($namenode_host, $namenode_port, $port = "50090", $auth = "simple") {

    $hadoop_secondarynamenode_port = $port
    $hadoop_security_authentication = $auth
      include common

      package { "hadoop-secondarynamenode":
        ensure => latest,
        require => Package["jdk"],
      }

    service { "hadoop-secondarynamenode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-secondarynamenode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [Package["hadoop-secondarynamenode"]],
    }
  }

  define client ($namenode_host, $namenode_port, $jobtracker_host, $jobtracker_port, $auth = "simple") {
      $hadoop_namenode_host = $namenode_host
      $hadoop_namenode_port = $namenode_port
      $hadoop_jobtracker_host = $jobtracker_host
      $hadoop_jobtracker_port = $jobtracker_port
      $hadoop_security_authentication = $auth

      include common
  
      package { ["hadoop-doc", "hadoop-source", "hadoop-debuginfo", 
                 "hadoop-fuse", "hadoop-libhdfs", "hadoop-pipes"]:
        ensure => latest,
        require => [Package["jdk"], Package["hadoop"]],  
      }
  }
}
