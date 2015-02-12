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

import org.apache.bigtop.itest.shell.Shell

class CDHUpgradeSequence {
  private static Shell shRoot = new Shell("/bin/bash", "root");
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");

  public static String getScript(String pkg, String from, String to) {
    return "";
  }

  public static int execute(String pkg, String from, String to) {
    if (pkg == "hadoop-0.20-namenode" && (from == "2" || from == "3b2")) {
      // su hadoop -s /bin/bash -c "jps"
      // chgrp hadoop /var/log/hadoop-0.20
      // chmod g+w /var/log/hadoop-0.20
      // chown mapred /var/log/hadoop-0.20/userlogs
      // sudo -u hdfs hadoop fs -mkdir /user/joe
      // sudo -u hdfs hadoop fs -chown joe /user/joe
      // sudo -u hdfs hadoop fs -chmod 1777 /tmp
      return shRoot.exec("""
                     chown -R hdfs:hadoop /var/lib/hadoop-0.20/cache/hadoop/dfs      &&
                     chown -R mapred:hadoop /var/lib/hadoop-0.20/cache/hadoop/mapred &&
                     ln -s hadoop /var/lib/hadoop-0.20/cache/hdfs                    &&
                     service hadoop-0.20-namenode upgrade                            &&
                     service hadoop-0.20-datanode start                              &&
                     sleep 61
                  """).getRet() +
        shHDFS.exec("""hadoop dfsadmin -finalizeUpgrade &&
                     hadoop fs -chown mapred /mapred/system || /bin/true
                  """).getRet() +
        shRoot.exec("service hadoop-0.20-datanode stop").getRet() +
        shRoot.exec("service hadoop-0.20-namenode stop").getRet();
    }
    return 0;
  }
}
