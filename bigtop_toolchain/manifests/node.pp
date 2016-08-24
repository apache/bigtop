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
  $node_version = "0.10.44"
  $brunch_version = "1.7.20"
  $node_name = "node-v${node_version}-linux-x64"
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

  exec { "install brunch":
    command     => "/usr/local/node/bin/npm install -g brunch@${brunch_version}",
    cwd         => "/usr/local",
    creates     => "/usr/local/node/bin/brunch",
    require     => File["/usr/local/node"],
  }
}
