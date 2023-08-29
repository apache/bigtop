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

class bigtop_toolchain::groovy {

  require bigtop_toolchain::packages

  $groovy_version = '2.5.4'
  $groovy = "apache-groovy-binary-${groovy_version}"

  exec { 'Download Groovy':
    command => "/usr/bin/wget https://dl.bintray.com/groovy/maven/${groovy}.zip",
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/${groovy}.zip",
  } ~>

  exec {'Extract Groovy':
    command => "/usr/bin/unzip -x -o /usr/src/${groovy}.zip",
    cwd     => '/usr/local',
  } ->

  file {'/usr/local/groovy':
    ensure  => link,
    target  => "/usr/local/groovy-${groovy_version}",
  }
}
