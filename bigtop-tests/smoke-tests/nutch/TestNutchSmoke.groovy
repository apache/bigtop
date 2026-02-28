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
 *
 * All tests run Nutch on the Hadoop cluster using HDFS (runtime/deploy).
 */

package org.apache.bigtop.itest.nutch

import org.apache.bigtop.itest.shell.Shell
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull

@FixMethodOrder(MethodSorters.JVM)
class TestNutchSmoke {
  static Shell sh = new Shell("/bin/bash -s")

  static final String NUTCH_CMD = "/usr/bin/nutch"
  static final String HDFS_BASE = "/user/root/nutch-smoke"

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -mkdir -p ${HDFS_BASE}/urls")
    assertTrue("hadoop fs mkdir failed: " + sh.getErr(), sh.getRet() == 0)
    sh.exec("echo 'https://bigtop.apache.org' | hadoop fs -put - ${HDFS_BASE}/urls/seed.txt")
    assertTrue("hadoop fs put seed failed: " + sh.getErr(), sh.getRet() == 0)
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs -rm -r -f ${HDFS_BASE} 2>/dev/null || true")
  }

  @Test
  void testNutchUsage() {
    sh.exec(NUTCH_CMD)
    assertTrue("nutch usage failed: " + sh.getErr(), sh.getRet() == 0)
  }

  @Test
  void testNutchInjectSubcommand() {
    sh.exec("${NUTCH_CMD} inject")
    assertTrue("nutch inject without args should fail with non-zero exit", sh.getRet() != 0)
    String out = (sh.getOut() + " " + sh.getErr()).toLowerCase()
    assertTrue("nutch inject should print usage or error (got: " + out + ")", out.contains("inject") || out.contains("usage") || out.contains("argument"))
  }

  @Test
  void testNutchInjectAndReaddb() {
    sh.exec("${NUTCH_CMD} inject ${HDFS_BASE}/crawldb ${HDFS_BASE}/urls")
    assertTrue("nutch inject (HDFS) failed: " + sh.getErr(), sh.getRet() == 0)

    sh.exec("${NUTCH_CMD} readdb ${HDFS_BASE}/crawldb -stats")
    assertTrue("nutch readdb -stats (HDFS) failed: " + sh.getErr(), sh.getRet() == 0)
    String statsOut = sh.getOut() + " " + sh.getErr()
    assertTrue("readdb output should show url/crawldb stats (got: " + statsOut + ")", statsOut.contains("url") || statsOut.contains("Number") || statsOut.contains("count") || statsOut.contains("1"))
  }

  @Test
  void testNutchGenerate() {
    sh.exec("${NUTCH_CMD} generate ${HDFS_BASE}/crawldb ${HDFS_BASE}/segments -topN 1")
    assertTrue("nutch generate (HDFS) failed: " + sh.getErr(), sh.getRet() == 0)

    sh.exec("hadoop fs -ls ${HDFS_BASE}/segments")
    assertTrue("generate should create at least one segment under ${HDFS_BASE}/segments: " + sh.getErr(), sh.getRet() == 0 && (sh.getOut() + sh.getErr()).trim().length() > 0)
  }
}
