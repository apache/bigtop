class bigtop-toolchain::ant {
  file { '/tmp/apache-ant-1.9.0-bin.tar.gz':
    source => 'puppet:///modules/bigtop-toolchain/apache-ant-1.9.0-bin.tar.gz',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 755
  }
  
  exec {'/bin/tar xvzf /tmp/apache-ant-1.9.0-bin.tar.gz':
    cwd         => '/usr/local',
    refreshonly => true,
    subscribe   => File["/tmp/apache-ant-1.9.0-bin.tar.gz"],
  }

  file {'/usr/local/ant':
    ensure  => link,
    target  => '/usr/local/apache-ant-1.9.0',
    require => Exec['/bin/tar xvzf /tmp/apache-ant-1.9.0-bin.tar.gz'],
  }
}
