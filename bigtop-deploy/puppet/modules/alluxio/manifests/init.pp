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
class alluxio {

  class deploy ($roles) {
    if ("alluxio-master" in $roles) {
      include alluxio::master
    }

    if ("alluxio-worker" in $roles) {
      include alluxio::worker
    }
  }

  class common ($master_host){
    package { "alluxio":
      ensure => latest,
    }

    # add logging into /var/log/..
    file {
        "/etc/alluxio/conf/log4j.properties":
        content => template("alluxio/log4j.properties"),
        require => [Package["alluxio"]]
    }

    # add alluxio-env.sh to point to alluxio master
    file { "/etc/alluxio/conf/alluxio-env.sh":
        content => template("alluxio/alluxio-env.sh"),
        require => [Package["alluxio"]]
    }
  }

  class master {
    include alluxio::common

   exec {
        "alluxio formatting":
           command => "/usr/lib/alluxio/bin/alluxio format",
           require => [ Package["alluxio"], File["/etc/alluxio/conf/log4j.properties"], File["/etc/alluxio/conf/alluxio-env.sh"] ]
    }

    if ( $fqdn == $alluxio::common::master_host ) {
      service { "alluxio-master":
        ensure => running,
        require => [ Package["alluxio"], Exec["alluxio formatting"] ],
        hasrestart => true,
        hasstatus => true,
      }
    }

  }

  class worker {
    include alluxio::common

   if ( $fqdn == $alluxio::common::master_host ) {
      notice("alluxio ---> master host")
      # We want master to run first in all cases
      Service["alluxio-master"] ~> Service["alluxio-worker"]
   }

    service { "alluxio-worker":
      ensure => running,
      require => [ Package["alluxio"], File["/etc/alluxio/conf/log4j.properties"], File["/etc/alluxio/conf/alluxio-env.sh"] ],
      hasrestart => true,
      hasstatus => true,
    }
  }
}
