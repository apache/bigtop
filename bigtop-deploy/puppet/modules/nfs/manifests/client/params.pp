# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class nfs::client::params {
  case "$::operatingsystem $::operatingsystemrelease" {
    /(CentOS|RedHat) 6/: {
      $package_names   = [ "rpcbind", "nfs-utils" ]
      $portmap_service = "rpcbind"
      $idmapd_service  = "rpcidmapd"
      $nfs_version     = 4
    }

    /(CentOS|RedHat) 5/: {
      $package_names   = [ "portmap", "nfs-utils" ]
      $portmap_service = "portmap"
      $nfs_version     = 3
    }

    /SLES 11/: {
      $package_names   = [ "rpcbind", "nfs-client" ]
      $portmap_service = "rpcbind"
      $nfs_version     = 3
    }

    /(Debian|Ubuntu)/: {
      $package_names   = [ "nfs-common", "portmap" ]
      $portmap_service = "portmap"
      $nfs_version     = 3
    }

    default: {
      fail("nfs::client::params not supported on $::operatingsystem $::operatingsystemerelease")
    }
  }
}
