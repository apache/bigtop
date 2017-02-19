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
  $ant =  'apache-ant-1.9.9'

  $apache_prefix = nearest_apache_mirror()

  exec {"/usr/bin/wget $apache_prefix/ant/binaries/$ant-bin.tar.gz":
    cwd     => "/usr/src",
    unless  => "/usr/bin/test -f /usr/src/$ant-bin.tar.gz",
  }

  exec {"/bin/tar xvzf /usr/src/$ant-bin.tar.gz":
    cwd         => '/usr/local',
    creates     => "/usr/local/$ant",
    require     => Exec["/usr/bin/wget $apache_prefix/ant/binaries/$ant-bin.tar.gz"],
  }

  file {'/usr/local/ant':
    ensure  => link,
    target  => "/usr/local/$ant",
    require => Exec["/bin/tar xvzf /usr/src/$ant-bin.tar.gz"],
  }
}
