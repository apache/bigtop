class hadoop::resourcemanager {
  include hadoop::common_yarn
  package { "hadoop-yarn-resourcemanager":
    ensure => latest,
    require => Package["jdk"],
  }
  service { "hadoop-yarn-resourcemanager":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-yarn-resourcemanager"], File["/etc/hadoop/conf/hadoop-env.sh"], 
                  File["/etc/hadoop/conf/yarn-site.xml"], File["/etc/hadoop/conf/core-site.xml"]],
    require => [ Package["hadoop-yarn-resourcemanager"] ],
  }
  Kerberos::Host_keytab <| tag == "mapreduce" |> -> Service["hadoop-yarn-resourcemanager"]
}
