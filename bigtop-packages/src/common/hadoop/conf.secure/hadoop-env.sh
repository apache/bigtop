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

# Set Hadoop-specific environment variables here.
# 
# The only required environment variable is JAVA_HOME.  All others are
# optional.  When running a distributed configuration it is best to
# set JAVA_HOME in this file, so that it is correctly defined on
# remote nodes.

# The maximum amount of heap to use, in MB. Default is 1000.
#export HADOOP_HEAPSIZE=
#export HADOOP_NAMENODE_INIT_HEAPSIZE=""

# Extra Java runtime options.  Empty by default.
export HADOOP_OPTS="-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}"

# Command specific options appended to HADOOP_OPTS when specified
export HDFS_NAMENODE_OPTS="-Dsecurity.audit.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HDFS_NAMENODE_OPTS}"
HADOOP_JOBTRACKER_OPTS="-Dsecurity.audit.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dmapred.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}"
HADOOP_TASKTRACKER_OPTS="-Dsecurity.audit.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}"
HDFS_DATANODE_OPTS="-Dsecurity.audit.logger=ERROR,DRFAS ${HDFS_DATANODE_OPT}"

export HADOOP_SECONDARYNAMENODE_OPTS="-Dsecurity.audit.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_SECONDARYNAMENODE_OPTS}"

# The following applies to multiple commands (fs, dfs, fsck, distcp etc)
export HADOOP_CLIENT_OPTS="-Xmx128m ${HADOOP_CLIENT_OPTS}"
#HADOOP_JAVA_PLATFORM_OPTS="-XX:-UsePerfData ${HADOOP_JAVA_PLATFORM_OPTS}"

# On secure datanodes, user to run the datanode as after dropping privileges
export HADOOP_SECURE_DN_USER=hdfs

# Where log files are stored.  $HADOOP_HOME/logs by default.
export HADOOP_LOG_DIR=/var/local/hadoop/logs

# Where log files are stored in the secure data environment.
export HADOOP_SECURE_DN_LOG_DIR=$HADOOP_LOG_DIR

# The directory where pid files are stored. /tmp by default.
export HADOOP_PID_DIR=/var/local/hadoop/pid
export HADOOP_SECURE_DN_PID_DIR=$HADOOP_PID_DIR

# A string representing this instance of hadoop. $USER by default.
export HADOOP_IDENT_STRING=$USER
