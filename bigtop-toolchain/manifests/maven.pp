class bigtop-toolchain::maven  {
  file { '/tmp/apache-maven-3.0.5-bin.tar.gz':
    source => 'puppet:///modules/bigtop-toolchain/apache-maven-3.0.5-bin.tar.gz',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 755
  }
  
  exec {'/bin/tar xvzf /tmp/apache-maven-3.0.5-bin.tar.gz':
    cwd         => '/usr/local',
    refreshonly => true,
    subscribe   => File["/tmp/apache-maven-3.0.5-bin.tar.gz"],
  }
  
  file {'/usr/local/maven':
    ensure  => link,
    target  => '/usr/local/apache-maven-3.0.5',
    require => Exec['/bin/tar xvzf /tmp/apache-maven-3.0.5-bin.tar.gz'],
  }
}
