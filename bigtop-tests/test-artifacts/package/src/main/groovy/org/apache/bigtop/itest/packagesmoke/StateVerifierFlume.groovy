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
import java.security.MessageDigest
import org.junit.Before
import org.junit.After
import org.junit.Test

class StateVerifierFlume extends StateVerifier {
  Shell shFlume = new Shell("flume shell");

  public void createState() {
    String node;
    sleep(120001);
    shFlume.exec("connect localhost",
      "getnodestatus",
      "quit\n");
    node = shFlume.getOut().join(' ').replaceAll(/ --> IDLE.*$/, '')
      .replaceAll(/^.*Master knows about [0-9]* nodes /, '')
      .trim();
    shFlume.exec("connect localhost",
      "exec config $node 'text(\"/etc/group\")' 'collectorSink(\"hdfs://localhost/flume\",\"test\")'",
      "quit\n");
    sleep(5001);
  }

  public boolean verifyState() {
    sleep(5001);
    boolean ret = ((new Shell()).exec("hadoop fs -ls /flume >/dev/null 2>&1").getRet() == 0);
    (new Shell()).exec("hadoop fs -rmr /flume");
    return ret;
  }
}