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

class hcatalog {

  class deploy ($roles) {
    if ("hcatalog-server" in $roles) {
      include hcatalog::server
    }

    if ("webhcat-server" in $roles) {
      include hcatalog::webhcat::server
    }
  }

  class server($port = "9083", $kerberos_realm = "") {
    package { "hcatalog-server":
      ensure => latest,
    }

    file { "/etc/default/hcatalog-server":
      content => template("hcatalog/hcatalog-server"),
      require => Package["hcatalog-server"],
    }

    service { "hcatalog-server":
      ensure => running,
      require => [ Package["hcatalog-server"], File["/etc/default/hcatalog-server"] ],
      hasrestart => true,
      hasstatus => true,
    } 
  }

  class webhcat {
    class server($port = "50111", $kerberos_realm = "") {
      package { "webhcat-server":
        ensure => latest,
      }
  
      file { "/etc/webhcat/conf/webhcat.xml":
        content => template("hcatalog/webhcat.xml"),
        require => Package["webhcat-server"],
      }
  
      service { "webhcat-server":
        ensure => running,
        require => [ Package["webhcat-server"], File["/etc/webhcat/conf/webhcat.xml"] ],
        hasrestart => true,
        hasstatus => true,
      } 
    }
  }
}
