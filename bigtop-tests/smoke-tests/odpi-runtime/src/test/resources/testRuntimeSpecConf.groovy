/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

specs {
  tests {
    'HADOOP_EJH1' {
      name = 'HADOOP_EJH1'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'JAVA_HOME'
      }
    }
    'HADOOP_EC1' {
      name = 'HADOOP_EC1'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'HADOOP_TOOLS_PATH'
        donotcheckexistance = true
      }
    }
    'HADOOP_EC2' {
      name = 'HADOOP_EC2'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'HADOOP_COMMON_HOME'
      }
    }
    'HADOOP_EC3' {
      name = 'HADOOP_EC3'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'HADOOP_COMMON_DIR'
        relative = true
      }
    }
    'HADOOP_EC4' {
      name = 'HADOOP_EC4'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'HADOOP_COMMON_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EC5' {
      name = 'HADOOP_EC5'
      type = 'envdir'
      arguments {
        envcmd = 'hadoop envvars'
        variable = 'HADOOP_CONF_DIR'
      }
    }
    'HADOOP_EH1' {
      name = 'HADOOP_EH1'
      type = 'envdir'
      arguments {
        envcmd = 'hdfs envvars'
        variable = 'HADOOP_HDFS_HOME'
      }
    }
    'HADOOP_EH2' {
      name = 'HADOOP_EH2'
      type = 'envdir'
      arguments {
        envcmd = 'hdfs envvars'
        variable = 'HDFS_DIR'
        relative = true
      }
    }
    'HADOOP_EH3' {
      name = 'HADOOP_EH3'
      type = 'envdir'
      arguments {
        envcmd = 'hdfs envvars'
        variable = 'HDFS_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EY1' {
      name = 'HADOOP_EY1'
      type = 'envdir'
      arguments {
        envcmd = 'yarn envvars'
        variable = 'HADOOP_YARN_HOME'
      }
    }
    'HADOOP_EY2' {
      name = 'HADOOP_EY2'
      type = 'envdir'
      arguments {
        envcmd = 'yarn envvars'
        variable = 'YARN_DIR'
        relative = true
      }
    }
    'HADOOP_EY3' {
      name = 'HADOOP_EY3'
      type = 'envdir'
      arguments {
        envcmd = 'yarn envvars'
        variable = 'YARN_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EM1' {
      name = 'HADOOP_EM1'
      type = 'envdir'
      arguments {
        envcmd = 'mapred envvars'
        variable = 'HADOOP_MAPRED_HOME'
      }
    }
    'HADOOP_EM2' {
      name = 'HADOOP_EM2'
      type = 'envdir'
      arguments {
        envcmd = 'mapred envvars'
        variable = 'MAPRED_DIR'
        relative = true
      }
    }
    'HADOOP_EM3' {
      name = 'HADOOP_EM3'
      type = 'envdir'
      arguments {
        envcmd = 'mapred envvars'
        variable = 'MAPRED_LIB_JARS_DIR'
        relative = true
      }
    }
    'HADOOP_EJH2_HADOOP' {
      name = 'HADOOP_EJH2_HADOOP'
      type = 'shell'
      arguments {
        command = '[ "${JAVA_HOME}xxx" != "xxx" ] || grep -E "^\\s*export\\s+JAVA_HOME=[\\w/]+" `hadoop envvars | grep HADOOP_CONF_DIR | sed "s|[^=]\\+=\'\\([^\']\\+\\)\'$|\\1|g"`/hadoop-env.sh'
        message = 'JAVA_HOME is not set'
      }
    }
    'HADOOP_EJH2_YARN' {
      name = 'HADOOP_EJH2_YARN'
      type = 'shell'
      arguments {
        command = '[ "${JAVA_HOME}xxx" != "xxx" ] || grep -E "^\\s*export\\s+JAVA_HOME=[\\w/]+" `hadoop envvars | grep HADOOP_CONF_DIR | sed "s|[^=]\\+=\'\\([^\']\\+\\)\'$|\\1|g"`/yarn-env.sh'
        message = 'JAVA_HOME is not set'
      }
    }
    'HADOOP_PLATVER_1' {
      name = 'HADOOP_PLATVER'
      type = 'shell'
      arguments {
        command = 'hadoop version | head -n 1 | grep -E \'Hadoop\\s+[0-9\\.]+[_\\-][A-Za-z_0-9]+\''
        message = 'Hadoop\'s version string is not correct'
      }
    }
    'HADOOP_DIRSTRUCT_COMMON' {
      name = 'HADOOP_DIRSTRUCT_COMMON'
      type = 'dirstruct'
      arguments {
        envcmd = 'hadoop envvars'
        baseDirEnv = 'HADOOP_COMMON_HOME'
        referenceList = 'hadoop-common.list'
      }
    }
    'HADOOP_DIRSTRUCT_HDFS' {
      name = 'HADOOP_DIRSTRUCT_HDFS'
      type = 'dirstruct'
      arguments {
        envcmd = 'hdfs envvars'
        baseDirEnv = 'HADOOP_HDFS_HOME'
        referenceList = 'hadoop-hdfs.list'
      }
    }
    'HADOOP_DIRSTRUCT_MAPRED' {
      name = 'HADOOP_DIRSTRUCT_MAPRED'
      type = 'dirstruct'
      arguments {
        envcmd = 'mapred envvars'
        baseDirEnv = 'HADOOP_MAPRED_HOME'
        referenceList = 'hadoop-mapreduce.list'
      }
    }
    'HADOOP_DIRSTRUCT_YARN' {
      name = 'HADOOP_DIRSTRUCT_YARN'
      type = 'dirstruct'
      arguments {
        envcmd = 'yarn envvars'
        baseDirEnv = 'HADOOP_YARN_HOME'
        referenceList = 'hadoop-yarn.list'
      }
    }
    'HADOOP_SUBPROJS' {
      name = 'HADOOP_SUBPROJS'
      type = 'dirstruct'
      arguments {
        envcmd = 'hadoop envvars'
        baseDirEnv = 'HADOOP_COMMON_HOME'
        referenceList = 'hadoop-subprojs.list'
      }
    }
    'HADOOP_BINCONTENT_COMMON' {
      name = 'HADOOP_BINCONTENT_COMMON'
      type = 'dirstruct'
      arguments {
        envcmd = 'hadoop envvars'
        baseDirEnv = 'HADOOP_COMMON_HOME'
        subDir = 'bin'
        referenceList = 'hadoop-common-bin.list'
      }
    }
    'HADOOP_BINCONTENT_HDFS' {
      name = 'HADOOP_BINCONTENT_HDFS'
      type = 'dirstruct'
      arguments {
        envcmd = 'hdfs envvars'
        baseDirEnv = 'HADOOP_HDFS_HOME'
        subDir = 'bin'
        referenceList = 'hadoop-hdfs-bin.list'
      }
    }
    'HADOOP_BINCONTENT_MAPRED' {
      name = 'HADOOP_BINCONTENT_MAPRED'
      type = 'dirstruct'
      arguments {
        envcmd = 'mapred envvars'
        baseDirEnv = 'HADOOP_MAPRED_HOME'
        subDir = 'bin'
        referenceList = 'hadoop-mapreduce-bin.list'
      }
    }
    'HADOOP_BINCONTENT_YARN' {
      name = 'HADOOP_BINCONTENT_YARN'
      type = 'dirstruct'
      arguments {
        envcmd = 'yarn envvars'
        baseDirEnv = 'HADOOP_YARN_HOME'
        subDir = 'bin'
        referenceList = 'hadoop-yarn-bin.list'
      }
    }
    'HADOOP_LIBJARSCONTENT_COMMON' {
      name = 'HADOOP_JARCONTENT_COMMON'
      type = 'dirstruct'
      arguments {
        envcmd = 'hadoop envvars'
        baseDirEnv = 'HADOOP_COMMON_HOME'
        subDirEnv = 'HADOOP_COMMON_LIB_JARS_DIR'
        referenceList = 'hadoop-common-jar.list'
      }
    }
    'HADOOP_LIBJARSCONTENT_HDFS' {
      name = 'HADOOP_JARCONTENT_HDFS'
      type = 'dirstruct'
      arguments {
        envcmd = 'hdfs envvars'
        baseDirEnv = 'HADOOP_HDFS_HOME'
        subDirEnv = 'HDFS_LIB_JARS_DIR'
        referenceList = 'hadoop-hdfs-jar.list'
      }
    }
    'HADOOP_LIBJARSCONTENT_MAPRED' {
      name = 'HADOOP_JARCONTENT_MAPRED'
      type = 'dirstruct'
      arguments {
        envcmd = 'mapred envvars'
        baseDirEnv = 'HADOOP_MAPRED_HOME'
        subDirEnv = 'MAPRED_LIB_JARS_DIR'
        referenceList = 'hadoop-mapreduce-jar.list'
      }
    }
    'HADOOP_LIBJARSCONTENT_YARN' {
      name = 'HADOOP_JARCONTENT_YARN'
      type = 'dirstruct'
      arguments {
        envcmd = 'yarn envvars'
        baseDirEnv = 'HADOOP_YARN_HOME'
        subDirEnv = 'YARN_LIB_JARS_DIR'
        referenceList = 'hadoop-yarn-jar.list'
      }
    }
    'HADOOP_GETCONF' {
      name = 'HADOOP_GETCONF'
      type = 'shell'
      arguments {
        command = '[ `hdfs getconf -confKey dfs.permissions.superusergroup >/dev/null 2>/dev/null; echo $?` == "0" ]'
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
        message = 'hadoop-common-project must be build with -Prequire.snappy'
      }
    }
    'HADOOP_HNATIVE1' {
      name = 'HADOOP_HNATIVE1'
      type = 'shell'
      arguments {
        command = '[ ! -n ${HADOOP_COMMON_HOME} ] || HADOOP_COMMON_HOME=`hadoop envvars | grep HADOOP_COMMON_HOME | sed "s/.*=\'\\(.*\\)\'/\\1/"`; '+
            'test -e $HADOOP_COMMON_HOME/lib/native/libhdfs.a'
        message = 'hadoop-hdfs-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_YNATIVE1' {
      name = 'HADOOP_YNATIVE1'
      type = 'shell'
      arguments {
        command = '[ ! -n ${HADOOP_YARN_HOME} ] || HADOOP_YARN_HOME=`yarn envvars | grep HADOOP_YARN_HOME | sed "s/.*=\'\\(.*\\)\'/\\1/"`; '+
            'echo $HADOOP_YARN_HOME; test -e $HADOOP_YARN_HOME/bin/container-executor'
        message = 'hadoop-yarn-project must be build with -Pnative or -Pnative-win'
      }
    }
    'HADOOP_MNATIVE1' {
      name = 'HADOOP_MNATIVE1'
      type = 'shell'
      arguments {
        command = 'hadoop checknative -a 2>/dev/null | grep snappy | grep true'
        message = 'hadoop-mapreduce-project must be build with -Prequire.snappy'
      }
    }
    'HADOOP_COMPRESSION' {
      name = 'HADOOP_COMPRESSION'
      type = 'shell'
      arguments {
        command = '[[ "$(hadoop checknative -a 2>/dev/null | egrep -e ^zlib -e ^snappy | sort -u | grep true | wc -l)" == 2 ]]'
        message = 'hadoop must be built with -Dcompile.native=true'
      }
    }
    'HADOOP_TOOLS' {
      name = 'HADOOP_TOOLS'
      type = 'hadoop_tools'
      arguments {
      }
    }
    'HADOOP_USERS' {
      name = 'HADOOP_USERS'
      type = 'hadoop_users'
      arguments {
        envcmd = 'hadoop envvars'
        confDir = 'HADOOP_CONF_DIR'
      }
    }
    'HADOOP_API1' {
      name = "HADOOP_API1"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_COMMON_HOME'
        libDir = 'HADOOP_COMMON_DIR'
        envcmd = 'hadoop envvars'
        jar = 'hadoop-common'
        resourceFile = 'hadoop-common-2.7.3-api-report.json'
      }
    }
    'HADOOP_API2' {
      name = "HADOOP_API2"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_HDFS_HOME'
        libDir = 'HDFS_DIR'
        envcmd = 'hdfs envvars'
        jar = 'hadoop-hdfs'
        resourceFile = 'hadoop-hdfs-2.7.3-api-report.json'
      }
    }
    'HADOOP_API3' {
      name = "HADOOP_API3"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_YARN_HOME'
        libDir = 'YARN_DIR'
        envcmd = 'yarn envvars'
        jar = 'hadoop-yarn-common'
        resourceFile = 'hadoop-yarn-common-2.7.3-api-report.json'
      }
    }
    'HADOOP_API4' {
      name = "HADOOP_API4"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_YARN_HOME'
        libDir = 'YARN_DIR'
        envcmd = 'yarn envvars'
        jar = 'hadoop-yarn-client'
        resourceFile = 'hadoop-yarn-client-2.7.3-api-report.json'
      }
    }
    'HADOOP_API5' {
      name = "HADOOP_API5"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_YARN_HOME'
        libDir = 'YARN_DIR'
        envcmd = 'yarn envvars'
        jar = 'hadoop-yarn-api'
        resourceFile = 'hadoop-yarn-api-2.7.3-api-report.json'
      }
    }
    'HADOOP_API6' {
      name = "HADOOP_API6"
      type = 'api_examination'
      arguments {
        baseDirEnv = 'HADOOP_MAPRED_HOME'
        libDir = 'MAPRED_DIR'
        envcmd = 'mapred envvars'
        jar = 'hadoop-mapreduce-client-core'
        resourceFile = 'hadoop-mapreduce-client-core-2.7.3-api-report.json'
      }
    }
  }
}
