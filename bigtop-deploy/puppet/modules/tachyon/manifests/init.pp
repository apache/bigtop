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
class tachyon {
  class common ($master_host){
    package { "tachyon":
      ensure => latest,
    }

    # add logging into /var/log/..
    file {
        "/etc/tachyon/log4j.properties":
        content => template("tachyon/log4j.properties"),
        require => [Package["tachyon"]]
    }

    # add tachyon-env.sh to point to tachyon master
    file { "/etc/tachyon/tachyon-env.sh":
        content => template("tachyon/tachyon-env.sh"),
        require => [Package["tachyon"]]
    }
  }

  class master {
    include common

   exec {
        "tachyon formatting":
           command => "/usr/lib/tachyon/bin/tachyon format",
           require => [ Package["tachyon"]]
    }

    if ( $fqdn == $tachyon::common::master_host ) {
      service { "tachyon-master":
        ensure => running,
        require => [ Package["tachyon"] ],
        hasrestart => true,
        hasstatus => true,
      }
    }

  }

  class worker {
    include common

   if ( $fqdn == $tachyon::common::master_host ) {
      notice("tachyon ---> master host")
      # We want master to run first in all cases
      Service["tachyon-master"] ~> Service["tachyon-worker"]
   }

    service { "tachyon-worker":
      ensure => running,
      require => [Package["tachyon"]],
      hasrestart => true,
      hasstatus => true,
    }
  }
}
