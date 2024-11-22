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

  class common ($master_host,
      $alluxio_underfs_address = hiera('bigtop::hadoop_namenode_uri'),
  ) {
    package { "alluxio":
      ensure => latest,
    }

    exec { "daemon-reload":
      path => ["/bin", "/usr/bin"],
      command => "systemctl daemon-reload",
      require => [ Package["alluxio"] ]
    }

    file { "/etc/alluxio/conf/alluxio-site.properties":
        content => template("alluxio/alluxio-site.properties"),
        require => [Package["alluxio"]]
    }
  }

  class master {
    include alluxio::common

    exec {
        "alluxio formatting":
           command => "/usr/lib/alluxio/bin/alluxio format",
           require => [ Package["alluxio"], File["/etc/alluxio/conf/alluxio-site.properties"] ]
    }

    if ( $fqdn == $alluxio::common::master_host ) {
      service { "alluxio-master":
        ensure => running,
        require => [ Package["alluxio"], Exec["daemon-reload"], Exec["alluxio formatting"] ],
        subscribe => File["/etc/alluxio/conf/alluxio-site.properties"],
        hasrestart => true,
        hasstatus => true,
      }
      service { "alluxio-job-master":
        ensure => running,
        require => [ Package["alluxio"], Exec["daemon-reload"], Exec["alluxio formatting"] ],
        subscribe => File["/etc/alluxio/conf/alluxio-site.properties"],
        hasrestart => true,
        hasstatus => true,
      }
      Exec<| title == "init hdfs" |> -> Service["alluxio-master"]
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
      require => [ Package["alluxio"], Exec["daemon-reload"], File["/etc/alluxio/conf/alluxio-site.properties"] ],
      subscribe => File["/etc/alluxio/conf/alluxio-site.properties"],
      hasrestart => true,
      hasstatus => true,
    }
    service { "alluxio-job-worker":
      ensure => running,
      require => [ Package["alluxio"], Exec["daemon-reload"], File["/etc/alluxio/conf/alluxio-site.properties"] ],
      subscribe => File["/etc/alluxio/conf/alluxio-site.properties"],
      hasrestart => true,
      hasstatus => true,
    }
  }
}
