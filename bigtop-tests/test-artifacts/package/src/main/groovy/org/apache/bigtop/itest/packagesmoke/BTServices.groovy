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

class BTServices {
  static final List serviceDaemonUserNames = ["hbase", "hdfs", "hue", "mapred",
    "zookeeper", "hadoop"];

  static final Map components = [
    HDFS: [services: ["hadoop-namenode", "hadoop-datanode",
      "hadoop-secondarynamenode"],
      verifier: new StateVerifierHDFS(),
      killIDs: ["hdfs"],
    ],
    mapreduce: [services: ["hadoop-namenode", "hadoop-datanode",
      "hadoop-jobtracker", "hadoop-tasktracker"],
      killIDs: ["hdfs", "mapred"],
      verifier: new StateVerifierMapreduce(),
    ],
    hive: [services: ["hadoop-namenode", "hadoop-datanode",
      "hadoop-jobtracker", "hadoop-tasktracker"],
      killIDs: ["hdfs", "mapred"],
      verifier: new StateVerifierHive(),
    ],
    HBase: [services: ["hadoop-namenode", "hadoop-datanode",
      "hbase-master"],
      killIDs: ["hdfs", "hbase"],
      verifier: new StateVerifierHBase(),
    ],
    zookeeper: [services: ["hadoop-zookeeper"],
      verifier: new StateVerifierZookeeper(),
      killIDs: ["zookeeper"],
    ],
    hue: [services: ["hadoop-namenode", "hadoop-datanode", "hadoop-jobtracker", "hadoop-tasktracker",
      "hue"],
      killIDs: ["hdfs", "mapred", "hue"],
      verifier: new StateVerifierHue(),
    ],
  ];

  static final Map<String, List<String>> release2services = [
    "bigtop": ["HDFS", "mapreduce", "hive", "HBase", "zookeeper"],
  ];

  public static Map getServices(String release) {
    Map res = [:];
    List<String> services;

    if ((release =~ /\.\./).find()) {
      String release_from = release.replaceAll(/\.\..*$/, "");
      release = release.replaceAll(/^.*\.\./, "");
      services = release2services[release].clone();
      release2services[release_from].each {
        services.remove(it);
      }
    } else {
      services = release2services[release];
    }

    services.each {
      // zookeeper is a very messy case of naming :-(
      if (it == "zookeeper" &&
        (PackageManager.getPackageManager().type == 'apt' ||
          release == "3" || release == "3u1" || release == "bigtop")) {
        res[it] = [services: ["hadoop-zookeeper-server"],
          verifier: new StateVerifierZookeeper(),
        ];
      } else {
        res[it] = components[it];
      }
    }
    return res;
  }
}
