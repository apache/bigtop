class hadoop::namenode ( $nfs_server = "", $nfs_path = "",
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
