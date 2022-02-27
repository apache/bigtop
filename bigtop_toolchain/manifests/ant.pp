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

class bigtop_toolchain::ant {

  require bigtop_toolchain::gnupg
  require bigtop_toolchain::packages

  # Ant version restricted to 1.9 because 1.10 supports Java>=8 only.
  $ant =  latest_ant_binary("1.9.[0-9]*")
  $apache_prefix = nearest_apache_mirror()

  exec { 'Download Ant binaries':
    command => "/usr/bin/wget $apache_prefix/ant/binaries/$ant-bin.tar.gz",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/$ant-bin.tar.gz",
  } ~>

  exec { 'Download Ant binaries signature':
    command => "/usr/bin/wget $apache_prefix/ant/binaries/$ant-bin.tar.gz.asc",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/$ant-bin.tar.gz.asc",
  } ~>

  exec { 'Verify Ant binaries':
    command => "/usr/bin/$bigtop_toolchain::gnupg::cmd -v --verify --auto-key-retrieve --keyserver hkp://keyserver.ubuntu.com:80 $ant-bin.tar.gz.asc",
    cwd     => "/usr/src",
  } ->

  exec { 'Extract Ant binaries':
    command => "/bin/tar xvzf /usr/src/$ant-bin.tar.gz",
    cwd     => '/usr/local',
    creates => "/usr/local/$ant",
  } ->

  file {'/usr/local/ant':
    ensure  => link,
    target  => "/usr/local/$ant",
  }
}
