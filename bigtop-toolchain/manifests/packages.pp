class bigtop-toolchain::packages {
  case $operatingsystem{
  centos: { $pkgs = [ "wget", "git", "make" , "cmake" , "rpm-build" , "lzo-devel", "redhat-rpm-config", "openssl-devel", "fuse-libs", "fuse-devel", "fuse", "gcc", "gcc-c++", "autoconf", "automake", "libtool"] }
  SLES: { $pkgs = [ "wget", "git", "make" , "cmake" , "rpm-devel" , "lzo-devel", "libopenssl-devel", "fuse-devel", "fuse", "gcc", "gcc-c++", "autoconf", "automake", "libtool", "pkg-config"] }
}
  package { $pkgs:
    ensure => installed,
  }
}
