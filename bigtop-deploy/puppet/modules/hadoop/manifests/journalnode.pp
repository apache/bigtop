class hadoop::journalnode {
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
