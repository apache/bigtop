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

class hue {
  define server($sqoop_url, $solr_url, $hbase_thrift_url,
                $webhdfs_url, $rm_host, $rm_port, $oozie_url, $rm_url, $rm_proxy_url, $history_server_url,
                $hue_host = "0.0.0.0", $hue_port = "8888", $default_fs = "hdfs://localhost:8020",
                $kerberos_realm = "") {
    if ($kerberos_realm) {
      require kerberos::client
      kerberos::host_keytab { "hue":
        spnego => false,
        require => Package["hue"],
      }
    }

    package { "hue":
      ensure => latest,
    }

    file { "/etc/hue/conf/hue.ini":
      content => template("hue/hue.ini"),
      require => Package["hue"],
    }

    service { "hue":
      ensure => running,
      require => [ Package["hue"], File["/etc/hue/conf/hue.ini"] ],
      subscribe => [Package["hue"], File["/etc/hue/conf/hue.ini"] ],
      hasrestart => true,
      hasstatus => true,
    } 
    Kerberos::Host_keytab <| title == "hue" |> -> Service["hue"]

  }
}
