class hadoop::httpfs ($hadoop_httpfs_port = "14000",
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
