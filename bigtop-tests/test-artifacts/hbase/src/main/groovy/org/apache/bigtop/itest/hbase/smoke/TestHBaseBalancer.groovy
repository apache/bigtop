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

package org.apache.bigtop.itest.hbase.smoke

import org.junit.Test
import org.apache.bigtop.itest.shell.Shell
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertTrue

/**
 * Tests the HBase balancer tool via the HBase shell.
 */
class TestHBaseBalancer {
  private static Shell sh = new Shell("hbase shell")
  private static String balancer = "balancer"
  private static String balanceSwitchTrue = "balance_switch true"
  private static String balanceSwitchFalse = "balance_switch false"

  @Test
  void testBalancer() {
    // Enable the balancer.
    sh.exec(balanceSwitchTrue)
    logError(sh)
    assertTrue(sh.getRet() == 0)

    /* Record the original state of the balancer switch. HBASE-5953
     * should allow us to get the state in an easier manner. */
    String origState = balanceSwitchTrue
    if (sh.getOut().toString().indexOf("false") != -1) {
      origState = balanceSwitchFalse
    }

    // Run the balancer.
    sh.exec(balancer)
    logError(sh)
    assertTrue(sh.getRet() == 0)

    // Disable the balancer, and verify its previous state to be true.
    sh.exec(balanceSwitchFalse)
    logError(sh)
    assertTrue(sh.getRet() == 0)
    assertTrue("balance_switch failed switching to true",
      sh.getOut().toString().indexOf("true") != -1)

    // Return balancer switch to original state, and verify its
    // previous state to be false.
    sh.exec(origState)
    logError(sh)
    assertTrue(sh.getRet() == 0)
    assertTrue("balance_switch failed switching to false",
      sh.getOut().toString().indexOf("false") != -1)
  }
}
