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

class bigtop_toolchain::maven {

  require bigtop_toolchain::gnupg
  require bigtop_toolchain::packages

  $mvnversion = latest_maven_binary("3.6.[0-9]*")
  $mvn = "apache-maven-$mvnversion"

  $apache_prefix = nearest_apache_mirror()

  exec { 'Download Maven binaries':
    command => "/usr/bin/wget $apache_prefix/maven/maven-3/$mvnversion/binaries/$mvn-bin.tar.gz",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/$mvn-bin.tar.gz",
  } ~>

  exec { 'Download Maven binaries signature':
    command => "/usr/bin/wget https://www.apache.org/dist/maven/maven-3/$mvnversion/binaries/$mvn-bin.tar.gz.asc",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/$mvn-bin.tar.gz.asc",
  } ~>

  exec { 'Verify Maven binaries signature':
    command => "/usr/bin/$bigtop_toolchain::gnupg::cmd --no-tty -v --verify --auto-key-retrieve --keyserver hkp://keyserver.ubuntu.com:80 $mvn-bin.tar.gz.asc",
    cwd     => "/usr/src",
  } ->

  exec { 'Extract Maven binaries':
    command => "/bin/tar xvzf /usr/src/$mvn-bin.tar.gz",
    cwd     => '/usr/local',
    creates => "/usr/local/$mvn",
  } ->

  file {'/usr/local/maven':
    ensure  => link,
    target  => "/usr/local/$mvn",
  }
}
