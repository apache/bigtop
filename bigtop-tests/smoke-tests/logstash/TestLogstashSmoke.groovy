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
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import org.junit.Test
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith
import static org.junit.Assert.assertEquals

class TestLogstashSmoke {
  static Shell sh = new Shell("/bin/bash -s");
  private static String LOGSTASH_HOME = System.getenv("LOGSTASH_HOME");

  static final String LOGSTASH_CMD =  LOGSTASH_HOME + "/bin/logstash "
  static final String TINYBENCH_CONFIG = "resources/generator.conf "

  @Test
  public void LogstashTinyBenchTest() {
    /* Tiny benchmark with logstash Generator-plugin */
    sh.exec("time "
      + LOGSTASH_CMD
      + "-f "
      + TINYBENCH_CONFIG
    );
    assertTrue("Logstash tiny benchmark Failed.", sh.getRet() == 0);
  }
}
