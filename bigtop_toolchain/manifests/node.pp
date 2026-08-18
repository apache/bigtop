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

class bigtop_toolchain::node {
   case $architecture {
      /amd64|x86_64/ : { $arch = "x64" }
      'ppc64le' : { $arch = "ppc64le" }
      'aarch64' : { $arch = "arm64" }
  }
  $node_version = "16.20.2"
  $node_name = "node-v${node_version}-linux-$arch"
  $node_dl_url = 'https://nodejs.org/dist/v$node_version/'

  exec { "get node":
    command => "/usr/bin/wget -O - https://nodejs.org/dist/v${node_version}/${node_name}.tar.gz | /bin/tar xzf -",
    cwd     => "/usr/local",
    unless  => "/usr/bin/test -x /usr/local/${node_name}/bin/npm",
  }

  file { "/usr/local/node":
    ensure  => link,
    target  => "/usr/local/${node_name}",
    require => Exec["get node"],
  }

}
