class hadoop::common_mapred_app (
    $mapreduce_cluster_acls_enabled = undef,
    $mapreduce_jobhistory_host = undef,
    $mapreduce_jobhistory_port = "10020",
    $mapreduce_jobhistory_webapp_port = "19888",
    $mapreduce_framework_name = "yarn",
    $mapred_data_dirs = suffix($hadoop::hadoop_storage_dirs, "/mapred"),
    $mapreduce_cluster_temp_dir = "/mapred/system",
    $yarn_app_mapreduce_am_staging_dir = "/user",
    $mapreduce_task_io_sort_factor = 64,              # 10 default
    $mapreduce_task_io_sort_mb = 256,                 # 100 default
    $mapreduce_reduce_shuffle_parallelcopies = undef, # 5 is default
    # processorcount == facter fact
    $mapreduce_tasktracker_map_tasks_maximum = inline_template("<%= [1, @processorcount.to_i * 0.20].max.round %>"),
    $mapreduce_tasktracker_reduce_tasks_maximum = inline_template("<%= [1, @processorcount.to_i * 0.20].max.round %>"),
    $mapreduce_tasktracker_http_threads = 60,         # 40 default
    $mapreduce_output_fileoutputformat_compress_type = "BLOCK", # "RECORD" default
    $mapreduce_map_output_compress = undef,
    $mapreduce_job_reduce_slowstart_completedmaps = undef,
    $mapreduce_map_memory_mb = undef,
    $mapreduce_reduce_memory_mb = undef,
    $mapreduce_map_java_opts = "-Xmx1024m",
    $mapreduce_reduce_java_opts = "-Xmx1024m",
    $hadoop_security_authentication = $hadoop::hadoop_security_authentication,
    $kerberos_realm = $hadoop::kerberos_realm,
) inherits hadoop {
  include hadoop::common_hdfs
  package { "hadoop-mapreduce":
    ensure => latest,
    require => [Package["jdk"], Package["hadoop"]],
  }
  if ($hadoop_security_authentication == "kerberos") {
    require kerberos::client
    kerberos::host_keytab { "mapred":
      tag    => "mapreduce",
      spnego => true,
      # we don't actually need this package as long as we don't put the
      # keytab in a directory managed by it. But it creates user yarn whom we
      # wan't to give the keytab to.
      require => Package["hadoop-mapreduce"],
    }
  }
  file {
    "/etc/hadoop/conf/mapred-site.xml":
      content => template('hadoop/mapred-site.xml'),
      require => [Package["hadoop"]],
  }
  file { "/etc/hadoop/conf/taskcontroller.cfg":
    content => template('hadoop/taskcontroller.cfg'), 
    require => [Package["hadoop"]],
  }
}
