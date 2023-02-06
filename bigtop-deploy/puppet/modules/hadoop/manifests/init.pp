# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class hadoop ($hadoop_security_authentication = "simple",
  $kerberos_realm = undef,
  $zk = "",
  # Set from facter if available
  $hadoop_storage_dirs = split($::hadoop_storage_dirs, ";"),
  $proxyusers = {
    spark => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
    oozie => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
     hive => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" },
   httpfs => { groups => 'hudson,testuser,root,hadoop,jenkins,oozie,hive,httpfs,users,spark', hosts => "*" } },
  $generate_secrets = false,
  $kms_host = undef,
  $kms_port = undef,
  $hadoop_ssl_hostname_verifier = undef,
  $hadoop_http_authentication_type = undef,
  $hadoop_http_authentication_signature_secret = undef,
  $hadoop_http_authentication_signature_secret_file = "/etc/hadoop/conf/hadoop-http-authentication-signature-secret",
  $hadoop_http_authentication_cookie_domain = regsubst($fqdn, "^[^\\.]+\\.", ""),
) {

  include stdlib

  class deploy ($roles) {

    if ("datanode" in $roles) {
      include hadoop::datanode
    }

    if ("namenode" in $roles) {
      include hadoop::init_hdfs
      include hadoop::namenode

      if ("datanode" in $roles) {
        Class['Hadoop::Namenode'] -> Class['Hadoop::Datanode'] -> Class['Hadoop::Init_hdfs']
      } else {
        Class['Hadoop::Namenode'] -> Class['Hadoop::Init_hdfs']
      }
    }

    if ("standby-namenode" in $roles and $hadoop::common_hdfs::ha != "disabled") {
      include hadoop::namenode
    }

    if ("mapred-app" in $roles) {
      include hadoop::mapred_app
    }

    if ("nodemanager" in $roles) {
      include hadoop::nodemanager
    }

    if ("resourcemanager" in $roles) {
      include hadoop::resourcemanager
      include hadoop::historyserver
      include hadoop::proxyserver

      if ("nodemanager" in $roles) {
        Class['Hadoop::Resourcemanager'] -> Class['Hadoop::Nodemanager']
      }
    }

    if ("secondarynamenode" in $roles and $hadoop::common_hdfs::ha == "disabled") {
      include hadoop::secondarynamenode
    }

    if ("httpfs-server" in $roles) {
      include hadoop::httpfs
    }

    if ("kms" in $roles) {
      include hadoop::kms
    }

    if ("hadoop-client" in $roles) {
      include hadoop::client
    }
  }

  class init_hdfs {
    exec { "init hdfs":
      path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
      command => 'bash -x /usr/lib/hadoop/libexec/init-hdfs.sh',
      require => Package['hadoop-hdfs']
    }
  }

  class common ($hadoop_java_home = undef,
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

  class common_yarn (
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

  class common_hdfs ($ha = "disabled",
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

    file {
      "/etc/hadoop/conf/ssl-client.xml":
        content => template('hadoop/ssl-client.xml'),
        owner   => 'root',
        group   => 'hadoop',
        mode    => '0660',
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/ssl-server.xml":
        content => template('hadoop/ssl-server.xml'),
        owner   => 'root',
        group   => 'hadoop',
        mode    => '0660',
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/http.keystore":
        source  => "puppet:///modules/hadoop/http.keystore",
        owner   => 'root',
        group   => 'hadoop',
        mode    => '0660',
        require => [Package["hadoop"]],
    }

    file {
      "/etc/hadoop/conf/http.truststore":
        source  => "puppet:///modules/hadoop/http.truststore",
        owner   => 'root',
        group   => 'hadoop',
        mode    => '0660',
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

  class common_mapred_app (
      $mapreduce_cluster_acls_enabled = undef,
      $mapreduce_jobhistory_host = undef,
      $mapreduce_jobhistory_port = "10020",
      $mapreduce_jobhistory_webapp_port = "19888",
      $mapreduce_framework_name = "yarn",
      $mapred_data_dirs = suffix($hadoop::hadoop_storage_dirs, "/mapred"),
      $mapreduce_cluster_temp_dir = "/mapred/system",
      $yarn_app_mapreduce_am_staging_dir = "/user",
      $mapreduce_task_io_sort_factor = 64,              # 10 default
      $mapreduce_task_io_sort_mb = 256,                 # 100 default
      $mapreduce_reduce_shuffle_parallelcopies = undef, # 5 is default
      # processorcount == facter fact
      $mapreduce_tasktracker_map_tasks_maximum = inline_template("<%= [1, @processorcount.to_i * 0.20].max.round %>"),
      $mapreduce_tasktracker_reduce_tasks_maximum = inline_template("<%= [1, @processorcount.to_i * 0.20].max.round %>"),
      $mapreduce_tasktracker_http_threads = 60,         # 40 default
      $mapreduce_output_fileoutputformat_compress_type = "BLOCK", # "RECORD" default
      $mapreduce_map_output_compress = undef,
      $mapreduce_job_reduce_slowstart_completedmaps = undef,
      $mapreduce_map_memory_mb = undef,
      $mapreduce_reduce_memory_mb = undef,
      $mapreduce_map_java_opts = "-Xmx1024m",
      $mapreduce_reduce_java_opts = "-Xmx1024m",
      $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
      $kerberos_realm = $hadoop::kerberos_realm,
  ) inherits hadoop {
    include hadoop::common_hdfs

    package { "hadoop-mapreduce":
      ensure => latest,
      require => [Package["jdk"], Package["hadoop"]],
    }

    if ($hadoop_security_authentication == "kerberos") {
      require kerberos::client

      kerberos::host_keytab { "mapred":
        tag    => "mapreduce",
        spnego => true,
        # we don't actually need this package as long as we don't put the
        # keytab in a directory managed by it. But it creates user yarn whom we
        # wan't to give the keytab to.
        require => Package["hadoop-mapreduce"],
      }
    }

    file {
      "/etc/hadoop/conf/mapred-site.xml":
        content => template('hadoop/mapred-site.xml'),
        require => [Package["hadoop"]],
    }

    file { "/etc/hadoop/conf/taskcontroller.cfg":
      content => template('hadoop/taskcontroller.cfg'), 
      require => [Package["hadoop"]],
    }
  }

  class datanode (
    $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
  ) inherits hadoop {
    include hadoop::common_hdfs

    package { "hadoop-hdfs-datanode":
      ensure => latest,
      require => Package["jdk"],
    }

    file {
      "/etc/default/hadoop-hdfs-datanode":
        content => template('hadoop/hadoop-hdfs'),
        require => [Package["hadoop-hdfs-datanode"]],
    }

    service { "hadoop-hdfs-datanode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-hdfs-datanode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [ Package["hadoop-hdfs-datanode"], File["/etc/default/hadoop-hdfs-datanode"], File[$hadoop::common_hdfs::hdfs_data_dirs] ],
    }
    Kerberos::Host_keytab <| title == "hdfs" |> -> Service["hadoop-hdfs-datanode"]
    Service<| title == 'hadoop-hdfs-namenode' |> -> Service['hadoop-hdfs-datanode']

    hadoop::create_storage_dir { $hadoop::common_hdfs::hdfs_data_dirs: } ->
    file { $hadoop::common_hdfs::hdfs_data_dirs:
      ensure => directory,
      owner => hdfs,
      group => hdfs,
      mode => '755',
      require => [ Package["hadoop-hdfs"] ],
    }
  }

  class httpfs ($hadoop_httpfs_port = "14000",
      $secret = "hadoop httpfs secret",
      $generate_secrets = $hadoop::generate_secrets,
      $hadoop_core_proxyusers = $hadoop::proxyusers,
      $hadoop_security_authentcation = $hadoop::hadoop_security_authentication,
      $kerberos_realm = $hadoop::kerberos_realm,
  ) inherits hadoop {
    include hadoop::common_hdfs

    if ($hadoop_security_authentication == "kerberos") {
      kerberos::host_keytab { "httpfs":
        spnego => true,
        require => Package["hadoop-httpfs"],
      }
    }

    package { "hadoop-httpfs":
      ensure => latest,
      require => Package["jdk"],
    }

    file { "/etc/hadoop/conf/httpfs-site.xml":
      content => template('hadoop/httpfs-site.xml'),
      require => [Package["hadoop-httpfs"]],
    }

    file { "/etc/hadoop/conf/httpfs-env.sh":
      content => template('hadoop/httpfs-env.sh'),
      require => [Package["hadoop-httpfs"]],
    }

    if $generate_secrets {
      $httpfs_signature_secret = trocla("httpfs-signature-secret", "plain")
    } else {
      $httpfs_signature_secret = $secret
    }
    if $httpfs_signature_secret == undef {
      fail("HTTPFS signature secret must be set")
    }

    file { "/etc/hadoop/conf/httpfs-signature.secret":
      content => $httpfs_signature_secret,
      # it's a password file - do not filebucket
      backup => false,
      require => [Package["hadoop-httpfs"]],
    }

    service { "hadoop-httpfs":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-httpfs"], File["/etc/hadoop/conf/httpfs-site.xml"], File["/etc/hadoop/conf/httpfs-env.sh"], File["/etc/hadoop/conf/httpfs-signature.secret"],
        File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"]],
      require => [ Package["hadoop-httpfs"] ],
    }
    Kerberos::Host_keytab <| title == "httpfs" |> -> Service["hadoop-httpfs"]
  }

  class kms ($kms_host = $hadoop::kms_host,
      $kms_port = $hadoop::kms_port,
      $secret = "hadoop kms secret",
      $generate_secrets = $hadoop::generate_secrets,
      $hadoop_core_proxyusers = $hadoop::proxyusers,
      $hadoop_security_authentcation = $hadoop::hadoop_security_authentication,
      $kerberos_realm = $hadoop::kerberos_realm,
  ) inherits hadoop {
    include hadoop::common_hdfs

    if ($hadoop_security_authentication == "kerberos") {
      kerberos::host_keytab { "kms":
        spnego => true,
        require => Package["hadoop-kms"],
      }
    }

    package { "hadoop-kms":
      ensure => latest,
      require => Package["jdk"],
    }

    file { "/etc/hadoop/conf/kms-site.xml":
      content => template('hadoop/kms-site.xml'),
      require => [Package["hadoop-kms"]],
    }

    file { "/etc/hadoop/conf/kms-env.sh":
      content => template('hadoop/kms-env.sh'),
      owner   => 'kms',
      group   => 'kms',
      mode    => '0400',
      require => [Package["hadoop-kms"]],
    }

    file { "/etc/hadoop/conf/kms.keystore.password":
      content => 'keystore-password',
      owner   => 'kms',
      group   => 'kms',
      mode    => '0400',
      require => [Package["hadoop-kms"]],
    }

    if $generate_secrets {
      $kms_signature_secret = trocla("kms-signature-secret", "plain")
    } else {
      $kms_signature_secret = $secret
    }
    if $kms_signature_secret == undef {
      fail("KMS signature secret must be set")
    }

    file { "/etc/hadoop/conf/kms-signature.secret":
      content => $kms_signature_secret,
      # it's a password file - do not filebucket
      backup => false,
      require => [Package["hadoop-kms"]],
    }

    service { "hadoop-kms":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-kms"], File["/etc/hadoop/conf/kms-site.xml"], File["/etc/hadoop/conf/kms-env.sh"], File["/etc/hadoop/conf/kms-signature.secret"],
        File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"]],
      require => [ Package["hadoop-kms"] ],
    }
    Kerberos::Host_keytab <| title == "kms" |> -> Service["hadoop-kms"]
  }

  class kinit {
    include hadoop::common_hdfs

    exec { "HDFS kinit":
      command => "/usr/bin/kinit -kt /etc/hdfs.keytab hdfs/$fqdn && /usr/bin/kinit -R",
      user    => "hdfs",
      require => Kerberos::Host_keytab["hdfs"],
    }
  }

  class create_hdfs_dirs($hdfs_dirs_meta,
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

  class rsync_hdfs($files,
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

  class namenode ( $nfs_server = "", $nfs_path = "",
      $standby_bootstrap_retries = 10,
      # milliseconds
      $standby_bootstrap_retry_interval = 30000) {
    include hadoop::common_hdfs

    if ($hadoop::common_hdfs::ha != 'disabled') {
      file { $hadoop::common_hdfs::sshfence_keydir:
        ensure  => directory,
        owner   => 'hdfs',
        group   => 'hdfs',
        mode    => '0700',
        require => Package["hadoop-hdfs"],
      }

      file { $hadoop::common_hdfs::sshfence_keypath:
        source  => "puppet:///files/$hadoop::common_hdfs::sshfence_privkey",
        owner   => 'hdfs',
        group   => 'hdfs',
        mode    => '0600',
        before  => Service["hadoop-hdfs-namenode"],
        require => File[$hadoop::common_hdfs::sshfence_keydir],
      }

      file { "$hadoop::common_hdfs::sshfence_keydir/authorized_keys":
        source  => "puppet:///files/$hadoop::common_hdfs::sshfence_pubkey",
        owner   => 'hdfs',
        group   => 'hdfs',
        mode    => '0600',
        before  => Service["hadoop-hdfs-namenode"],
        require => File[$hadoop::common_hdfs::sshfence_keydir],
      }

      if (! ('qjournal://' in $hadoop::common_hdfs::shared_edits_dir)) {
        hadoop::create_storage_dir { $hadoop::common_hdfs::shared_edits_dir: } ->
        file { $hadoop::common_hdfs::shared_edits_dir:
          ensure => directory,
        }

        if ($nfs_server) {
          if (!$nfs_path) {
            fail("No nfs share specified for shared edits dir")
          }

          require nfs::client

          mount { $hadoop::common_hdfs::shared_edits_dir:
            ensure  => "mounted",
            atboot  => true,
            device  => "${nfs_server}:${nfs_path}",
            fstype  => "nfs",
            options => "tcp,soft,timeo=10,intr,rsize=32768,wsize=32768",
            require => File[$hadoop::common::hdfs::shared_edits_dir],
            before  => Service["hadoop-hdfs-namenode"],
          }
        }
      }
    }

    package { "hadoop-hdfs-namenode":
      ensure => latest,
      require => Package["jdk"],
    }

    service { "hadoop-hdfs-namenode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-hdfs-namenode"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
      require => [Package["hadoop-hdfs-namenode"]],
    } 
    Kerberos::Host_keytab <| title == "hdfs" |> -> Exec <| tag == "namenode-format" |> -> Service["hadoop-hdfs-namenode"]

    if ($hadoop::common_hdfs::ha == "auto") {
      package { "hadoop-hdfs-zkfc":
        ensure => latest,
        require => Package["jdk"],
      }

      service { "hadoop-hdfs-zkfc":
        ensure => running,
        hasstatus => true,
        subscribe => [Package["hadoop-hdfs-zkfc"], File["/etc/hadoop/conf/core-site.xml"], File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/hadoop-env.sh"]],
        require => [Package["hadoop-hdfs-zkfc"]],
      } 
      Service <| title == "hadoop-hdfs-zkfc" |> -> Service <| title == "hadoop-hdfs-namenode" |>
    }

    $namenode_array = any2array($hadoop::common_hdfs::hadoop_namenode_host)
    $first_namenode = $namenode_array[0]
    if ($::fqdn == $first_namenode) {
      exec { "namenode format":
        user => "hdfs",
        command => "/bin/bash -c 'hdfs namenode -format -nonInteractive >> /var/lib/hadoop-hdfs/nn.format.log 2>&1'",
        returns => [ 0, 1],
        creates => "${hadoop::common_hdfs::namenode_data_dirs[0]}/current/VERSION",
        require => [ Package["hadoop-hdfs-namenode"], File[$hadoop::common_hdfs::namenode_data_dirs], File["/etc/hadoop/conf/hdfs-site.xml"] ],
        tag     => "namenode-format",
      }

      if ($hadoop::common_hdfs::ha != "disabled") {
        if ($hadoop::common_hdfs::ha == "auto") {
          exec { "namenode zk format":
            user => "hdfs",
            command => "/bin/bash -c 'hdfs zkfc -formatZK -nonInteractive >> /var/lib/hadoop-hdfs/zk.format.log 2>&1'",
            returns => [ 0, 2],
            require => [ Package["hadoop-hdfs-zkfc"], File["/etc/hadoop/conf/hdfs-site.xml"] ],
            tag     => "namenode-format",
          }
          Service <| title == "zookeeper-server" |> -> Exec <| title == "namenode zk format" |>
          Exec <| title == "namenode zk format" |>  -> Service <| title == "hadoop-hdfs-zkfc" |>
        } else {
          exec { "activate nn1": 
            command => "/usr/bin/hdfs haadmin -transitionToActive nn1",
            user    => "hdfs",
            unless  => "/usr/bin/test $(/usr/bin/hdfs haadmin -getServiceState nn1) = active",
            require => Service["hadoop-hdfs-namenode"],
          }
        }
      }
    } elsif ($hadoop::common_hdfs::ha == "auto") {
      $retry_params = "-Dipc.client.connect.max.retries=$standby_bootstrap_retries \
        -Dipc.client.connect.retry.interval=$standby_bootstrap_retry_interval"

      exec { "namenode bootstrap standby":
        user => "hdfs",
        # first namenode might be rebooting just now so try for some time
        command => "/bin/bash -c 'hdfs namenode -bootstrapStandby $retry_params >> /var/lib/hadoop-hdfs/nn.bootstrap-standby.log 2>&1'",
        creates => "${hadoop::common_hdfs::namenode_data_dirs[0]}/current/VERSION",
        require => [ Package["hadoop-hdfs-namenode"], File[$hadoop::common_hdfs::namenode_data_dirs], File["/etc/hadoop/conf/hdfs-site.xml"] ],
        tag     => "namenode-format",
      }
    } elsif ($hadoop::common_hdfs::ha != "disabled") {
      hadoop::namedir_copy { $hadoop::common_hdfs::namenode_data_dirs:
        source       => $first_namenode,
        ssh_identity => $hadoop::common_hdfs::sshfence_keypath,
        require      => File[$hadoop::common_hdfs::sshfence_keypath],
      }
    }

    file {
      "/etc/default/hadoop-hdfs-namenode":
        content => template('hadoop/hadoop-hdfs'),
        require => [Package["hadoop-hdfs-namenode"]],
    }

    hadoop::create_storage_dir { $hadoop::common_hdfs::namenode_data_dirs: } ->
    file { $hadoop::common_hdfs::namenode_data_dirs:
      ensure => directory,
      owner => hdfs,
      group => hdfs,
      mode => '700',
      require => [Package["hadoop-hdfs"]], 
    }
  }

  define create_storage_dir {
    exec { "mkdir $name":
      command => "/bin/mkdir -p $name",
      creates => $name,
      user =>"root",
    }
  }

  define namedir_copy ($source, $ssh_identity) {
    exec { "copy namedir $title from first namenode":
      command => "/usr/bin/rsync -avz -e '/usr/bin/ssh -oStrictHostKeyChecking=no -i $ssh_identity' '${source}:$title/' '$title/'",
      user    => "hdfs",
      tag     => "namenode-format",
      creates => "$title/current/VERSION",
    }
  }
      
  class secondarynamenode {
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

  class journalnode {
    include hadoop::common_hdfs

    package { "hadoop-hdfs-journalnode":
      ensure => latest,
      require => Package["jdk"],
    }

    $journalnode_cluster_journal_dir = "${hadoop::common_hdfs::journalnode_edits_dir}/${hadoop::common_hdfs::nameservice_id}"

    service { "hadoop-hdfs-journalnode":
      ensure => running,
      hasstatus => true,
      subscribe => [Package["hadoop-hdfs-journalnode"], File["/etc/hadoop/conf/hadoop-env.sh"],
                    File["/etc/hadoop/conf/hdfs-site.xml"], File["/etc/hadoop/conf/core-site.xml"]],
      require => [ Package["hadoop-hdfs-journalnode"], File[$journalnode_cluster_journal_dir] ],
    }

    hadoop::create_storage_dir { [$hadoop::common_hdfs::journalnode_edits_dir, $journalnode_cluster_journal_dir]: } ->
    file { [ "${hadoop::common_hdfs::journalnode_edits_dir}", "$journalnode_cluster_journal_dir" ]:
      ensure => directory,
      owner => 'hdfs',
      group => 'hdfs',
      mode => '755',
      require => [Package["hadoop-hdfs"]],
    }
  }


  class resourcemanager {
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

  class proxyserver {
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

  class historyserver {
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


  class nodemanager {
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

  class mapred_app {
    include hadoop::common_mapred_app

    hadoop::create_storage_dir { $hadoop::common_mapred_app::mapred_data_dirs: } ->
    file { $hadoop::common_mapred_app::mapred_data_dirs:
      ensure => directory,
      owner => yarn,
      group => yarn,
      mode => '755',
      require => [Package["hadoop-mapreduce"]],
    }
  }

  class client {
      include hadoop::common_mapred_app
      include hadoop::common_yarn

      $hadoop_client_packages = $operatingsystem ? {
        /(OracleLinux|CentOS|RedHat|Fedora)/  => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "hadoop-libhdfs", "hadoop-debuginfo" ],
        /(SLES|OpenSuSE)/                     => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "hadoop-libhdfs" ],
        /(Ubuntu|Debian)/                     => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client", "libhdfs0-dev"   ],
        default                               => [ "hadoop-doc", "hadoop-hdfs-fuse", "hadoop-client" ],
      }

      package { $hadoop_client_packages:
        ensure => latest,
        require => [Package["jdk"], Package["hadoop"], Package["hadoop-hdfs"], Package["hadoop-mapreduce"]],  
      }
  }

}
