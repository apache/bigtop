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

class spark {

  class deploy ($roles) {
    if ('spark-client' in $roles) {
      include spark::client
    }

    if ('spark-on-yarn' in $roles) {
      include spark::yarn
    }

    if ('spark-yarn-slave' in $roles) {
      include spark::yarn_slave
    }

    if ('spark-master' in $roles) {
      include spark::master
    }

    if ('spark-worker' in $roles) {
      include spark::worker
    }

    if ('spark-history-server' in $roles) {
      include spark::history_server
    }

    if ('spark-thriftserver' in $roles) {
      include spark::spark_thriftserver
    }
    
  }

  class spark_thriftserver {
    include spark::common

    package { 'spark-thriftserver': 
      ensure => latest,
    }

    service { 'spark-thriftserver':
      ensure     => running,
      subscribe  => [
        Package['spark-thriftserver'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus  => true,
    }

    Exec<| title == "init hdfs" |> -> Service["spark-thriftserver"]
  }

  class client {
    include spark::common
    include spark::sparkr

    package { 'spark-python':
      ensure  => latest,
      require => Package['spark-core'],
    }

    package { 'spark-external':
      ensure  => latest,
      require => Package['spark-core'],
    }
  }

  class master {
    include spark::common

    package { "spark-master":
      ensure => latest,
    }

    service { 'spark-master':
      ensure     => running,
      subscribe  => [
        Package['spark-master'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus  => true,
    }
  }

  class worker {
    include spark::common

    package { "spark-worker":
      ensure => latest,
    }

    service { 'spark-worker':
      ensure     => running,
      subscribe  => [
        Package['spark-worker'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus  => true,
    }
  }

  class history_server {
    include spark::common

    package { 'spark-history-server':
      ensure => latest,
    }

    service { 'spark-history-server':
      ensure     => running,
      subscribe  => [
        Package['spark-history-server'],
        File['/etc/spark/conf/spark-env.sh'],
        File['/etc/spark/conf/spark-defaults.conf'],
      ],
      hasrestart => true,
      hasstatus => true,
    }
  }

  class yarn {
    include spark::common
    include spark::datanucleus
  }

  class yarn_slave {
    include spark::yarn_shuffle
    include spark::datanucleus
  }

  class yarn_shuffle {
    package { 'spark-yarn-shuffle':
      ensure => latest,
    }
  }

  class datanucleus {
    package { 'spark-datanucleus':
      ensure => latest,
    }
  }

  class sparkr {
    # BIGTOP-3579. On these distros, the default version of R is earlier than 3.5.0,
    # which is required to run SparkR. So the newer version of R is installed here.
    if (($operatingsystem == 'Ubuntu' and versioncmp($operatingsystemmajrelease, '18.04') <= 0) or
        ($operatingsystem == 'Debian' and versioncmp($operatingsystemmajrelease, '10') < 0)) {
      $url = "http://cran.r-project.org/src/base/R-3/"
      $rfile = "R-3.6.3.tar.gz"
      $rdir = "R-3.6.3"

      $pkgs = [
        "g++",
        "gcc",
        "gfortran",
        "libbz2-dev",
        "libcurl4-gnutls-dev",
        "liblzma-dev",
        "libpcre3-dev",
        "libreadline-dev",
        "libz-dev",
        "make",
      ]
      package { $pkgs:
        ensure => installed,
        before => [Exec["install_R"]],
      }

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
        before  => [Package["spark-sparkr"]],
        timeout => 3000
      }
    }

    package { 'spark-sparkr':
      ensure => latest,
    }
  }

  class common(
      $spark_thrift_server_without_hive = true,
      $spark_hadoop_javax_jdo_option_ConnectionURL = undef,
      $spark_hadoop_javax_jdo_option_ConnectionDriverName = undef,
      $spark_hive_server2_thrift_port = undef,
      $spark_sql_warehouse_dir = undef,
      $master_url = undef,
      $master_host = $fqdn,
      $zookeeper_connection_string = undef,
      $master_port = 7077,
      $worker_port = 7078,
      $master_ui_port = 8080,
      $worker_ui_port = 8081,
      $history_ui_port = 18080,
      $use_yarn_shuffle_service = false,
      $event_log_dir =  "hdfs:///var/log/spark/apps",
      $history_log_dir = "hdfs:///var/log/spark/apps",
      $extra_lib_dirs = "/usr/lib/hadoop/lib/native",
      $driver_mem = "1g",
      $executor_mem = "1g",
  ) {

### This is an ungodly hack to deal with the consequence of adding
### unconditional hive-support into Spark
### The addition is tracked by BIGTOP-2154
### The real fix will come in BIGTOP-2268
    include spark::datanucleus

    package { 'spark-core':
      ensure => latest,
    }

    if $zookeeper_connection_string == undef {
      $spark_daemon_java_opts = "\"-Dspark.deploy.recoveryMode=NONE\""
    } else {
      $spark_daemon_java_opts = "\"-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=${zookeeper_connection_string}\""
    }

    file { '/etc/spark/conf/spark-env.sh':
      content => template('spark/spark-env.sh'),
      require => Package['spark-core'],
    }

    file { '/etc/spark/conf/spark-defaults.conf':
      content => template('spark/spark-defaults.conf'),
      require => Package['spark-core'],
    }

    file { '/etc/spark/conf/log4j2.properties':
      source  => '/etc/spark/conf/log4j2.properties.template',
      require => Package['spark-core'],
    }
  }
}
