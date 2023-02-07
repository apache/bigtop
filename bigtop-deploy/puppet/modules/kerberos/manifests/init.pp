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

class kerberos {

  class deploy ($roles) {
    if ("kerberos-server" in $roles) {
      include kerberos::server
      include kerberos::kdc
      include kerberos::kdc::admin_server
    }
  }

  class krb_site ($domain = inline_template('<%= @domain %>'),
      $realm = inline_template('<%= @domain.upcase %>'),
      $kdc_server = 'localhost',
      $kdc_port = '88',
      $admin_port = 749,
      $keytab_export_dir = "/var/lib/bigtop_keytabs") {

    case $operatingsystem {
        'ubuntu','debian': {
            $package_name_kdc    = 'krb5-kdc'
            $service_name_kdc    = 'krb5-kdc'
            $package_name_admin  = 'krb5-admin-server'
            $service_name_admin  = 'krb5-admin-server'
            $package_name_client = 'krb5-user'
            $exec_path           = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'
            $kdc_etc_path        = '/etc/krb5kdc'
            $kdc_db_path         = '/var/lib/krb5kdc'
        }
        # default assumes CentOS, Redhat 5 series (just look at how random it all looks :-()
        default: {
            $package_name_kdc    = 'krb5-server'
            $service_name_kdc    = 'krb5kdc'
            $package_name_admin  = 'krb5-libs'
            $service_name_admin  = 'kadmin'
            $package_name_client = 'krb5-workstation'
            $exec_path           = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/kerberos/sbin:/usr/kerberos/bin'
            $kdc_etc_path        = '/var/kerberos/krb5kdc'
            $kdc_db_path         = '/var/kerberos/krb5kdc'
        }
    }

    file { "/etc/krb5.conf":
      content => template('kerberos/krb5.conf'),
      owner => "root",
      group => "root",
      mode => "0644",
    }

    @file { $keytab_export_dir:
      ensure => directory,
      owner  => "root",
      group  => "root",
    }

    # Required for SPNEGO
    @kerberos::principal { "HTTP":

    }
  }

  class kdc inherits kerberos::krb_site {
    Class['kerberos::kdc'] -> Class['hadoop_cluster_node']

    package { $package_name_kdc:
      ensure => installed,
    }

    file { $kdc_etc_path:
    	ensure => directory,
        owner => root,
        group => root,
        mode => "0700",
        require => Package["$package_name_kdc"],
    }
    file { "${kdc_etc_path}/kdc.conf":
      content => template('kerberos/kdc.conf'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }
    file { "${kdc_etc_path}/kadm5.acl":
      content => template('kerberos/kadm5.acl'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }

    exec { "kdb5_util":
      path => $exec_path,
      command => "rm -f /etc/kadm5.keytab ; kdb5_util -P cthulhu -r ${realm} create -s && kadmin.local -q 'cpw -pw secure kadmin/admin'",
      
      creates => "${kdc_etc_path}/stash",

      subscribe => File["${kdc_etc_path}/kdc.conf"],
      # refreshonly => true, 

      require => [Package["$package_name_kdc"], Package["$package_name_admin"], File["${kdc_etc_path}/kdc.conf"], File["/etc/krb5.conf"]],
    }

    service { $service_name_kdc:
      ensure => running,
      require => [Package["$package_name_kdc"], File["${kdc_etc_path}/kdc.conf"], Exec["kdb5_util"]],
      subscribe => [File["${kdc_etc_path}/kadm5.acl"], File["${kdc_etc_path}/kdc.conf"]],
      hasrestart => true,
    }


    class admin_server inherits kerberos::kdc {

      package { "$package_name_admin":
        ensure => installed,
        require => Package["$package_name_kdc"],
      }

      exec { '/usr/bin/setsebool -P kadmind_disable_trans 1':
        onlyif => '/usr/bin/test -f /usr/bin/setsebook'
      } ->
      exec { '/usr/bin/setsebool -P krb5kdc_disable_trans 1':
        onlyif => '/usr/bin/test -f /usr/bin/setsebook'
      } ->
      service { "$service_name_admin":
        ensure => running,
        require => [Package["$package_name_admin"], Service["$service_name_kdc"]],
        subscribe => [File["${kdc_etc_path}/kadm5.acl"], File["${kdc_etc_path}/kdc.conf"]],
        hasrestart => true,
      }
    }
  }

  class client inherits kerberos::krb_site {
    package { $package_name_client:
      ensure => installed,
    }
  }

  class server {
    include kerberos::client

    class { "kerberos::kdc": } 
    ->
    Class["kerberos::client"] 

    class { "kerberos::kdc::admin_server": }
    -> 
    Class["kerberos::client"]
  }

  define principal {
    require "kerberos::client"

    realize(File[$kerberos::krb_site::keytab_export_dir])

    $principal = "$title/$::fqdn"
    $keytab    = "$kerberos::krb_site::keytab_export_dir/$title.keytab"

    exec { "addprinc.$title":
      path => $kerberos::krb_site::exec_path,
      command => "kadmin -w secure -p kadmin/admin -q 'addprinc -randkey $principal'",
      unless => "kadmin -w secure -p kadmin/admin -q listprincs | grep -q $principal",
      require => Package[$kerberos::krb_site::package_name_client],
      tries => 180,
      try_sleep => 1,
    } 
    ->
    exec { "xst.$title":
      path    => $kerberos::krb_site::exec_path,
      command => "kadmin -w secure -p kadmin/admin -q 'xst -k $keytab $principal'",
      unless  => "klist -kt $keytab 2>/dev/null | grep -q $principal",
      require => File[$kerberos::krb_site::keytab_export_dir],
    }
  }

  define host_keytab($princs = [ $title ], $spnego = disabled,
    $owner = $title, $group = "", $mode = "0400",
  ) {
    $keytab = "/etc/$title.keytab"

    $internal_princs = $spnego ? {
      true      => [ 'HTTP' ],
      'enabled' => [ 'HTTP' ],
      default   => [ ],
    }
    realize(Kerberos::Principal[$internal_princs])

    $includes = inline_template("<%=
      [@princs, @internal_princs].flatten.map { |x|
        \"rkt $kerberos::krb_site::keytab_export_dir/#{x}.keytab\"
      }.join(\"\n\")
    %>")

    kerberos::principal { $princs:
    }

    exec { "ktinject.$title":
      path     => $kerberos::krb_site::exec_path,
      command  => "ktutil <<EOF
        $includes
        wkt $keytab
EOF
        chown ${owner}:${group} ${keytab}
        chmod ${mode} ${keytab}",
      creates => $keytab,
      require => [ Kerberos::Principal[$princs],
                   Kerberos::Principal[$internal_princs] ],
    }

    exec { "aquire $title keytab":
        path    => $kerberos::krb_site::exec_path,
        user    => $owner,
        command => "bash -c 'kinit -kt $keytab ${title}/$::fqdn ; kinit -R'",
        require => Exec["ktinject.$title"],
    }
  }
}
