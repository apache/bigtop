/*
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
package org.apache.bigtop.itest.crunch.smoke

import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import org.apache.bigtop.itest.shell.Shell
import org.apache.bigtop.itest.TestUtils

public class TestCrunchSmoke {
  static String runnerScript = "hadoop jar"

  static String crunchJar = System.getProperty(
    "org.apache.bigtop.itest.crunch.smoke.crunch.jar",
    "/usr/share/doc/crunch*/crunch-examples-*job.jar");

  static Shell sh = new Shell("/bin/bash -s");
  private static final String EXAMPLES = "crunch-examples";
  private static final String EXAMPLES_OUT = "crunch-examples-output";

  @BeforeClass
  static void setUp() {
    TestUtils.unpackTestResources(TestCrunchSmoke.class, EXAMPLES, null, EXAMPLES_OUT);
  }

  static Map examples =
    [
      WordCount: "${EXAMPLES}/text/pg11.txt $EXAMPLES_OUT",
      SecondarySortExample: "${EXAMPLES}/text/secondary_sort_input.txt ${EXAMPLES_OUT}",
      AverageBytesByIP: "${EXAMPLES}/access_log/000000 ${EXAMPLES_OUT}",
      TotalBytesByIP: "${EXAMPLES}/access_log/000000 ${EXAMPLES_OUT}"
    ];

  private void _runExampleJobs(String algorithm) {
    sh.exec("hadoop fs -rmr ${EXAMPLES_OUT}");
    sh.exec("${runnerScript} ${crunchJar}"
      + " org.apache.crunch.examples.${algorithm}"
      + " ${examples.get(algorithm)}"
    );
    assertEquals("running Crunch example failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testWordCount() {
    _runExampleJobs("WordCount");
  }

  @Test(timeout = 300000L)
  public void testSecondarySort() {
    _runExampleJobs("SecondarySortExample");
  }

  @Test(timeout = 300000L)
  public void testAverageBytesByIP() {
    _runExampleJobs("AverageBytesByIP");
  }

  @Test(timeout = 300000L)
  public void testTotalBytesByIP() {
    _runExampleJobs("TotalBytesByIP");
  }
}
