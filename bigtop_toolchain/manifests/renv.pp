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

class bigtop_toolchain::renv {

  require bigtop_toolchain::packages

  case $operatingsystem{
    /(?i:(centos|fedora|redhat|Amazon))/: {
      $pkgs = [
        "R",
        "R-devel",
        "pandoc"
      ]
    }
    /(?i:(SLES|opensuse))/: { 
      $pkgs = [
        "R-base",
        "R-base-devel",
        "pandoc"
      ]
    }
    /(Ubuntu|Debian)/: {
      if (versioncmp($operatingsystemmajrelease, '18.04') <= 0) {
        $pkgs = [
          "r-base-dev",
          "libcairo2-dev",
          "pandoc",
          "pandoc-citeproc",
        ]
      } else {
        $pkgs = [
          "r-base",
          "r-base-dev",
          "pandoc",
        ]
      }
    }
  }

  package { $pkgs:
    ensure => installed,
    before => [Exec["install_r_packages"]]
  }

  # BIGTOP-3483:
  #   Upgrade R version to 3.6.3 to build Spark 3.0.1 on Ubuntu 16.04 and 18.04
  #
  # Then Install required R packages dependency
  if ($operatingsystem == 'Ubuntu' and versioncmp($operatingsystemmajrelease, '18.04') <= 0) {
    $url = "http://cran.r-project.org/src/base/R-3/"
    $rfile = "R-3.6.3.tar.gz"
    $rdir = "R-3.6.3"

    exec { "download_R":
      cwd  => "/usr/src",
      command => "/usr/bin/wget $url/$rfile && mkdir -p $rdir && /bin/tar -xvzf $rfile -C $rdir --strip-components=1 && cd $rdir",
      creates => "/usr/src/$rdir",
    }
    exec { "install_R":
      cwd => "/usr/src/$rdir",
      command => "/usr/src/$rdir/configure --with-recommended-packages=yes --without-x --with-cairo --with-libpng --with-libtiff --with-jpeglib --with-tcltk --with-blas --with-lapack --enable-R-shlib --prefix=/usr/local && /usr/bin/make && /usr/bin/make install && /sbin/ldconfig",
      creates => "/usr/local/bin/R",
      require => [Exec["download_R"]],
      timeout => 3000
    }

    exec { "install_r_packages" :
      cwd     => "/usr/local/bin",
      command => "/usr/local/bin/R -e \"install.packages(c('devtools', 'evaluate', 'rmarkdown', 'knitr', 'roxygen2', 'testthat', 'e1071'), repos = 'http://cran.r-project.org/')\"",
      require => [Exec["install_R"]],
      timeout => 6000
    }
  } else {
    exec { "install_r_packages" :
      cwd     => "/usr/bin",
      command => "/usr/bin/R -e \"install.packages(c('devtools', 'evaluate', 'rmarkdown', 'knitr', 'roxygen2', 'testthat', 'e1071'), repos = 'http://cran.r-project.org/')\"",
      timeout => 6000
    }
  }
}
