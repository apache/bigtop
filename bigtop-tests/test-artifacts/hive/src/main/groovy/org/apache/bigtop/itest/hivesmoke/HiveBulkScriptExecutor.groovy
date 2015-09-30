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
package org.apache.bigtop.itest.hivesmoke

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell
import static junit.framework.Assert.assertEquals
import static org.apache.bigtop.itest.LogErrorsUtils.logError

public class HiveBulkScriptExecutor {
  private Shell sh;

  private File scripts;
  private String location;

  public HiveBulkScriptExecutor(Shell sh1,String l) {
    this.sh=sh1;
    location = l;
    scripts = new File(location);

    if (!scripts.exists()) {
      JarContent.unpackJarContainer(HiveBulkScriptExecutor.class, '.' , null);
    }
  }

  public List<String> getScripts() {
    List<String> res = [];

    try {
      scripts.eachDir { res.add(it.name); }
    } catch (Throwable ex) {}
    return res;
  }

  public void runScript(String test, String extraArgs) {
    String l = "${location}/${test}";
    String out = getOutFileName();
    sh.exec("""
    F=cat
    if [ -f ${l}/filter ]; then
      chmod 777 ${l}/filter
      F=${l}/filter
    fi
    hive ${extraArgs} -v -f ${l}/in > ${l}/actual && diff -u -b -B -w <(\$F < ${l}/actual) <(\$F < ${l}/$out)
    """);
    logError(sh)
    assertEquals("Got unexpected output from test script ${test}",
                  0, sh.ret);
  }

  public void runScript(String test) {
    runScript(test, "");
  }
  
  public static String getOutFileName() {
        return (System.getenv("HADOOP_VERSION") != null && System.getenv("HADOOP_VERSION").trim().equals("2.7.1")) ? "out_271" : "out";
  }
}
