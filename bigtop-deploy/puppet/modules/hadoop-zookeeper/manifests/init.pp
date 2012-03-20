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

class hadoop-zookeeper {
  define client {
    package { "zookeeper":
      ensure => latest,
    } 
  }

  define server($myid, $ensemble = ["localhost:2888:3888"],
                $kerberos_realm = "") 
  {
    package { "zookeeper-server":
      ensure => latest,
    }

    service { "zookeeper-server":
      ensure => running,
      require => Package["zookeeper-server"],
      subscribe => File["/etc/zookeeper/conf/zoo.cfg"],
      hasrestart => true,
      hasstatus => true,
    } 

    file { "/etc/zookeeper/conf/zoo.cfg":
      content => template("hadoop-zookeeper/zoo.cfg"),
      require => Package["zookeeper-server"],
    }

    file { "/var/lib/zookeeper/myid":
      content => inline_template("<%= myid %>"),
      require => Package["zookeeper-server"],
    }

    if ($kerberos_realm) {
      require kerberos::client

      kerberos::host_keytab { "zookeeper":
        notify => Service["zookeeper-server"],
      }

      file { "/etc/zookeeper/conf/java.env":
        source  => "puppet:///modules/hadoop-zookeeper/java.env",
        require => Package["zookeeper-server"],
        notify  => Service["zookeeper-server"],
      }

      file { "/etc/zookeeper/conf/jaas.conf":
        content => template("hadoop-zookeeper/jaas.conf"),
        require => Package["zookeeper-server"],
        notify  => Service["zookeeper-server"],
      }
    }
  }
}
