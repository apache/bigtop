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

class gridgain-hadoop {
  define server() {
    package { "gridgain-hadoop":
      ensure => latest,
    }

    package { "gridgain-hadoop-service":
      ensure => latest,
    }

    file { "/etc/default/gridgain-hadoop":
      content => template("gridgain-hadoop/gridgain-hadoop"),
      require => Package["gridgain-hadoop"],
    }

    file { "/etc/hadoop/gridgain.client.conf":
      ensure  => directory,
      owner   => 'root',
      group   => 'root',
      mode    => '0755',
      require => Package["gridgain-hadoop-service"],
    }
    file { "/etc/hadoop/gridgain.client.conf/core-site.xml":
        content => template('gridgain-hadoop/core-site.xml'),
        require => [File["/etc/hadoop/gridgain.client.conf"]],
    }
    file {
      "/etc/hadoop/gridgain.client.conf/mapred-site.xml":
        content => template('gridgain-hadoop/mapred-site.xml'),
        require => [File["/etc/hadoop/gridgain.client.conf"]],
    }
## let's make sure that gridgain-hadoop libs are linked properly
    file {'/usr/lib/hadoop/lib/gridgain-core.jar':
      ensure  => link,
      target  => '/usr/lib/gridgain-hadoop/libs/gridgain-core.jar',
      require => [Package["gridgain-hadoop-service"]],
    }
    file {'/usr/lib/hadoop/lib/gridgain-hadoop.jar':
      ensure  => link,
      target  => '/usr/lib/gridgain-hadoop/libs/gridgain-hadoop/gridgain-hadoop.jar',
      require => [Package["gridgain-hadoop-service"]],
    }

    service { "gridgain-hadoop":
      ensure  => running,
      require => [ Package["gridgain-hadoop", "gridgain-hadoop-service"], File["/etc/default/gridgain-hadoop"] ],
    }
  }
}