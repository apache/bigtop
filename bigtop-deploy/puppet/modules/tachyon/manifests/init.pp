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
  class common {
    package { "tachyon":
      ensure => latest,
    }
  }

  define master($master_host, $master_port) {
    include common   

    if ( $fqdn == $master_host ) {
      service { "tachyon-master":
        ensure => running,
        require => [ Package["tachyon"] ],
        hasrestart => true,
        hasstatus => true,
      }
    }
  }

  define worker($master_host, $master_port) {
    include common

   if ( $fqdn == $master_host ) {
      # We want master to run first in all cases 
      Service["tachyon-master"] ~> Service["tachyon-worker"]
    }
    service { "tachyon-worker":
      ensure => running,
      hasrestart => true,
      hasstatus => true,
    } 
  }
}
