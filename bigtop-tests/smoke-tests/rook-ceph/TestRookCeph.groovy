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

class TestRookCeph {
  static private Log LOG = LogFactory.getLog(Object.class)

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
  void testRookCephStatus() {
    // Basic test to verify that the server is running, and is in a
    // state that we expect.
    LOG.info("Running ceph status");
    // $ kubectl -n rook-ceph exec -it $(kubectl -n rook-ceph get pod -l "app=rook-ceph-tools" -o jsonpath='{.items[0].metadata.name}') ceph status
    LOG.info("How to get Cepth status from Kubernetes: " + "kubectl -n rook-ceph exec -it \$(kubectl -n rook-ceph get pod -l \"app=rook-ceph-tools\" -o jsonpath='{.items[0].metadata.name}') ceph status");
    sh.exec("kubectl -n rook-ceph exec -it \$(kubectl -n rook-ceph get pod -l \"app=rook-ceph-tools\" -o jsonpath='{.items[0].metadata.name}') ceph status");
    logError(sh);
    assertTrue("Failed ...", sh.getRet() == 0);

    String out = sh.getOut().toString();
    LOG.info("Output from command:\n" + out + "\n");
    assertTrue(out.contains("health:"));
    assertTrue(out.contains("HEALTH_OK"));
  }
}

