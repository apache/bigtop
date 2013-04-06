class bigtop-toolchain::jdk {
  file { '/tmp/jdk-6u43-linux-amd64.rpm':
    source => 'puppet:///modules/bigtop-toolchain/jdk-6u43-linux-amd64.rpm',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 755
  }
  
  exec {'/bin/rpm -Uvh /tmp/jdk-6u43-linux-amd64.rpm':
    cwd         => '/tmp',
    refreshonly => true,
    subscribe   => File["/tmp/jdk-6u43-linux-amd64.rpm"],
  }
}
