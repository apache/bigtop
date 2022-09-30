class hadoop::create_hdfs_dirs(
  $hdfs_dirs_meta,
  $hadoop_security_authentcation = $hadoop::hadoop_security_authentication ) inherits hadoop {
  $user = $hdfs_dirs_meta[$title][user]
  $perm = $hdfs_dirs_meta[$title][perm]

  if ($hadoop_security_authentication == "kerberos") {
    require hadoop::kinit
    Exec["HDFS kinit"] -> Exec["HDFS init $title"]
  }

  exec { "HDFS init $title":
    user => "hdfs",
    command => "/bin/bash -c 'hadoop fs -mkdir $title ; hadoop fs -chmod $perm $title && hadoop fs -chown $user $title'",
    require => Service["hadoop-hdfs-namenode"],
  }

  Exec <| title == "activate nn1" |>  -> Exec["HDFS init $title"]
  
}
