class hadoop::common ($hadoop_java_home = undef,
    $hadoop_classpath = undef,
    $hadoop_heapsize = undef,
    $hadoop_opts = undef,
    $hadoop_namenode_opts = undef,
    $hadoop_secondarynamenode_opts = undef,
    $hadoop_datanode_opts = undef,
    $hadoop_balancer_opts = undef,
    $hadoop_jobtracker_opts = undef,
    $hadoop_tasktracker_opts = undef,
    $hadoop_client_opts = undef,
    $hadoop_ssh_opts = undef,
    $hadoop_log_dir = undef,
    $hadoop_slaves = undef,
    $hadoop_master = undef,
    $hadoop_slave_sleep = undef,
    $hadoop_pid_dir = undef,
    $hadoop_ident_string = undef,
    $hadoop_niceness = undef,
    $use_tez = false,
    $tez_conf_dir = undef,
    $tez_jars = undef,
) inherits hadoop {
  file {
    "/etc/hadoop/conf/hadoop-env.sh":
      content => template('hadoop/hadoop-env.sh'),
      require => [Package["hadoop"]],
  }
  package { "hadoop":
    ensure => latest,
    require => Package["jdk"],
  }
  #FIXME: package { "hadoop-native":
  #  ensure => latest,
  #  require => [Package["hadoop"]],
  #}
}
