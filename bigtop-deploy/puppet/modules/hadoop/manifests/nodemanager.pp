class hadoop::nodemanager {
  include hadoop::common_mapred_app
  include hadoop::common_yarn
  package { "hadoop-yarn-nodemanager":
    ensure => latest,
    require => Package["jdk"],
  }

  service { "hadoop-yarn-nodemanager":
    ensure => running,
    hasstatus => true,
    subscribe => [Package["hadoop-yarn-nodemanager"], File["/etc/hadoop/conf/hadoop-env.sh"], 
                  File["/etc/hadoop/conf/yarn-site.xml"], File["/etc/hadoop/conf/core-site.xml"]],
    require => [ Package["hadoop-yarn-nodemanager"], File[$hadoop::common_yarn::yarn_data_dirs] ],
  }
  
  Kerberos::Host_keytab <| tag == "mapreduce" |> -> Service["hadoop-yarn-nodemanager"]
  hadoop::create_storage_dir { $hadoop::common_yarn::yarn_data_dirs: } ->
  file { $hadoop::common_yarn::yarn_data_dirs:
    ensure => directory,
    owner => yarn,
    group => yarn,
    mode => '755',
    require => [Package["hadoop-yarn"]],
  }
}
