class bigtop-toolchain::user {

  user { 'jenkins':
    ensure => present,
    home => '/var/lib/jenkins',
    managehome => true,
  }

  file {"/var/lib/jenkins/.m2":
    ensure => directory,
    owner => 'jenkins',
    group => 'jenkins',
    mode => 755,
    require => User['jenkins'],
  }
}
