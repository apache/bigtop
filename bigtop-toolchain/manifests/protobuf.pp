class bigtop-toolchain::protobuf {
  file { '/etc/yum.repos.d/mrdocs-protobuf-rpm.repo':
    source => 'puppet:///modules/bigtop-toolchain/mrdocs-protobuf-rpm.repo',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 755,
  }
  
  package { 'protobuf-devel':
    ensure => present,
    require => File['/etc/yum.repos.d/mrdocs-protobuf-rpm.repo'],
  }
}

