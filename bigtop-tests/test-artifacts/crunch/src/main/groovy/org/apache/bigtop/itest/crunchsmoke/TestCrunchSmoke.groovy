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

public class TestCrunchSmoke {
  static String runnerScript = "hadoop jar"

  static String crunchJar = "/usr/lib/crunch/crunch-examples-*job.jar";

  static Shell sh = new Shell("/bin/bash -s");
  private static final String EXAMPLES = "crunch-examples";
  private static final String EXAMPLES_OUT = "crunch-examples-output";

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -test -e $EXAMPLES");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $EXAMPLES");
      assertTrue("Deletion of previous $EXAMPLES from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("hadoop fs -test -e $EXAMPLES_OUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $EXAMPLES_OUT");
      assertTrue("Deletion of previous examples output from HDFS failed",
          sh.getRet() == 0);
    }

    // copy test files to HDFS
    sh.exec("hadoop fs -put $EXAMPLES $EXAMPLES",
        "hadoop fs -mkdir $EXAMPLES_OUT");
    assertTrue("Could not create output directory", sh.getRet() == 0);
  }

  static Map examples =
    [
        WordCount             : "${EXAMPLES}/text/pg11.txt $EXAMPLES_OUT",
        SecondarySortExample  : "${EXAMPLES}/text/secondary_sort_input.txt ${EXAMPLES_OUT}",
        AverageBytesByIP      : "${EXAMPLES}/access_log/000000 ${EXAMPLES_OUT}",
        TotalBytesByIP        : "${EXAMPLES}/access_log/000000 ${EXAMPLES_OUT}"
    ];

  private void _runExampleJobs(String algorithm) {
    sh.exec("${runnerScript} ${crunchJar}" 
      + " org.apache.crunch.examples.${algorithm}"
      + " ${examples.get(algorithm)}"
      );
    assertEquals("running Crunch example failed", sh.getRet(), 0);
  }

  @Test(timeout=300000L)
  public void testWordCount() {
    _runExampleJobs("WordCount");
  }

  @Test(timeout=300000L)
  public void testSecondarySort() {
    _runExampleJobs("SecondarySortExample");
  }

  @Test(timeout=300000L)
  public void testAverageBytesByIP() {
    _runExampleJobs("AverageBytesByIP");
  }

  @Test(timeout=300000L)
  public void testTotalBytesByIP() {
    _runExampleJobs("TotalBytesByIP");
  }
}
