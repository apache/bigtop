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
  static Shell sh = new Shell("/bin/bash -s");

  private File scripts;
  private String location;

  public HiveBulkScriptExecutor(String l) {
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
    sh.exec("""
    F=cat
    if [ -f ${l}/filter ]; then
      chmod 777 ${l}/filter
      F=${l}/filter
    fi
    """)
    logError(sh)
    sh.exec("hive ${extraArgs} -v -f ${l}/in > ${l}/actual")
    logError(sh)
    sh.exec("diff -B -w -b -u <(\$F < ${l}/actual) <(\$F < ${l}/out)")
    logError(sh)
    assertEquals("Got unexpected output from test script ${test}",
                  0, sh.ret);
  }

  public void runScript(String test) {
    runScript(test, "");
  }
}
