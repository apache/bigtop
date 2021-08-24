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

class bigtop_toolchain::grpc {

  require bigtop_toolchain::packages
  $grpc_version = '1.28.0'
  $proto_version = '3.5.1.1'
  $proto_home = "/usr/local/protobuf-3.5.1.1"

  if ($architecture == 'ppc64le') {

  exec { "download grpc-java ${grpc_version}":
      cwd  => "/usr/src",
      command => "/usr/bin/wget https://github.com/grpc/grpc-java/archive/refs/tags/v${grpc_version}.tar.gz && mkdir -p grpc-java-${grpc_version} && /bin/tar -xvzf v${grpc_version}.tar.gz -C grpc-java-${grpc_version} --strip-components=1",
      creates => "/usr/src/grpc-java-${grpc_version}",
  }

    file { "/usr/src/grpc-java-${grpc_version}/grpc-java-${grpc_version}-add-support-for-ppc64le.patch":
      source => "puppet:///modules/bigtop_toolchain/grpc-java-${grpc_version}-add-support-for-ppc64le.patch",
      require => Exec["download grpc-java ${grpc_version}"],
    }

  exec { "build grpc-java ${grpc_version}":
      cwd => "/usr/src/grpc-java-${grpc_version}",
      command => "/usr/bin/patch -p1 < grpc-java-${grpc_version}-add-support-for-ppc64le.patch && export LDFLAGS=-L/${proto_home}/lib && export CXXFLAGS=-I/${proto_home}/include && export LD_LIBRARY_PATH=/${proto_home}/lib && cd compiler && ../gradlew java_pluginExecutable -PskipAndroid=true",
      creates => "/usr/local/grpc-java-${grpc_version}",
      require => File["/usr/src/grpc-java-${grpc_version}/grpc-java-${grpc_version}-add-support-for-ppc64le.patch"],
      timeout => 3000
    }
  }

}
