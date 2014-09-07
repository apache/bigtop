# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class bigtop_toolchain::packages {
  case $operatingsystem{
  /(?i:(centos|fedora))/: { $pkgs = [ "wget", "git", "make" , "cmake" , "rpm-build" , "lzo-devel", "redhat-rpm-config", "openssl-devel", "fuse-libs", "fuse-devel", "fuse", "gcc", "gcc-c++", "autoconf", "automake", "libtool", "createrepo", "cppunit-devel"] }
  /(?i:(SLES|opensuse))/: { $pkgs = [ "unzip", "wget", "git", "make" , "cmake" , "rpm-devel" , "lzo-devel", "libopenssl-devel", "fuse-devel", "fuse", "gcc", "gcc-c++", "autoconf", "automake", "libtool", "pkg-config", "createrepo", "libcppunit-devel"] }
  Ubuntu: { $pkgs = [ "liblzo2-dev", "libzip-dev", "sharutils", "libfuse-dev", "cmake", "pkg-config", "debhelper", "devscripts", "protobuf-compiler", "build-essential", "dh-make", "reprepro", "automake", "autoconf", "libfuse2", "libssh-dev", "libjansi-java", "libcppunit-dev" ] }
}
  package { $pkgs:
    ensure => installed,
  }
}
