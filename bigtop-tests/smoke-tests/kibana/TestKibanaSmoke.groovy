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
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertEquals

class TestKibanaSmoke {
  static Shell sh = new Shell("/bin/bash -s");
  private static String KIBANA_HOME = System.getenv("KIBANA_HOME");

  static final String KIBANA_START =  KIBANA_HOME + "/bin/start-kibana;"
  static final String WAIT_FOR_COMPLETION =  "sleep 60;"
  static final String GET_KIBANA_STATUS =  "curl -i localhost:5601;"

  @Test
  public void KibanaWebTest() {
    /* Check if elasticsearch was deployed firstly */
    def folder = new File("/usr/lib/elasticsearch");
    if (!folder.exists()) {
      println "No ElasticSearch stack, please install ES first.";
      assertTrue(0);
    }

    sh.exec(KIBANA_START + WAIT_FOR_COMPLETION + GET_KIBANA_STATUS);
    logError(sh);
    assertTrue("Kibana start failed", sh.getRet() == 0);
  }
}
