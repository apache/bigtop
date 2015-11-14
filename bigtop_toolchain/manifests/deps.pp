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


class bigtop_toolchain::deps {

  $apache_prefix = nearest_apache_mirror()
  include bigtop_toolchain::packages
  exec {"/usr/bin/wget $apache_prefix/ant/binaries/apache-ant-1.9.6-bin.tar.gz":
    cwd     => "/usr/src",
    require => Package[$packages::pkgs],
    unless  => "/usr/bin/test -f /usr/src/apache-ant-1.9.6-bin.tar.gz",
  }

  exec {"/usr/bin/wget $apache_prefix/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz":
    cwd     => "/usr/src",
    require => Package[$packages::pkgs],
    unless  => "/usr/bin/test -f /usr/src/apache-maven-3.3.3-bin.tar.gz",
  }
}
