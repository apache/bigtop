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

class bigtop_toolchain::packages {
  case $operatingsystem{
    /(?i:(centos|fedora|redhat))/: {
      $_pkgs = [
        "unzip",
        "rsync",
        "curl",
        "wget",
        "git",
        "make",
        "cmake",
        "autoconf",
        "automake",
        "libtool",
        "gcc",
        "gcc-c++",
        "fuse",
        "createrepo",
        "lzo-devel",
        "fuse-devel",
        "cppunit-devel",
        "openssl-devel",
        "python2-pip",
        "libxml2-devel",
        "libxslt-devel",
        "cyrus-sasl-devel",
        "sqlite-devel",
        "openldap-devel",
        "mariadb-devel",
        "rpm-build",
        "redhat-rpm-config",
        "fuse-libs",
        "asciidoc",
        "xmlto",
        "libyaml-devel",
        "gmp-devel",
        "snappy-devel",
        "boost-devel",
        "xfsprogs-devel",
        "libuuid-devel",
        "bzip2-devel",
        "readline-devel",
        "ncurses-devel",
        "libidn-devel",
        "libcurl-devel",
        "libevent-devel",
        "apr-devel",
        "bison",
        "libffi-devel"
      ]
      if ($operatingsystem == 'Fedora' or $operatingsystemmajrelease !~ /^[0-7]$/) {
        $pkgs = concat($_pkgs, ["python2-devel", "libtirpc-devel"])
      } else {
        $pkgs = concat($_pkgs, "python-devel")
      }
    }
    /(?i:(SLES|opensuse))/: { $pkgs = [
        "unzip",
        "curl",
        "wget",
        "git",
        "make",
        "cmake",
        "autoconf",
        "automake",
        "libtool",
        "gcc",
        "gcc-c++",
        "fuse",
        "createrepo",
        "lzo-devel",
        "fuse-devel",
        "cppunit-devel",
        "rpm-devel",
        "rpm-build",
        "pkg-config",
        "gmp-devel",
        "python-devel",
        "python-pip",
        "libxml2-devel",
        "libxslt-devel",
        "cyrus-sasl-devel",
        "sqlite3-devel",
        "openldap2-devel",
        "libyaml-devel",
        "krb5-devel",
        "asciidoc",
        "xmlto",
        "libmysqlclient-devel",
        "snappy-devel",
        "boost-devel",
        "xfsprogs-devel",
        "libuuid-devel",
        "libbz2-devel",
        "libcurl-devel",
        "libevent-devel",
        "bison",
        "flex",
        "libffi48-devel",
        "texlive-latex-bin-bin",
        "libapr1",
        "libapr1-devel"
      ]
      # fix package dependencies: BIGTOP-2120 and BIGTOP-2152 and BIGTOP-2471
      exec { '/usr/bin/zypper -n install  --force-resolution krb5 libopenssl-devel libxml2-devel libxslt-devel boost-devel':
      } -> Package <| |>
    }
    /Amazon/: { $pkgs = [
      "unzip",
      "curl",
      "wget",
      "git",
      "make",
      "cmake",
      "autoconf",
      "automake",
      "libtool",
      "gcc",
      "gcc-c++",
      "fuse",
      "createrepo",
      "lzo-devel",
      "fuse-devel",
      "openssl-devel",
      "python27-pip",
      "rpm-build",
      "system-rpm-config",
      "fuse-libs",
      "gmp-devel",
      "snappy-devel",
      "bzip2-devel",
      "libffi-devel"
    ] }
    /(Ubuntu|Debian)/: {
      $pkgs = [
        "unzip",
        "curl",
        "wget",
        "git-core",
        "make",
        "cmake",
        "autoconf",
        "automake",
        "libtool",
        "gcc",
        "g++",
        "fuse",
        "reprepro",
        "rsync",
        "liblzo2-dev",
        "libfuse-dev",
        "libcppunit-dev",
        "libssl-dev",
        "libzip-dev",
        "sharutils",
        "pkg-config",
        "debhelper",
        "devscripts",
        "build-essential",
        "dh-make",
        "libfuse2",
        "libjansi-java",
        "python2.7-dev",
        "libxml2-dev",
        "libxslt1-dev",
        "zlib1g-dev",
        "libsqlite3-dev",
        "libldap2-dev",
        "libsasl2-dev",
        "libmariadbd-dev",
        "libkrb5-dev",
        "asciidoc",
        "libyaml-dev",
        "libgmp3-dev",
        "libsnappy-dev",
        "libboost-regex-dev",
        "xfslibs-dev",
        "libbz2-dev",
        "libreadline-dev",
        "zlib1g",
        "libapr1",
        "libapr1-dev",
        "libevent-dev",
        "libcurl4-gnutls-dev",
        "bison",
        "flex",
        "python-dev",
        "python-pip",
        "libffi-dev"
      ]
      file { '/etc/apt/apt.conf.d/01retries':
        content => 'Aquire::Retries "5";'
      } -> Package <| |>
    }
  }
  package { $pkgs:
    ensure => installed
  }

  # Some bigtop packages use `/usr/lib/rpm/redhat` tools
  # from `redhat-rpm-config` package that doesn't exist on AmazonLinux.
  if $operatingsystem == 'Amazon' {
    file { '/usr/lib/rpm/redhat':
      ensure => 'link',
      target => '/usr/lib/rpm/amazon',
    }
  }

  if $operatingsystem == 'CentOS' {
    package { 'epel-release':
      ensure => installed
    }
    # On CentOS 8, EPEL requires that the PowerTools repository is enabled.
    # See https://fedoraproject.org/wiki/EPEL#How_can_I_use_these_extra_packages.3F
    if $operatingsystemmajrelease !~ /^[0-7]$/ {
      yumrepo { 'PowerTools':
        ensure  => 'present',
        enabled => '1'
      }
      Yumrepo<||> -> Package<||>
    }
  }

  # Install Python packages using pip
  case $operatingsystem{
    /(?i:(centos|fedora|redhat))/: {
      $pip = 'python2-pip'
    } /(?i:(SLES|opensuse))/: { 
      $pip = 'python-pip'
    } /Amazon/: { 
      $pip = 'python27-pip'
    } /(Ubuntu|Debian)/: {
      $pip = 'python-pip'
    }
  }
  file { '/usr/bin/pip-python':
    ensure => 'link',
    target => '/usr/bin/pip2',
  }
  package { 'setuptools':
    ensure => 'latest',
    provider => 'pip',
    require => [ Package[$pip], File['/usr/bin/pip-python'] ]
  }
  package { ['flake8', 'wheel']:
    ensure => 'installed',
    provider => 'pip',
    require => [ Package[$pip], File['/usr/bin/pip-python'] ]
  }
}
