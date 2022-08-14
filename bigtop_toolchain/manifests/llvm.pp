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

class bigtop_toolchain::llvm {

  require bigtop_toolchain::gnupg

  $llvmversion = "14.0.6"
  $llvmproject = "llvm-project"
  $llvm = "${llvmproject}-${llvmversion}"

  if ($operatingsystem == 'CentOS' and $operatingsystemmajrelease == 7) {
    $make = "/usr/bin/scl enable devtoolset-9 -- make"
    $cmake = "/usr/bin/scl enable devtoolset-9 -- cmake"
  } else {
    $make = "/usr/bin/make"
    $cmake = "/usr/bin/cmake"
  }

  exec { 'Mkdirs':
    command => "/usr/bin/mkdir -p ${llvmproject}/build",
    cwd     => "/usr/src",
    creates => "/usr/src/${llvmproject}/build",
  } ~>

  exec { 'Download LLVM Binaries':
    command => "/usr/bin/wget https://github.com/llvm/${llvmproject}/releases/download/llvmorg-${llvmversion}/${llvm}.src.tar.xz",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/${llvm}.src.tar.xz",
  } ~>

  exec { 'Download LLVM Binaries Signature':
    command => "/usr/bin/wget https://github.com/llvm/${llvmproject}/releases/download/llvmorg-${llvmversion}/${llvm}.src.tar.xz.sig",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/${llvm}.src.tar.xz.sig",
  } ~>

  exec { 'Verify LLVM Binaries Signature':
    command => "/usr/bin/$bigtop_toolchain::gnupg::cmd --no-tty -v --verify --auto-key-retrieve --keyserver hkp://keyserver.ubuntu.com ${llvm}.src.tar.xz.sig",
    cwd     => "/usr/src",
  } ->

  exec { 'Extract LLVM Binaries':
    command => "/bin/tar xf /usr/src/${llvm}.src.tar.xz --strip 1 -C ${llvmproject}",
    cwd     => "/usr/src",
  } ->

  exec { 'CMake LLVM':
    command => "${cmake} -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS=\"clang;lld\" -DLLVM_ENABLE_RUNTIMES=\"compiler-rt;libc;libcxx;libcxxabi\" -DCMAKE_INSTALL_PREFIX=/usr/local ../llvm",
    cwd     => "/usr/src/${llvmproject}/build",
    timeout => 600
  } ->

  exec { 'Make LLVM':
    command => "${make} -j ${processorcount} install",
    cwd     => "/usr/src/${llvmproject}/build",
    timeout => 7200
  } -> 

  exec { 'Remove LLVM Source Directory':
    command => "/usr/bin/rm -rf ${llvmproject}",
    cwd     => "/usr/src"
  }
}
