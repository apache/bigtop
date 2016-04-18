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

class flink {

  class deploy ($roles) {
   
   if ('flink-master' in $roles) {
      include flink::master
    }
    if ('flink-worker' in $roles) {
      include flink::worker
    }
   
  }

 class master {
    include common   

    #package { "flink-master":
     # ensure => latest,
    #}

    service { "flink-master":
      ensure     => running,
      require  => [
        Package["flink"],
        File["/etc/flink/conf/flink-env.sh"],
	File["/etc/flink/conf/flink-conf.yaml"],
	File["/etc/flink/conf/masters"]
	],
      hasrestart => true,
      hasstatus  => true,
    }
  }

  class worker {
    include common

    #package { "flink-worker":
     # ensure => latest,
    #}

    service { "flink-worker":
      ensure     => running,
      require  => [
        Package["flink"],
        File["/etc/flink/conf/flink-env.sh"],
        File["/etc/flink/conf/flink-conf.yaml"],
 	File["/etc/flink/conf/masters"]
      ],
      hasrestart => true,
      hasstatus  => true,
    }
  }



  class common(
      $master_host = $fqdn,
      $master_port = 6123,
      $master_ui_port = 8081,
     )
  {
      package { "flink":
      ensure => latest,
    }
  

  file { '/etc/flink/conf/flink-conf.yaml':
	ensure => present,
	content => template('flink/flink-conf.yaml'),
	require => Package['flink'],
	mode => 755,
  }

  file { '/etc/flink/conf/flink-env.sh':
	ensure => present,
	content => template('flink/flink-env.sh'),
	require => Package['flink'],
	mode => 755,
  }
  file { '/etc/flink/conf/masters':
	ensure => present,
	content => template('flink/masters'),
	require => Package['flink'],
	mode => 755,
  }
}
}         
