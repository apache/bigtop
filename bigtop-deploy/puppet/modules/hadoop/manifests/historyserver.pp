class hadoop::historyserver {
  include hadoop::common_mapred_app
  package { "hadoop-mapreduce-historyserver":
    ensure => latest,
    require => Package["jdk"],
  }
  service { "hadoop-mapreduce-historyserver":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-mapreduce-historyserver"], File["/etc/hadoop/conf/hadoop-env.sh"], 
                  File["/etc/hadoop/conf/yarn-site.xml"], File["/etc/hadoop/conf/core-site.xml"]],
    require => [Package["hadoop-mapreduce-historyserver"]],
  }
  Kerberos::Host_keytab <| tag == "mapreduce" |> -> Service["hadoop-mapreduce-historyserver"]
}
