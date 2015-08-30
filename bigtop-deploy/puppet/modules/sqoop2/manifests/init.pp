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

class sqoop2 {

  class deploy ($roles) {
    if ("sqoop-client" in $roles) {
      include sqoop2::client
    }

    if ("sqoop-server" in $roles) {
      include sqoop2::server
    }
  }

  class client {
    package { "sqoop2-client":
      ensure => latest,
    } 
  }

  class server {
    package { "sqoop2-server":
      ensure => latest,
    } 

    service { "sqoop2-server":
      ensure => running,
      require => Package["sqoop2-server"],
      hasstatus => true,
      hasrestart => true,
    }
  }
}
