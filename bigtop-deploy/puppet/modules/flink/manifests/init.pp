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
class flink {

  class deploy ($roles) {
    if ("flink-jobmanager" in $roles) {
      include flink::jobmanager
    }

    if ("flink-taskmanager" in $roles) {
      include flink::taskmanager
    }
  }

  class common($jobmanager_host, $jobmanager_port, $ui_port, $storage_dirs) {
    # make sure flink is installed
    package { "flink":
      ensure => latest
    }

    # set values in flink-conf.yaml
    file { "/etc/flink/conf/flink-conf.yaml":
        content => template("flink/flink-conf.yaml"),
        require => Package["flink"]
    }
  }

  class jobmanager {
    include flink::common

    service { "flink-jobmanager":
      ensure => running,
      require => Package["flink"],
      subscribe => File["/etc/flink/conf/flink-conf.yaml"],
      hasrestart => true,
      hasstatus => true
     
    }

  }

  class taskmanager {
    include flink::common

    service { "flink-taskmanager":
      ensure => running,
      require => Package["flink"],
      subscribe => File["/etc/flink/conf/flink-conf.yaml"],
      hasrestart => true,
      hasstatus => true,
    }
  }
}
