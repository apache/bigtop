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
class bigtop_toolchain::isal {
  require bigtop_toolchain::packages
  $url = "https://github.com/intel/isa-l/archive/refs/tags/v2.29.0.tar.gz"
  $isal = "v2.29.0.tar.gz"
  $isaldir = "isa-l-2.29.0"
  exec { "download isal":
    cwd     => "/usr/src",
    command => "/usr/bin/wget $url && mkdir -p $isaldir && /bin/tar -xvzf $isal -C $isaldir --strip-components=1",
    creates => "/usr/src/$isaldir",
  }
  exec { "install isal":
    cwd     => "/usr/src/$isaldir",
    command => "/usr/src/$isaldir/autogen.sh && /usr/src/$isaldir/configure && /usr/bin/make && /usr/bin/make install",
    require => EXEC["download isal"],
    timeout => 3000
  }
}
