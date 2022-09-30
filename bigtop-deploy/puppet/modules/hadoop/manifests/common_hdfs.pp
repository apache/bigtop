class hadoop::common_hdfs ($ha = "disabled",
    $hadoop_config_dfs_block_size = undef,
    $hadoop_config_namenode_handler_count = undef,
    $hadoop_dfs_datanode_plugins = "",
    $hadoop_dfs_namenode_plugins = "",
    $hadoop_namenode_host = $fqdn,
    $hadoop_namenode_port = "8020",
    $hadoop_namenode_bind_host = undef,
    $hadoop_namenode_http_port = "50070",
    $hadoop_namenode_http_bind_host = undef,
    $hadoop_namenode_https_port = "50470",
    $hadoop_namenode_https_bind_host = undef,
    $hdfs_data_dirs = suffix($hadoop::hadoop_storage_dirs, "/hdfs"),
    $hdfs_shortcut_reader = undef,
    $hdfs_support_append = undef,
    $hdfs_replace_datanode_on_failure = undef,
    $hdfs_webhdfs_enabled = "true",
    $hdfs_replication = undef,
    $hdfs_datanode_fsdataset_volume_choosing_policy = undef,
    $hdfs_nfs_bridge = "disabled",
    $hdfs_nfs_bridge_user = undef,
    $hdfs_nfs_gw_host = undef,
    $hdfs_nfs_proxy_groups = undef,
    $namenode_data_dirs = suffix($hadoop::hadoop_storage_dirs, "/namenode"),
    $nameservice_id = "ha-nn-uri",
    $journalnode_host = "0.0.0.0",
    $journalnode_port = "8485",
    $journalnode_http_port = "8480",
    $journalnode_https_port = "8481",
    $journalnode_edits_dir = "${hadoop::hadoop_storage_dirs[0]}/journalnode",
    $shared_edits_dir = "/hdfs_shared",
    $testonly_hdfs_sshkeys  = "no",
    $hadoop_ha_sshfence_user_home = "/var/lib/hadoop-hdfs",
    $sshfence_privkey = "hadoop/id_sshfence",
    $sshfence_pubkey = "hadoop/id_sshfence.pub",
    $sshfence_user = "hdfs",
    $zk = $hadoop::zk,
    $hadoop_config_fs_inmemory_size_mb = undef,
    $hadoop_security_group_mapping = undef,
    $hadoop_core_proxyusers = $hadoop::proxyusers,
    $hadoop_snappy_codec = undef,
    $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
    $kerberos_realm = $hadoop::kerberos_realm,
    $hadoop_http_authentication_type = undef,
    $hadoop_http_authentication_signature_secret = undef,
    $hadoop_http_authentication_signature_secret_file = "/etc/hadoop/conf/hadoop-http-authentication-signature-secret",
    $hadoop_http_authentication_cookie_domain = regsubst($fqdn, "^[^\\.]+\\.", ""),
    $generate_secrets = $hadoop::generate_secrets,
    $namenode_datanode_registration_ip_hostname_check = undef,
    $kms_host = $hadoop::kms_host,
    $kms_port = $hadoop::kms_port,
) inherits hadoop {
  $sshfence_keydir  = "$hadoop_ha_sshfence_user_home/.ssh"
  $sshfence_keypath = "$sshfence_keydir/id_sshfence"
  include hadoop::common
# Check if test mode is enforced, so we can install hdfs ssh-keys for passwordless
  if ($testonly_hdfs_sshkeys == "yes") {
    notify{"WARNING: provided hdfs ssh keys are for testing purposes only.\n
      They shouldn't be used in production cluster": }
    $ssh_user        = "hdfs"
    $ssh_user_home   = "/var/lib/hadoop-hdfs"
    $ssh_user_keydir = "$ssh_user_home/.ssh"
    $ssh_keypath     = "$ssh_user_keydir/id_hdfsuser"
    $ssh_privkey     = "hadoop/hdfs/id_hdfsuser"
    $ssh_pubkey      = "hadoop/hdfs/id_hdfsuser.pub"
    file { $ssh_user_keydir:
      ensure  => directory,
      owner   => 'hdfs',
      group   => 'hdfs',
      mode    => '0700',
      require => Package["hadoop-hdfs"],
    }
    file { $ssh_keypath:
      source  => "puppet:///modules/$ssh_privkey",
      owner   => 'hdfs',
      group   => 'hdfs',
      mode    => '0600',
      require => File[$ssh_user_keydir],
    }
    file { "$ssh_user_keydir/authorized_keys":
      source  => "puppet:///modules/$ssh_pubkey",
      owner   => 'hdfs',
      group   => 'hdfs',
      mode    => '0600',
      require => File[$ssh_user_keydir],
    }
  }
  if ($hadoop_security_authentication == "kerberos" and $ha != "disabled") {
    fail("High-availability secure clusters are not currently supported")
  }
  package { "hadoop-hdfs":
    ensure => latest,
    require => [Package["jdk"], Package["hadoop"]],
  }
  if ($hadoop_security_authentication == "kerberos") {
    require kerberos::client
    kerberos::host_keytab { "hdfs":
      princs => [ "hdfs", "host" ],
      spnego => true,
      # we don't actually need this package as long as we don't put the
      # keytab in a directory managed by it. But it creates user hdfs whom we
      # wan't to give the keytab to.
      require => Package["hadoop-hdfs"],
    }
  }
  file {
    "/etc/hadoop/conf/core-site.xml":
      content => template('hadoop/core-site.xml'),
      require => [Package["hadoop"]],
  }
  file {
    "/etc/hadoop/conf/hdfs-site.xml":
      content => template('hadoop/hdfs-site.xml'),
      require => [Package["hadoop"]],
  }
  if $hadoop_http_authentication_type == "kerberos" {
    if $generate_secrets {
      $http_auth_sig_secret = trocla("hadoop_http_authentication_signature_secret", "plain")
    } else {
      $http_auth_sig_secret = $hadoop_http_authentication_signature_secret
    }
    if $http_auth_sig_secret == undef {
      fail("Hadoop HTTP authentication signature secret must be set")
    }
    file { 'hadoop-http-auth-sig-secret':
      path => "${hadoop_http_authentication_signature_secret_file}",
      # it's a password file - do not filebucket
      backup => false,
      mode => "0440",
      owner => "root",
      # allows access by hdfs and yarn (and mapred - mhmm...)
      group => "hadoop",
      content => $http_auth_sig_secret,
      require => [Package["hadoop"]],
    }
    # all the services will need this
    File['hadoop-http-auth-sig-secret'] ~> Service<| title == "hadoop-hdfs-journalnode" |>
    File['hadoop-http-auth-sig-secret'] ~> Service<| title == "hadoop-hdfs-namenode" |>
    File['hadoop-http-auth-sig-secret'] ~> Service<| title == "hadoop-hdfs-datanode" |>
    File['hadoop-http-auth-sig-secret'] ~> Service<| title == "hadoop-yarn-resourcemanager" |>
    File['hadoop-http-auth-sig-secret'] ~> Service<| title == "hadoop-yarn-nodemanager" |>
    require kerberos::client
    kerberos::host_keytab { "HTTP":
      # we need only the HTTP SPNEGO keys
      princs => [],
      spnego => true,
      owner => "root",
      group => "hadoop",
      mode => "0440",
      # we don't actually need this package as long as we don't put the
      # keytab in a directory managed by it. But it creates group hadoop which
      # we wan't to give the keytab to.
      require => Package["hadoop"],
    }
    # all the services will need this as well
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-hdfs-journalnode" |>
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-hdfs-namenode" |>
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-hdfs-datanode" |>
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-yarn-resourcemanager" |>
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-yarn-nodemanager" |>
    Kerberos::Host_keytab["HTTP"] -> Service<| title == "hadoop-kms" |>
  }
}
