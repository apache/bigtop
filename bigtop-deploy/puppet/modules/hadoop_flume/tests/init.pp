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

hadoop_flume::agent  { "test-flume-agent":
  channels => {  mem    => { type => "memory", 
                             capacity => "1000", 
                             transactionCapactiy => "100" 
                           }, 
                 file   => { type => "file",
                             checkpointDir => "/var/lib/flume/checkpoint",
                             dataDirs => "/var/lib/flume/data",
                           }
              },
  sources  => {  netcat => { type => "netcat", 
                             bind => "0.0.0.0", 
                             port => "1949", 
                             channels => "mem" 
                           }, 
              },
  sinks    => {  hdfs   => { type => "hdfs",
                             "hdfs.path" => "/flume/test-flume-agent",
                             "hdfs.writeFormat" => "Text",
                             "hdfs.fileType" => "DataStream",
                             "hdfs.filePrefix" => "events-",
                             "hdfs.round" => "true",
                             "hdfs.roundValue" => "10",
                             "hdfs.roundUnit" => "minute",
                             "hdfs.serializer" => "org.apache.flume.serialization.BodyTextEventSerializer",
                             channel => "mem",
                           }
              },
}
