specs {
  tests {
    'HADOOP_EJH1' {
      name = 'HADOOP_EJH1'
      type = 'envdir'
      arguments {
        variable = 'JAVA_HOME'
        pattern = /.*\/usr\/.*/
      }
    }
    'HADOOP_EC2' {
      name = 'HADOOP_EC2'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_COMMON_HOME'
        pattern = /.*\/usr\/lib\/hadoop.*/
      }
    }
    'HADOOP_EC3' {
      name = 'HADOOP_EC3'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_COMMON_DIR'
        relative = true
      }
    }
    'HADOOP_EC4' {
      name = 'HADOOP_EC4'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_COMMON_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EC5' {
      name = 'HADOOP_EC5'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_CONF_DIR'
        pattern = /.*\/etc\/hadoop.*/
      }
    }
    'HADOOP_EH1' {
      name = 'HADOOP_EH1'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_HDFS_HOME'
        pattern = /.*\/usr\/lib\/hadoop-hdfs.*/
      }
    }
    'HADOOP_EH2' {
      name = 'HADOOP_EH2'
      type = 'envdir'
      arguments {
        variable = 'HDFS_DIR'
        relative = true
      }
    }
    'HADOOP_EH3' {
      name = 'HADOOP_EH3'
      type = 'envdir'
      arguments {
        variable = 'HDFS_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EY1' {
      name = 'HADOOP_EY1'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_YARN_HOME'
        pattern = /.*\/usr\/lib\/hadoop-yarn.*/
      }
    }
    'HADOOP_EY2' {
      name = 'HADOOP_EY2'
      type = 'envdir'
      arguments {
        variable = 'YARN_DIR'
        relative = true
      }
    }
    'HADOOP_EY3' {
      name = 'HADOOP_EY3'
      type = 'envdir'
      arguments {
        variable = 'YARN_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EM1' {
      name = 'HADOOP_EM1'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_MAPRED_HOME'
        pattern = /.*\/usr\/lib\/hadoop-mapreduce.*/
      }
    }
    'HADOOP_EM2' {
      name = 'HADOOP_EM2'
      type = 'envdir'
      arguments {
        variable = 'MAPRED_DIR'
        relative = true
      }
    }
    'HADOOP_EM3' {
      name = 'HADOOP_EM3'
      type = 'envdir'
      arguments {
        variable = 'MAPRED_LIB_JARS_DIR'
        relative = true
      }
    }
  }
}
