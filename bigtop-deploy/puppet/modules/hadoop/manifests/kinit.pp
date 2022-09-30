class hadoop::kinit {
  include hadoop::common_hdfs
  exec { "HDFS kinit":
    command => "/usr/bin/kinit -kt /etc/hdfs.keytab hdfs/$fqdn && /usr/bin/kinit -R",
    user    => "hdfs",
    require => Kerberos::Host_keytab["hdfs"],
  }
}
