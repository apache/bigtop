class bigtop-toolchain::installer {
  include bigtop-toolchain::jdk
  include bigtop-toolchain::maven
  include bigtop-toolchain::forrest
  include bigtop-toolchain::ant
  include bigtop-toolchain::protobuf
  include bigtop-toolchain::packages
  include bigtop-toolchain::env
  include bigtop-toolchain::user
}
