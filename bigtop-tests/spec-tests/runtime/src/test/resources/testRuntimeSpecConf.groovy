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
    'HADOOP_EC1' {
      name = 'HADOOP_EC1'
      type = 'envdir'
      arguments {
        variable = 'HADOOP_TOOLS_PATH'
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
    'HADOOP_EJH2_HADOOP' {
      name = 'HADOOP_EJH2_HADOOP'
      type = 'shell'
      arguments {
        command = '[ "${JAVA_HOME}xxx" != "xxx" ] || grep -E "^\\s*export\\s+JAVA_HOME=[\\w/]+" $HADOOP_CONF_DIR/hadoop-env.sh'
        message = 'JAVA_HOME is not set'
      }
    }
    'HADOOP_EJH2_YARN' {
      name = 'HADOOP_EJH2_YARN'
      type = 'shell'
      arguments {
        command = '[ "${JAVA_HOME}xxx" != "xxx" ] || grep -E "^\\s*export\\s+JAVA_HOME=[\\w/]+" $HADOOP_CONF_DIR/yarn-env.sh'
        message = 'JAVA_HOME is not set'
      }
    }
    'HADOOP_PLATVER_1' {
      name = 'HADOOP_PLATVER'
      type = 'shell'
      arguments {
        command = 'hadoop version | head -n 1 | grep -E \'Hadoop\\s+[0-9\\.]+-[A-Za-z_0-9]+\''
        message = 'Hadoop\'s version string is not correct'
      }
    }
    'HADOOP_PLATVER_2' {
      name = 'HADOOP_PLATVER'
      type = 'shell'
      arguments {
        command = 'grep -E \'STARTUP_MSG:\\s+version\' `find /var/log/ -path "*hadoop*" -name "*.log" | head -n 1` ' +
        '| sed \'s/[^=]\\+= //\' | grep -E \'[0-9\\.]+-[A-Za-z_0-9]+\''
        message = 'Log files do not contain correct correct version'
      }
    }
    'HADOOP_DIRSTRUCT_1' {
      name = 'HADOOP_DIRSTRUCT'
      type = 'dirstruct'
      arguments {
        baseDirEnv = 'HADOOP_COMMON_HOME'
        referenceList = 'hadoop-common.list'
      }
    }
    'HADOOP_DIRSTRUCT_2' {
      name = 'HADOOP_DIRSTRUCT'
      type = 'dirstruct'
      arguments {
        baseDirEnv = 'HADOOP_HDFS_HOME'
        referenceList = 'hadoop-hdfs.list'
      }
    }
    'HADOOP_DIRSTRUCT_3' {
      name = 'HADOOP_DIRSTRUCT'
      type = 'dirstruct'
      arguments {
        baseDirEnv = 'HADOOP_MAPRED_HOME'
        referenceList = 'hadoop-mapreduce.list'
      }
    }
    'HADOOP_DIRSTRUCT_4' {
      name = 'HADOOP_DIRSTRUCT'
      type = 'dirstruct'
      arguments {
        baseDirEnv = 'HADOOP_YARN_HOME'
        referenceList = 'hadoop-yarn.list'
      }
    }
    'HADOOP_GETCONF' {
      name = 'HADOOP_GETCONF'
      type = 'shell'
      arguments {
        command = '[ `${HADOOP_HDFS_HOME}/bin/hdfs getconf -confKey dfs.permissions.superusergroup` == "hadoop" ]'
        message = 'It\' not possible to to determine key Hadoop configuration values by using ${HADOOP_HDFS_HOME}/bin/hdfs getconf'
      }
    }
    'HADOOP_CNATIVE1' {
      name = 'HADOOP_CNATIVE1'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep hadoop | grep true'
        message = 'hadoop-common-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_CNATIVE2' {
      name = 'HADOOP_CNATIVE2'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep snappy | grep true'
        message = 'hadoop-common-project must be build with Prequire.snappy'
      }
    }
    'HADOOP_HNATIVE1' {
      name = 'HADOOP_HNATIVE1'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep hadoop | grep true'
        message = 'hadoop-hdfs-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_YNATIVE1' {
      name = 'HADOOP_YNATIVE1'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep hadoop | grep true'
        message = 'hadoop-yarn-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_MNATIVE1' {
      name = 'HADOOP_MNATIVE1'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep hadoop | grep true'
        message = 'hadoop-mapreduce-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_MNATIVE2' {
      name = 'HADOOP_MNATIVE2'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep snappy | grep true'
        message = 'hadoop-mapreduce-project must be build with Prequire.snappy'
    'HADOOP_TOOLS' {
      name = 'HADOOP_TOOLS'
      type = 'hadoop_tools'
      arguments {
      }
    }
  }
}
