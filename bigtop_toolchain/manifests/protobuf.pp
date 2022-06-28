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

class bigtop_toolchain::protobuf {

  require bigtop_toolchain::packages

  $url = "https://github.com/protocolbuffers/protobuf/archive/refs/tags/"

  $protobuf8 = "v3.7.1.tar.gz"
  $protobuf8dir = "protobuf-3.7.1"

  exec { "download protobuf":
     cwd  => "/usr/src",
     command => "/usr/bin/wget $url/$protobuf8 && \
                 mkdir -p $protobuf8dir && \
                 /bin/tar -xvzf $protobuf8 -C $protobuf8dir --strip-components=1",
     creates => "/usr/src/$protobuf8dir",
  }

  exec { "install protobuf":
     cwd => "/usr/src/$protobuf8dir",
     command => "/usr/src/$protobuf8dir/autogen.sh && /usr/src/$protobuf8dir/configure --prefix=/usr/local --disable-shared --with-pic && /usr/bin/make install",
     creates => "/usr/local/bin/protoc",
     require => EXEC["download protobuf"],
     timeout => 3000
  }

  if ($architecture in ['aarch64', 'ppc64le']) {
    file { "/usr/src/0001-Backport-atomic-operations-with-support-of-arm64-and.patch":
      source => "puppet:///modules/bigtop_toolchain/0001-Backport-atomic-operations-with-support-of-arm64-and.patch"
    }

    file { "/usr/src/0001-CVE-2021-22569-Improve-performance-of-parsing-unknow.patch":
      source => "puppet:///modules/bigtop_toolchain/0001-CVE-2021-22569-Improve-performance-of-parsing-unknow.patch"
    }

    exec { "download protobuf 2.5.0":
      cwd  => "/usr/src",
      command => "/usr/bin/wget https://github.com/google/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.gz && \
                  mkdir -p /usr/src/protobuf-2.5.0 && \
                  /bin/tar -xvzf protobuf-2.5.0.tar.gz -C /usr/src/protobuf-2.5.0 --strip-components=1 && \
                  cd /usr/src/protobuf-2.5.0 && \
                  /usr/bin/patch -p1 </usr/src/0001-Backport-atomic-operations-with-support-of-arm64-and.patch && \
                  /usr/bin/patch -p1 </usr/src/0001-CVE-2021-22569-Improve-performance-of-parsing-unknow.patch && \
                  curl -o config.guess 'https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD' && \
                  cp config.guess gtest/build-aux/",
      creates => "/usr/src/protobuf-2.5.0",
      require => [File["/usr/src/0001-Backport-atomic-operations-with-support-of-arm64-and.patch"],
                  File["/usr/src/0001-CVE-2021-22569-Improve-performance-of-parsing-unknow.patch"]]
    }

    exec { "install protobuf 2.5.0":
      cwd => "/usr/src/protobuf-2.5.0",
      command => "/usr/src/protobuf-2.5.0/autogen.sh && \
                  /usr/src/protobuf-2.5.0/configure --prefix=/usr/local/protobuf-2.5.0 --disable-shared --with-pic && \
                  /usr/bin/make install",
      creates => "/usr/local/protobuf-2.5.0",
      require => Exec["download protobuf 2.5.0"],
      timeout => 3000
    }
  }
  
  if ($architecture == 'ppc64le') {
    exec { "download protobuf 3.17.3":
      cwd  => "/usr/src",
      command => "/usr/bin/wget https://github.com/protocolbuffers/protobuf/archive/refs/tags/v3.17.3.tar.gz && mkdir -p protobuf-3.17.3 && /bin/tar -xvzf v3.17.3.tar.gz -C protobuf-3.17.3 --strip-components=1",
      creates => "/usr/src/protobuf-3.17.3",
    }

    exec { "install protobuf 3.17.3":
      cwd => "/usr/src/protobuf-3.17.3",
      command => "/usr/src/protobuf-3.17.3/autogen.sh && /usr/src/protobuf-3.17.3/configure --prefix=/usr/local/protobuf-3.17.3 --disable-shared --with-pic && /usr/bin/make install",
      creates => "/usr/local/protobuf-3.17.3",
      require => Exec["download protobuf 3.17.3"],
      timeout => 3000
    }
  }

}
