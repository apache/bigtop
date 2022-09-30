class hadoop::init_hdfs {
  exec { "init hdfs":
    path    => ['/bin','/sbin','/usr/bin','/usr/sbin'],
    command => 'bash -x /usr/lib/hadoop/libexec/init-hdfs.sh',
    require => Package['hadoop-hdfs']
  }
}
