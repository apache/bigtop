class hadoop::mapred_app {
  include hadoop::common_mapred_app
  hadoop::create_storage_dir { $hadoop::common_mapred_app::mapred_data_dirs: } ->
  file { $hadoop::common_mapred_app::mapred_data_dirs:
    ensure => directory,
    owner => yarn,
    group => yarn,
    mode => '755',
    require => [Package["hadoop-mapreduce"]],
  }
}
