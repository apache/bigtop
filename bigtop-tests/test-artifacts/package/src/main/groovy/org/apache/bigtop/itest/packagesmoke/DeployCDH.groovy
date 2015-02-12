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

import org.junit.Test
import org.apache.bigtop.itest.pmanager.PackageManager
import org.apache.bigtop.itest.pmanager.PackageInstance
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.rules.ErrorCollector
import static org.hamcrest.CoreMatchers.equalTo

class DeployCDH {
  List<String> cdh2 = [
    "hadoop-0.20", "hadoop-0.20-conf-pseudo", "hadoop-0.20-datanode",
    "hadoop-0.20-fuse", "hadoop-0.20-jobtracker", "hadoop-0.20-namenode", "hadoop-0.20-native",
    "hadoop-0.20-pipes", "hadoop-0.20-secondarynamenode", "hadoop-0.20-source",
    "hadoop-0.20-tasktracker", "hadoop-hive", "hadoop-pig"];
  List<String> cdh3b2 = [
    "flume", "flume-master", "flume-agent", "hadoop-0.20", "hadoop-0.20-conf-pseudo", "hadoop-0.20-conf-pseudo-hue",
    "hadoop-0.20-datanode", "hadoop-0.20-fuse", "hadoop-0.20-jobtracker", "hadoop-0.20-namenode", "hadoop-0.20-native",
    "hadoop-0.20-pipes", "hadoop-0.20-secondarynamenode", "hadoop-0.20-source",
    "hadoop-0.20-tasktracker", "hadoop-hbase",
    "hadoop-hbase-master", "hadoop-hbase-regionserver", "hadoop-hbase-thrift", "hadoop-hive", "hadoop-pig",
    "hadoop-zookeeper", "hadoop-zookeeper-server", "hue", "hue-about", "hue-beeswax", "hue-common",
    "hue-filebrowser", "hue-help", "hue-jobbrowser", "hue-jobsub", "hue-plugins", "hue-proxy",
    "hue-useradmin", "oozie", "sqoop"];
  List<String> cdh3b3 = [
    "flume", "flume-master", "flume-agent", "hadoop-0.20", "hadoop-0.20-conf-pseudo",
    "hadoop-0.20-datanode", "hadoop-0.20-fuse", "hadoop-0.20-jobtracker", "hadoop-0.20-namenode", "hadoop-0.20-native",
    "hadoop-0.20-pipes", "hadoop-0.20-sbin", "hadoop-0.20-secondarynamenode", "hadoop-0.20-source",
    "hadoop-0.20-tasktracker", "hadoop-hbase", "hadoop-hbase-doc",
    "hadoop-hbase-master", "hadoop-hbase-regionserver", "hadoop-hbase-thrift", "hadoop-hive", "hadoop-pig",
    "hadoop-zookeeper", "hadoop-zookeeper-server", "hue", "hue-about", "hue-beeswax", "hue-common",
    "hue-filebrowser", "hue-help", "hue-jobbrowser", "hue-jobsub", "hue-plugins", "hue-proxy",
    "hue-useradmin", "oozie", "oozie-client", "sqoop", "sqoop-metastore"];

  List<String> aptPkg = ["hadoop-0.20-doc", "libhdfs0", "libhdfs0-dev", "python-hive"];
  List<String> yumPkg = ["hadoop-0.20-debuginfo", "hadoop-0.20-libhdfs"];
  List<String> zypperPkg = ["hadoop-0.20-libhdfs", "hadoop-0.20-doc"];

  Map<String, LinkedHashMap<String, Collection>> distPackages = [
    "2": ["apt": cdh2 + aptPkg,
      "yum": cdh2 + yumPkg + ["hadoop-0.20-docs", "hadoop-hive-webinterface"],
      "zypper": [],
      // "cloudera-desktop", "cloudera-desktop-plugins",
    ],
    "3b2": ["apt": cdh3b2 + aptPkg,
      "yum": cdh3b2 + yumPkg + ["hadoop-0.20-docs", "hadoop-hive-webinterface"],
      "zypper": [],
    ],
    "3b3": ["apt": cdh3b3 + aptPkg + ["hadoop-hbase-doc",],
      "yum": cdh3b3 + yumPkg + ["hadoop-0.20-docs", "hadoop-hive-webinterface"],
      "zypper": [],
    ],
    "3b4": ["apt": cdh3b3 + aptPkg + ["hadoop-hbase-doc",],
      "yum": cdh3b3 + yumPkg + ["hadoop-0.20-doc", "hadoop-hive-webinterface"],
      "zypper": cdh3b3 + zypperPkg,
    ],
    "3": ["apt": cdh3b3 + aptPkg + ["hadoop-hbase-doc",],
      "yum": cdh3b3 + yumPkg + ["hadoop-0.20-doc"],
      "zypper": cdh3b3 + zypperPkg,
    ],
    "3u0": ["apt": cdh3b3 + aptPkg + ["hadoop-hbase-doc",],
      "yum": cdh3b3 + yumPkg + ["hadoop-0.20-doc"],
      "zypper": cdh3b3 + zypperPkg,
    ],
    "3u1": ["apt": cdh3b3 + aptPkg + ["hadoop-hbase-doc",],
      "yum": cdh3b3 + yumPkg + ["hadoop-0.20-doc"],
      "zypper": cdh3b3 + zypperPkg,
    ],
  ];

  @Rule
  public ErrorCollector errors = new ErrorCollector();

  @Test
  void deployCDH() {
    PackageTestRepoMgr oldRepo = new PackageTestRepoMgr("cdh.prev.repo");
    PackageManager pm = oldRepo.getPm();

    List<String> packages = (distPackages[System.getProperty("cdh.prev.repo.version", "3b4")] ?: [:])[pm.type] ?: [];

    oldRepo.addRepo();
    checkThat("failed to add repository for pre-upgrade CDH deployment",
      oldRepo.getPm().refresh(), equalTo(0));

    // Lets try to remove existing packages -- just in case
    List stalePkgs = [];
    distPackages.each { key, value -> stalePkgs.addAll(value[pm.type]); }
    (stalePkgs as Set).each {
      PackageInstance pkg = PackageInstance.getPackageInstance(pm, it);
      pkg.remove();
    }

    packages.each {
      PackageInstance pkg = PackageInstance.getPackageInstance(pm, it);
      // Some packages get installed as requirement for others that we install.
      // We don't want to install them for a second time.
      if (!pkg.isInstalled()) {
        checkThat("failed to install required package ${pkg.getName()}",
          pkg.install(), equalTo(0));
      }
      pkg.getServices().each { it.value.stop(); }
    }

    oldRepo.removeRepo();
  }

  public void checkThat(String msg, Object value, Matcher<Object> matcher) {
    PackageTestErrorProxy.checkThat(errors, msg, value, matcher);
  }
}