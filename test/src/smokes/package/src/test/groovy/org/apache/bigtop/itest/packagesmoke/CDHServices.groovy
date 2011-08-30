/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.packagesmoke

import org.apache.bigtop.itest.pmanager.PackageManager

class CDHServices {
  static final Map components = [
                     HDFS           : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode",
                                                     "hadoop-0.20-secondarynamenode" ],
                                        verifier : new StateVerifierHDFS(),
                                      ],
                     mapreduce      : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode",
                                                     "hadoop-0.20-jobtracker", "hadoop-0.20-tasktracker" ],
                                        verifier : new StateVerifierMapreduce(),
                                      ],
                     HBase          : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode",
                                                     "hadoop-hbase-master" ],
                                        verifier : new StateVerifierHBase(),
                                      ],
                     zookeeper      : [ services : [ "${PackageManager.getPackageManager().type == 'apt' ? 'hadoop-zookeeper-server' : 'hadoop-zookeeper'}" ],
                                        verifier : new StateVerifierZookeeper(),
                                      ],
                     oozie          : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode", "hadoop-0.20-jobtracker", "hadoop-0.20-tasktracker",
                                                     "oozie" ],
                                        verifier : new StateVerifierOozie(),
                                      ],
                     flume          : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode",
                                                     "flume-master", "flume-node" ],
                                        verifier : new StateVerifierFlume(),
                                      ],
                     sqoop          : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode",
                                                     "sqoop-metastore" ],
                                        verifier : new StateVerifierSqoop(),
                                      ],
                     hue            : [ services : [ "hadoop-0.20-namenode", "hadoop-0.20-datanode", "hadoop-0.20-jobtracker", "hadoop-0.20-tasktracker",
                                                     "hue" ],
                                        verifier : new StateVerifierHue(),
                                      ],
                   ];

  static final Map<String, List<String>> release2services = [
                     "2"            : [ "HDFS", "mapreduce" ],
                     "3b2"          : [ "HDFS", "mapreduce", "HBase", "zookeeper", "oozie", "flume",          "hue" ],
                     "3b3"          : [ "HDFS", "mapreduce", "HBase", "zookeeper", "oozie", "flume", "sqoop", "hue" ],
                     "3b4"          : [ "HDFS", "mapreduce", "HBase", "zookeeper", "oozie", "flume", "sqoop", "hue" ],
                     "3u0"          : [ "HDFS", "mapreduce", "HBase", "zookeeper", "oozie", "flume", "sqoop", "hue" ],
                     "3"            : [ "HDFS", "mapreduce", "HBase", "zookeeper", "oozie", "flume", "sqoop", "hue" ],
                   ];

  public static Map getServices(String release) {
    Map res = [:];
    List<String> services;

    if ((release =~ /\.\./).find()) {
      services = release2services[release.replaceAll(/^.*\.\./, "")].clone();
      release2services[release.replaceAll(/\.\..*$/, "")].each {
        services.remove(it);
      }
    } else {
      services = release2services[release];
    }

    services.each {
        res[it] = components[it];
    }
    return res;
  }
}
