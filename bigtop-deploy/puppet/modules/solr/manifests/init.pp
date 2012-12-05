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

class solr {
  define solrcloud_config($confdir, $zk) {
    exec { "ZK $title config upload":
      command => "/bin/bash -c \"java -classpath '/usr/lib/solr/server/webapps/solr/WEB-INF/lib/*' org.apache.solr.cloud.ZkCLI -cmd makepath /solr -zkhost ${zk} ; java -classpath '/usr/lib/solr/server/webapps/solr/WEB-INF/lib/*' org.apache.solr.cloud.ZkCLI -cmd upconfig  -confdir ${confdir}/${title}/conf -confname $title -zkhost ${zk}/solr\"",
      logoutput => true,
    }
  }
  

  define server($collections = ["solrcloud"], $port = "1978", $port_admin = "1979", $zk = "localhost:2181") {
    package { "solr-server":
      ensure => latest,
    }

    file {
      "/etc/default/solr":
        content => template("solr/solr"),
        require => [Package["solr-server"]],
    }

    file {
      "/etc/solr/conf/solr.xml":
        content => template("solr/solr.xml"),
        require => [Package["solr-server"]],
    }

    # FIXME: perhap we have to provide a way to manage collection configs
    solrcloud_config { $collections:
      zk      => $zk,
      confdir => "/etc/solr/conf",
      require => [Package["solr-server"]],
    }

    service { "solr-server":
      ensure => running,
      require => [ Package["solr-server"], File["/etc/default/solr"], File["/etc/solr/conf/solr.xml"], Solrcloud_config[$collections] ],
      subscribe => [Package["solr-server"], File["/etc/default/solr"], File["/etc/solr/conf/solr.xml"] ],
      hasrestart => true,
      hasstatus => true,
    } 
  }
}
