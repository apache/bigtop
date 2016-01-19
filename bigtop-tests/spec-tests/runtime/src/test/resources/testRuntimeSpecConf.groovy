specs {
  tests {
    'HADOOP_EJH1' {
      name = 'HADOOP_EJH1'
      type = 'shell'
      command = 'echo $JAVA_HOME'
      pattern = /.*\/usr\/.*/
    }
    'HADOOP_EC2' {
      name = 'HADOOP_EC2'
      type = 'shell'
      command = 'echo $HADOOP_COMMON_HOME'
      pattern = /.*\/usr\/lib\/hadoop.*/
    }
  }
}
