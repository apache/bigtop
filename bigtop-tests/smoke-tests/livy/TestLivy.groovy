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

package org.apache.bigtop.itest.hadoop.livy

import com.google.gson.Gson;
import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.Test

import static org.junit.Assert.assertEquals

class TestLivy {
  static private Log LOG = LogFactory.getLog(Object.class)
  static Shell sh = new Shell("/bin/bash -s")
  static String cmdPrefix = "export HADOOP_CONF_DIR=/etc/hadoop/conf;HADOOP_USER_NAME=hdfs"
  static Gson gson = new Gson();
  static private final String config_file = "/etc/livy/conf/livy.conf";

  private static void execCommand(String cmd) {
    LOG.info(cmd)
    sh.exec("$cmdPrefix $cmd")
  }

  private LivyResponse fromJSON(String data) {
    LivyResponse response = gson.fromJson(data, LivyResponse.class);
    return response;
  }

  private boolean waitUntilReady(String uri) {
    int retryCount = 30;
    for (int i=0;i<retryCount;i++) {
      execCommand("curl " + uri);
      String state = fromJSON(sh.out.join('\n')).getState();
      if (state.equals("idle") || state.equals("available")) {
        return true;
      }
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
      }
    }
    return false;
  }

  @Test
  void testCheckLivyRestAPIs() {
    // read Livy host and port from config file
    execCommand("awk '{if(/^livy.server.port/) print \$2}' < "+ config_file);
    String port = sh.out.join('\n');
    if (port.equals("")) {
      port = "8998";
    }
    execCommand("awk '{if(/^livy.server.host:/) print \$2}' < "+config_file);
    String host = sh.out.join('\n');
    if (host.equals("")) {
      host = "127.0.0.1";
    }
    String baseURL = "http://" + host + ":" + port;

    execCommand("curl " + baseURL + "/ui");
    String result = sh.out.join('\n');
    assert(result.contains("Livy"));

    // 1. Create Livy Session
    execCommand("curl -X POST --data '{\"kind\": \"pyspark\"}' -H \"Content-Type: application/json\" " + baseURL + "/sessions")
    result = sh.out.join('\n');
    assert(result.contains("starting"));
    int sessionId = fromJSON(result).getId();
    assert(waitUntilReady(baseURL + "/sessions/" + sessionId));

    // 2. Launch Spark job
    execCommand("curl " + baseURL + "/sessions/" + sessionId + "/statements -X POST -H 'Content-Type: application/json' -d '{\"code\":\"1 + 1\"}'");
    result = sh.out.join('\n');
    int statementId = fromJSON(result).getId();
    assert(waitUntilReady(baseURL + "/sessions/" + sessionId + "/statements/" + statementId));

    // 3. Kill Livy Session
    execCommand("curl localhost:8998/sessions/" + sessionId + " -X DELETE")
    result = sh.out.join('\n');
    assert(result.contains("deleted"));
  }

  private class LivyResponse {
    public int id;
    public String state;
    
    private int getId() {
      return id;
    }

    private String getState() {
      return state;
    }
  }
}
