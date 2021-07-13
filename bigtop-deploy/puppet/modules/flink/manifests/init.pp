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

  class common($jobmanager_host, $jobmanager_port, $jobmanager_memory, $taskmanager_memory, $taskmanager_number_of_taskslots, $parallelism_default, $jobmanager_failover_strategy, $rest_port) { 
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

    package { "flink-jobmanager":
      ensure => latest,
    }

    service { "flink-jobmanager":
      ensure => running,
      require => Package["flink-jobmanager"],
      subscribe => File["/etc/flink/conf/flink-conf.yaml"],
      hasrestart => true,
      hasstatus => true
    }

    Package<| title == 'hadoop-hdfs' |> -> Package['flink-jobmanager']
  }

  class taskmanager {
    include flink::common

    package { "flink-taskmanager":
      ensure => latest,
    }

    service { "flink-taskmanager":
      ensure => running,
      require => Package["flink-taskmanager"],
      subscribe => File["/etc/flink/conf/flink-conf.yaml"],
      hasrestart => true,
      hasstatus => true,
    }

    Package<| title == 'hadoop-hdfs' |> -> Package['flink-taskmanager']
  }
}
