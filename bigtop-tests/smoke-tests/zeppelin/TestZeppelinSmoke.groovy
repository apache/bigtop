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

import org.junit.BeforeClass
import org.junit.AfterClass
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith
import org.apache.bigtop.itest.shell.Shell
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertTrue

class TestZeppelinSmoke {
  static Shell sh = new Shell("/bin/bash -s");

  private static String ZEPPELIN_HOME = System.getenv("ZEPPELIN_HOME");

  @Test
  public void InstallIntperTest() {
    sh.exec(ZEPPELIN_HOME
      + "/bin/install-interpreter.sh "
      + "--name cassandra,scio,md,python,flink,hbase,bigquery,jdbc"
    );
    logError(sh);
    assertTrue("Install Interpreter failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);

    sh.exec("su -s /bin/bash zeppelin -c \"cd /var/lib/zeppelin && /usr/lib/zeppelin/bin/zeppelin-daemon.sh --config '/etc/zeppelin/conf' restart > /dev/null 2>&1\" ");
    logError(sh);
    assertTrue("Zeppelin restart failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }
}
