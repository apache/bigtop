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


class bigtop_toolchain::libhdfs {

  include bigtop_toolchain::packages

  case $operatingsystem {
    /Ubuntu|Debian/: {
      $hackrepourl = "https://bintray.com/artifact/download/wangzw/deb/dists/trusty/contrib/binary-amd64"
      $libhdfs = "libhdfs3_2.2.31-1_amd64.deb"
      $libhdfs_dev = "libhdfs3-dev_2.2.31-1_amd64.deb"

      $bisonurl_dev = "http://launchpadlibrarian.net/140087283"
      $bisonurl     = "http://launchpadlibrarian.net/140087282"
      $bison = "bison_2.7.1.dfsg-1_amd64.deb"
      $bison_dev = "libbison-dev_2.7.1.dfsg-1_amd64.deb"

      exec {"install":
        path    => "/usr/bin:/bin:/usr/sbin:/sbin",
        cwd     => "/usr/src",
        command => "dpkg --install $libhdfs $libhdfs_dev $bison $bison_dev",
        require => [ Exec[ 'download', 'download-bison' ], Package[ $packages::pkgs ] ],
      }

      exec {"download-bison":
        path    => "/usr/bin",
        cwd     => "/usr/src",
        command => "curl -L $bisonurl/$bison -o $bison ; curl -L $bisonurl_dev/$bison_dev -o $bison_dev",
        creates => "/usr/src/$bison, /usr/src/$bison_dev",
      }
    }

    /(?i:(centos|fedora|amazon))/: {

      $apache_prefix = nearest_apache_mirror()
      $hackrepourl = "https://bintray.com/artifact/download/wangzw/rpm/centos7/x86_64"
      $libhdfs = "libhdfs3-2.2.31-1.el7.centos.x86_64.rpm"
      $libhdfs_dev = "libhdfs3-devel-2.2.31-1.el7.centos.x86_64.rpm"


      exec {"install":
        path    => "/usr/bin",
        cwd     => "/usr/src",
        command => "yum install -y $libhdfs $libhdfs_dev",
        require => [ Exec[ 'download' ], Package[ $packages::pkgs ] ],
      }
    }
  }

  exec {"download":
    path    => "/usr/bin",
    cwd     => "/usr/src",
    command => "curl -L $hackrepourl/$libhdfs -o $libhdfs ; curl -L $hackrepourl/$libhdfs_dev -o $libhdfs_dev",
    creates => "/usr/src/$libhdfs, /usr/src/$libhdfs_dev",
  }
}
