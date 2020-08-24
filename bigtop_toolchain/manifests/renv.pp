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
      $pkgs = [
        "r-base",
        "r-base-dev",
        "pandoc"
      ]
    }
  }
  package { $pkgs:
    ensure => installed,
    before => [Exec['install_r_packages']] 
  }

  # Install required R packages
  exec { 'install_r_packages':
    cwd     => "/usr/bin",
    command => "/usr/bin/R -e \"install.packages(c('devtools', 'evaluate', 'rmarkdown', 'knitr', 'roxygen2', 'testthat', 'e1071'), repos = 'http://cran.us.r-project.org')\"",
    timeout => 6000
  }
}
