class hadoop::common_yarn (
    $yarn_data_dirs = suffix($hadoop::hadoop_storage_dirs, "/yarn"),
    $hadoop_ps_host,
    $hadoop_ps_port = "20888",
    $hadoop_rm_host,
    $hadoop_rm_port = "8032",
    $hadoop_rm_admin_port = "8033",
    $hadoop_rm_webapp_port = "8088",
    $hadoop_rm_bind_host = undef,
    $hadoop_rt_port = "8025",
    $hadoop_sc_port = "8030",
    $yarn_log_server_url = undef,
    $yarn_nodemanager_resource_memory_mb = undef,
    $yarn_scheduler_maximum_allocation_mb = undef,
    $yarn_scheduler_minimum_allocation_mb = undef,
    $yarn_resourcemanager_scheduler_class = undef,
    $yarn_resourcemanager_ha_enabled = undef,
    $yarn_resourcemanager_cluster_id = "ha-rm-uri",
    $yarn_resourcemanager_zk_address = $hadoop::zk,
    # work around https://issues.apache.org/jira/browse/YARN-2847 by default
    $container_executor_banned_users = "doesnotexist",
    $container_executor_min_user_id = "499",
    $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
    $kerberos_realm = $hadoop::kerberos_realm,
    $yarn_nodemanager_vmem_check_enabled = undef,
) inherits hadoop {
  include hadoop::common
  package { "hadoop-yarn":
    ensure => latest,
    require => [Package["jdk"], Package["hadoop"]],
  }
  if ($hadoop_security_authentication == "kerberos") {
    require kerberos::client
    kerberos::host_keytab { "yarn":
      tag    => "mapreduce",
      spnego => true,
      # we don't actually need this package as long as we don't put the
      # keytab in a directory managed by it. But it creates user mapred whom we
      # wan't to give the keytab to.
      require => Package["hadoop-yarn"],
    }
  }
  file {
    "/etc/hadoop/conf/yarn-site.xml":
      content => template('hadoop/yarn-site.xml'),
      require => [Package["hadoop"]],
  }
  file { "/etc/hadoop/conf/container-executor.cfg":
    content => template('hadoop/container-executor.cfg'), 
    require => [Package["hadoop"]],
  }
}
