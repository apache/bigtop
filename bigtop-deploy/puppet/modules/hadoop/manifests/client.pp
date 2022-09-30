class hadoop::client {
    include hadoop::common_mapred_app
    include hadoop::common_yarn
    $hadoop_client_packages = $operatingsystem ? {
      /(OracleLinux|CentOS|RedHat|Fedora)/  => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "hadoop-libhdfs", "hadoop-debuginfo" ],
      /(SLES|OpenSuSE)/                     => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "hadoop-libhdfs" ],
      /(Ubuntu|Debian)/                     => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "libhdfs0-dev"   ],
      default                               => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client" ],
    }
    package { $hadoop_client_packages:
      ensure => latest,
      require => [Package["jdk"], Package["hadoop"], Package["hadoop-hdfs"], Package["hadoop-mapreduce"]],  
    }
}
