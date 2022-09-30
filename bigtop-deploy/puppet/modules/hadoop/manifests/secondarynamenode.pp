class hadoop::secondarynamenode {
  include hadoop::common_hdfs
  package { "hadoop-hdfs-secondarynamenode":
    ensure => latest,
    require => Package["jdk"],
  }
  file {
    "/etc/default/hadoop-hdfs-secondarynamenode":
      content => template('hadoop/hadoop-hdfs'),
      require => [Package["hadoop-hdfs-secondarynamenode"]],
  }
  service { "hadoop-hdfs-secondarynamenode":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-hdfs-secondarynamenode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
    require => [Package["hadoop-hdfs-secondarynamenode"]],
  }
  Kerberos::Host_keytab <| title == "hdfs" |> -> Service["hadoop-hdfs-secondarynamenode"]
}
