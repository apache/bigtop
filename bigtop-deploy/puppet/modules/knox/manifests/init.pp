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

class knox {

  class deploy ($roles) {
    if ('knox-gateway' in $roles) {
      include knox::gateway
    }
  }

  class gateway(
      $port = "8443",
    ) {

    package { 'knox':
      ensure => latest,
    }

    file { '/etc/knox/conf/gateway-site.xml':
      content => template('knox/gateway-site.xml'),
      require => [ Package['knox'] ],
      owner   => 'knox',
      group   => 'knox',
    }

    file { '/etc/knox/conf/topologies/sandbox.xml':
      content => template('knox/sandbox.xml'),
      require => [ Package['knox'] ],
      owner   => 'knox',
      group   => 'knox',
    }

    service { 'knox-gateway':
      ensure     => running,
      subscribe  => [
          Package['knox'],
          File['/etc/knox/conf/gateway-site.xml'],
       ],
      hasrestart => true,
      hasstatus  => true,
    }

    if ($kerberos_realm and $kerberos_realm != "") {
      require kerberos::client

      kerberos::host_keytab { "knox":
        spnego  => true,
        require => Package["knox"],
        before  => Service["knox-gateway"],
      }
    }
  }
}
