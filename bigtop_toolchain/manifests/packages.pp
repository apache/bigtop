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
    /(?i:(centos|fedora|redhat|rocky))/: {
      $_pkgs = [
        "unzip",
        "rsync",
        "curl",
        "wget",
        "git",
        "make",
        "autoconf",
        "automake",
        "libtool",
        "gcc",
        "gcc-c++",
        "fuse",
        "fuse3",
        "fuse3-devel",
        "createrepo",
        "lzo-devel",
        "fuse-devel",
        "cppunit-devel",
        "openssl-devel",
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
        "libzstd-devel",
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
        "libffi-devel",
        "krb5-devel",
        "net-tools",
        "perl-Digest-SHA",
        "nasm",
        "yasm",
        "python3-devel",
        "clang",
        "byacc",
        "binutils-devel",
        "libstdc++-static",
        "glibc-static",
        "gettext-devel"
      ]

      if ($operatingsystem == 'Fedora') {
        $pkgs = concat($_pkgs, ["libtirpc-devel", "cmake"])
      } else { # RedHat, CentOS, Rocky
        if (0 <= versioncmp($operatingsystemmajrelease, '9')) {
          $pkgs = concat($_pkgs, ["libtirpc-devel", "cmake"])
        } elsif (0 == versioncmp($operatingsystemmajrelease, '8')) {
          $pkgs = concat($_pkgs, ["libtirpc-devel", "cmake"])
        } elsif (0 == versioncmp($operatingsystemmajrelease, '7')) {
          $pkgs = concat($_pkgs, ["cmake3"])
        }
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
        "python3-devel",
        "python3-pip",
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
        "libzstd-devel",
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
        "libapr1-devel",
        "nasm",
        "yasm"
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
      "rpm-build",
      "system-rpm-config",
      "fuse-libs",
      "gmp-devel",
      "snappy-devel",
      "libzstd-devel",
      "bzip2-devel",
      "libffi-devel",
      "nasm",
      "yasm"
    ] }
    /openEuler/: { $pkgs = [
       "unzip",
       "cmake",
       "rsync",
       "curl",
       "wget",
       "git",
       "make",
       "autoconf",
       "automake",
       "libtool",
       "libtool-devel",
       "gcc",
       "gcc-c++",
       "gcc-gfortran",
       "doxygen",
       "createrepo_c-devel",
       "lzo-devel",
       "fuse-devel",
       "fuse3-devel",
       "cppunit-devel",
       "openssl-devel",
       "libxml2-devel",
       "libxslt-devel",
       "cyrus-sasl-devel",
       "sqlite-devel",
       "openldap-devel",
       "mariadb-devel",
       "rpm-build",
       "openEuler-rpm-config",
       "asciidoc",
       "xmlto",
       "libyaml-devel",
       "gmp-devel",
       "snappy-devel",
       "zstd-devel",
       "boost-devel",
       "xfsprogs-devel",
       "libuuid",
       "bzip2-devel",
       "readline-devel",
       "ncurses-devel",
       "libidn-devel",
       "libcurl-devel",
       "libevent-devel",
       "apr-devel",
       "bison-devel",
       "libffi-devel",
       "krb5-devel",
       "net-tools",
       "perl-Digest-SHA",
       "python3-devel",
       "libtirpc-devel",
       "libgit2-devel",
       "libgit2-glib-devel",
       "libxml2",
       "libpng-devel",
       "libtiff-devel",
       "libjpeg-turbo-devel",
       "leveldbjni",
       "psmisc",
       "nc",
       "initscripts",
       "openeuler-lsb",
       "pcre-devel",
       "texlive",
       "rpmdevtools",
       "nasm",
       "yasm"
    ] }
    /(Ubuntu|Debian)/: {
      $pkgs = [
        "unzip",
        "curl",
        "wget",
        "git-core",
        "gnupg2",
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
        "dh-python",
        "libfuse2",
        "libjansi-java",
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
        "libzstd-dev",
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
        "python-setuptools",
        "libffi-dev",
        "python3-dev",
        "nasm",
        "yasm"
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
      ensure => installed,
      notify => Package[$pkgs]
    }

    if $operatingsystemmajrelease == 7 {
      # https://access.redhat.com/errata/RHBA-2021:3649
      package { 'ca-certificates':
        ensure => latest
      }

      # BIGTOP-4076: newer g++ is required for rebuilding watcher used by flink-runtime-web
      if ($architecture in ['aarch64', 'ppc64le']) {
        package { 'centos-release-scl':
          ensure => latest
        }

        package { 'devtoolset-9-gcc-c++':
          ensure => latest,
          require => Package['centos-release-scl']
        }
      }
    }
    if $operatingsystemmajrelease !~ /^[0-7]$/ {
      # On CentOS 8, EPEL requires that the PowerTools repository is enabled.
      # See https://fedoraproject.org/wiki/EPEL#How_can_I_use_these_extra_packages.3F
      yumrepo { 'powertools':
        ensure  => 'present',
        enabled => '1'
      }
      Yumrepo<||> -> Package<||>

      # On CentOS 8, cmake-3.18.2-9.el8 does not work without updated libarchive.
      # https://bugs.centos.org/view.php?id=18212
      package { libarchive:
        ensure => latest
      }
    }
  }


  # BIGTOP-3364: Failed to install setuptools by pip/pip2
  # on Ubuntu-16.04/18.04 and centos-7.
  # From https://packaging.python.org/tutorials/installing-packages/#requirements-for-installing-packages,
  # it suggests to leverage python3/pip3 to install setuptools.
  #
  # "provider => 'pip3'" is not available for puppet 3.8.5,
  #  Workaround: Exec {pip3 install setuptools} directly insead of Package{}.
  package { 'python3-pip':
    ensure => installed
  }

  exec { "Setuptools Installation":
    command => "/usr/bin/pip3 install -q --upgrade setuptools",
  }

  exec { "flake8 and whell Installation":
    command => "/usr/bin/pip3 freeze --all; /usr/bin/pip3 --version; /usr/bin/pip3 install -q flake8 wheel",
  }

  if $osfamily == 'RedHat' {

    if ! (($operatingsystem == 'Fedora' and versioncmp($operatingsystemmajrelease, '31') >= 0) or
      ($operatingsystem != 'Fedora' and versioncmp($operatingsystemmajrelease, '8') >= 0)) {
      file { '/usr/bin/cmake':
        ensure => 'link',
        target => '/usr/bin/cmake3',
      }
    }

    # The rpm-build package had installed brp-python-bytecompile
    # just under /usr/lib/rpm until Fedora 34,
    # but it seems to have been removed in Fedora 35.
    # So we manually create a symlink instead.
    if ($operatingsystem == 'Fedora' and versioncmp($operatingsystemmajrelease, '35') >= 0) {
      file { '/usr/lib/rpm/brp-python-bytecompile':
        ensure => 'link',
        target => '/usr/lib/rpm/redhat/brp-python-bytecompile',
      }
    }
  }

}
