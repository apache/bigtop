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
    "/usr/share/doc/crunch/crunch-examples-*job.jar");


  static String crunchTestDir = "test.crunchsmoke"
  static String localReSrc = System.properties["test.resources.dir"]?
			"${System.properties['test.resources.dir']}": ".";

  static Shell sh = new Shell("/bin/bash -s");

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -mkdir -p ${crunchTestDir}");
    sh.exec("hadoop fs -put ${localReSrc} ${crunchTestDir}");
  }

  @Test(timeout = 300000L)
  public void testWordCount() {
    sh.exec("${runnerScript} ${crunchJar}"
      + " org.apache.crunch.examples.WordCount"
      + " ${crunchTestDir}/test/text/pg11.txt out.1"
    );
    assertEquals("running Crunch example failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testSecondarySort() {
    sh.exec("${runnerScript} ${crunchJar}"
      + " org.apache.crunch.examples.SecondarySortExample"
      + " ${crunchTestDir}/test/text/secondary_sort_input.txt out.2"
    );
    assertEquals("running Crunch example failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testAverageBytesByIP() {
    sh.exec("${runnerScript} ${crunchJar}"
      + " org.apache.crunch.examples.AverageBytesByIP"
      + " ${crunchTestDir}/test/access_log/000000 out.3"
    );
    assertEquals("running Crunch example failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testTotalBytesByIP() {
    sh.exec("${runnerScript} ${crunchJar}"
      + " org.apache.crunch.examples.TotalBytesByIP"
      + " ${crunchTestDir}/test/access_log/000000 out.4"
    );
    assertEquals("running Crunch example failed", 0, sh.getRet());
  }
}

