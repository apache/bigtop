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

class bigtop_toolchain::jdk17 {
  $jdk_version = '17.0.2'
  $jdk_url = "https://download.java.net/java/GA/jdk${jdk_version}/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-${jdk_version}_linux-x64_bin.tar.gz"
  $download_path = "/tmp/java${jdk_version}.tar.gz"
  $extract_dir = "/opt/jdk-${jdk_version}"

  exec { 'download_jdk':
    command => "/usr/bin/wget ${jdk_url} -O ${download_path}",
    creates => $download_path,
  }

  exec { 'extract_jdk':
    command => "/bin/tar -xzf ${download_path} -C /opt",
    creates => $extract_dir,
    require => Exec['download_jdk'],
  }
}
