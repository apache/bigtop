class bigtop-toolchain::forrest{

  file{ '/tmp/apache-forrest-0.9.tar.gz':
    source => 'puppet:///modules/bigtop-toolchain/apache-forrest-0.9.tar.gz',
    ensure => present,
    owner  => root,
    group  => root,
    mode   => 755
  }

  exec{'/bin/tar xvzf /tmp/apache-forrest-0.9.tar.gz':
    cwd         => '/usr/local',
    refreshonly => true,
    subscribe   => File["/tmp/apache-forrest-0.9.tar.gz"],
  }

  file { '/usr/local/apache-forrest':
    ensure  => link,
    target  => '/usr/local/apache-forrest-0.9',
    require => Exec['/bin/tar xvzf /tmp/apache-forrest-0.9.tar.gz'],
  }
}
