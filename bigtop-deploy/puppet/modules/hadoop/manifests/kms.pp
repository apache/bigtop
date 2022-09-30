class hadoop::kms ($kms_host = $hadoop::kms_host,
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
