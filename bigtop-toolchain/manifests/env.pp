class bigtop-toolchain::env {
  file { '/etc/profile.d/jenkins.sh':
    source => 'puppet:///modules/bigtop-toolchain/jenkins.sh',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 644,
  }
}
