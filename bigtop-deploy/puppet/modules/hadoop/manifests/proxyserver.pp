class hadoop::proxyserver {
  include hadoop::common_yarn
  package { "hadoop-yarn-proxyserver":
    ensure => latest,
    require => Package["jdk"],
  }
  service { "hadoop-yarn-proxyserver":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-yarn-proxyserver"], File["/etc/hadoop/conf/hadoop-env.sh"], 
                  File["/etc/hadoop/conf/yarn-site.xml"], File["/etc/hadoop/conf/core-site.xml"]],
    require => [ Package["hadoop-yarn-proxyserver"] ],
  }
  Kerberos::Host_keytab <| tag == "mapreduce" |> -> Service["hadoop-yarn-proxyserver"]
}
