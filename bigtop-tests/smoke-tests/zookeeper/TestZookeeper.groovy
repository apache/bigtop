/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.junit.Test
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestZookeeper {
  static private Log LOG = LogFactory.getLog(Object.class)
  static final String ZK_HOME = System.getenv("ZOOKEEPER_HOME") ?: "/usr/lib/zookeeper"
  static Shell sh = new Shell("/bin/bash -s")

  @BeforeClass
  static void setUp() {
    // noop for now.
  }

  @AfterClass
  public static void tearDown() {
    // noop for now
  }

  @Test
  void testZkServerStatus() {
    // Basic test to verify that the server is running, and is in a
    // state that we expect.
    LOG.info('Running zkServer.sh status');
    sh.exec("${ZK_HOME}/bin/zkServer.sh status");
    logError(sh);
    assertTrue("Failed ...", sh.getRet() == 0);

    for (line in sh.getOut()) {
      if (line.contains("Mode")) {
        assertTrue(
          // If this is the only Zookeeper node, then we should be in
          // "standalone" mode. If not, we should be in "leader" or
          // "follower" mode.
          line.contains("follower") ||
          line.contains("leader") ||
          line.contains("standalone")
        );
      }
    }
    LOG.info('zkServer.sh status checks out.');
  }
}
