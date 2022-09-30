class hadoop::rsync_hdfs($files,
    $hadoop_security_authentcation = $hadoop::hadoop_security_authentication ) inherits hadoop {
  $src = $files[$title]
  if ($hadoop_security_authentication == "kerberos") {
    require hadoop::kinit
    Exec["HDFS kinit"] -> Exec["HDFS init $title"]
  }
  exec { "HDFS rsync $title":
    user => "hdfs",
    command => "/bin/bash -c 'hadoop fs -mkdir -p $title ; hadoop fs -put -f $src $title'",
    require => Service["hadoop-hdfs-namenode"],
  }
  Exec <| title == "activate nn1" |>  -> Exec["HDFS rsync $title"]
}
