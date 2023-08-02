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

class hadoop_oozie {

  class deploy ($roles) {
    if ("oozie-client" in $roles) {
      include hadoop_oozie::client
    }

    if ("oozie-server" in $roles) {
      include hadoop::init_hdfs
      include hadoop_oozie::server
      Class['Hadoop::Init_hdfs'] -> Class['Hadoop_oozie::Server']
      if ("mapred-app" in $roles) {
        Class['Hadoop::Mapred_app'] -> Class['Hadoop_oozie::Server']
      }
    }
  }

  class client {
    package { "oozie-client":
      ensure => latest,
    } 
  }

  class server($kerberos_realm = "") {
    if ($kerberos_realm and $kerberos_realm != "") {
      require kerberos::client
      kerberos::host_keytab { "oozie":
        spnego => true,
        require => Package["oozie"],
      }
    }

    package { "oozie":
      ensure => latest,
    }

    file { "/etc/oozie/conf/oozie-site.xml":
      content => template("hadoop_oozie/oozie-site.xml"),
      require => Package["oozie"],
    }

    if ($operatingsystem == "openEuler") {
      exec { "Oozie DB init":
        command => "/usr/sbin/usermod -G hadoop oozie && /etc/init.d/oozie init",
        cwd     => "/var/lib/oozie",
        creates => "/var/lib/oozie/derby.log",
        require => Package["oozie"],
        unless => "/etc/init.d/oozie status",
      }
    } else {
      exec { "Oozie DB init":
        command => "/etc/init.d/oozie init",
        cwd     => "/var/lib/oozie",
        creates => "/var/lib/oozie/derby.log",
        require => Package["oozie"],
        unless => "/etc/init.d/oozie status",
      }
    }

    service { "oozie":
      ensure => running,
      require => [ Package["oozie"], Exec["Oozie DB init"] ],
      hasrestart => true,
      hasstatus => true,
    } 
    Kerberos::Host_keytab <| title == "oozie" |> -> Service["oozie"]

  }
}
