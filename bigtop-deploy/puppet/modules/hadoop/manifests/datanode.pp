class hadoop::datanode (
  $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
) inherits hadoop {
  include hadoop::common_hdfs
  package { "hadoop-hdfs-datanode":
    ensure => latest,
    require => Package["jdk"],
  }
  file {
    "/etc/default/hadoop-hdfs-datanode":
      content => template('hadoop/hadoop-hdfs'),
      require => [Package["hadoop-hdfs-datanode"]],
  }
  service { "hadoop-hdfs-datanode":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-hdfs-datanode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
    require => [ Package["hadoop-hdfs-datanode"], File["/etc/default/hadoop-hdfs-datanode"], File[$hadoop::common_hdfs::hdfs_data_dirs] ],
  }
  Kerberos::Host_keytab <| title == "hdfs" |> -> Service["hadoop-hdfs-datanode"]
  Service<| title == 'hadoop-hdfs-namenode' |> -> Service['hadoop-hdfs-datanode']
  hadoop::create_storage_dir { $hadoop::common_hdfs::hdfs_data_dirs: } ->
  file { $hadoop::common_hdfs::hdfs_data_dirs:
    ensure => directory,
    owner => hdfs,
    group => hdfs,
    mode => '755',
    require => [ Package["hadoop-hdfs"] ],
  }
}
