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

class yumrepo {
  case $::operatingsystem {
    /(Rocky)/: {
      package { 'epel-release':
        ensure => installed,
      } 
      if versioncmp($operatingsystemmajrelease, '8') == 0 {
        # On Rocky 8, EPEL requires that the PowerTools repository is enabled.
        # See https://fedoraproject.org/wiki/EPEL#How_can_I_use_these_extra_packages.3F
        yumrepo { 'powertools':
          ensure  => 'present',
          enabled => '1'
        }
        yumrepo { 'devel':
          ensure  => 'present',
          enabled => '1'
        }
      }
      if versioncmp($operatingsystemmajrelease, '9') == 0 {
        # On Rocky 9, EPEL requires that the crb repository is enabled.
        yumrepo { 'crb':
          ensure  => 'present',
          enabled => '1'
        }
        yumrepo { 'devel':
          ensure  => 'present',
          enabled => '1'
        }
      }
    }
  }
}
