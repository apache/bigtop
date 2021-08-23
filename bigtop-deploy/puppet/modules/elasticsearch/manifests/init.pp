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
class elasticsearch {

  class deploy ($roles) {
    if ("elasticsearch-server" in $roles) {
      include elasticsearch::server
    }
  }

  class server() {
    # make sure elasticsearch is installed
    package { "elasticsearch":
      ensure => latest
    }
    # read nodes in this cluster
    $elasticsearch_cluster_nodes = hiera('hadoop_cluster_node::cluster_nodes')
    # minimum number of eligible master nodes, usually calced with N/2+1
    $elasticsearch_cluster_min_master = size($elasticsearch_cluster_nodes)/2 + 1
    # check if system_call_filter is available during bootstrap
    $elasticsearch_bootstrap_system_call_filter = hiera('elasticsearch::bootstrap::system_call_filter')

    # set values in elasticsearch.yaml
    file { "/etc/elasticsearch/conf/elasticsearch.yml":
        content => template("elasticsearch/elasticsearch.yml"),
        require => Package["elasticsearch"]
    }

    exec { "daemon-reload":
      path => ["/bin", "/usr/bin"],
      command => "systemctl daemon-reload",
      require => [ Package["elasticsearch"] ]
    }

    service { "elasticsearch":
      ensure => running,
      require => [ Package["elasticsearch"], Exec["daemon-reload"] ],
      subscribe => File["/etc/elasticsearch/conf/elasticsearch.yml"],
      hasrestart => true,
      hasstatus => true
    }

  }
}
