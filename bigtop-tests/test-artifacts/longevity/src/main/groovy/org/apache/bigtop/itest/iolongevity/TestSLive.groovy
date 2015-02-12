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

package org.apache.bigtop.itest.iolongevity

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.failures.FailureExecutor
import org.apache.bigtop.itest.failures.FailureVars
import org.apache.bigtop.itest.shell.Shell
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/** SLive test includes the following test sequence
 *  100% creates, 100% renames, 100% read, 100% append, 100% ls, and 100% delete
 *  A mix 20% creates, 20% read, 20% append, 20% ls, 20% mkdir
 */

public class TestSLive {
  static Shell sh = new Shell("/bin/bash -s")
  private static final String hadoopMapReduceHome =
    System.getProperty('HADOOP_MAPRED_HOME', '/usr/lib/hadoop-mapreduce')
  private static final String Slive_jar =
    JarContent.getJarName(hadoopMapReduceHome,
      'hadoop.mapreduce.client.jobclient.*.tests.jar')
  private static final String SLIVE_JAR =
    hadoopMapReduceHome + "/" + Slive_jar
  private static final int SLEEP_TIMEOUT = 5000
  private static final String SLIVE_OUTPUT_FILE = "/test/slive/slive/output"
  private static final String SLIVE_ROOT_FILE = "/test/slive"
  private final int numOfIterations = Integer.getInteger("numOfIterations", 1);
  static String[] sliveCmds

  @Before
  void configureVars() {
    def failureVars = new FailureVars();
  }

  @BeforeClass
  static void setUp() throws IOException {
    assertNotNull("Can't find hadoop.mapreduce.client.jobclient.tests.jar",
      Slive_jar)
    final String numSliveFiles = System.getProperty("numSliveFiles", "100")
    final String writeSize = System.getProperty("writeSize", "20480,20480")
    final String readSize = System.getProperty("readSize", "20480,20480")
    final String appendSize = System.getProperty("appendSize", "20480,20480")
    final String blockSize = System.getProperty("blockSize", "10240,10240")

    String SLIVE_TEMPLATE = "hadoop jar %s SliveTest -create %s -delete %s " +
      "-rename %s -read %s -append %s -ls %s -mkdir %s -files %s " +
      "-writeSize %s -readSize %s -appendSize %s -blockSize %s -resFile %s"
    sliveCmds = [
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 100, 0, 0, 0, 0, 0, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputcreate.txt"), //create
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 0, 0, 100, 0, 0, 0, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputrename.txt"), //rename
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 0, 0, 0, 100, 0, 0, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputread.txt"), //read
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 0, 0, 0, 0, 100, 0, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputappend.txt"), //append
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 0, 0, 0, 0, 0, 100, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputls.txt"), //ls
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 0, 100, 0, 0, 0, 0, 0,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputdelete.txt"), //delete
      String.format(SLIVE_TEMPLATE, SLIVE_JAR, 20, 0, 0, 20, 20, 20, 20,
        numSliveFiles, writeSize, readSize, appendSize, blockSize,
        "sliveOutputmix.txt") //mix
    ]
  }

  public void setupDir() {
    sh.exec("hadoop fs -rm -r " + SLIVE_ROOT_FILE)
    sh.exec("hadoop fs -mkdir " + SLIVE_ROOT_FILE)
    Thread.sleep(SLEEP_TIMEOUT)
  }

  public void cleanup() {
    sh.exec("hadoop fs -rm -r " + SLIVE_OUTPUT_FILE)
    Thread.sleep(SLEEP_TIMEOUT)
  }

  @Test
  public void testSlive() {
    if (FailureVars.instance.getRunFailures()
      || FailureVars.instance.getServiceRestart()
      || FailureVars.instance.getServiceKill()
      || FailureVars.instance.getNetworkShutdown()) {
      runFailureThread();
    }

    String suffix = ""
    for (int counter = 0; counter < numOfIterations; counter++) {
      setupDir()
      sliveCmds.each { cmd ->
        sh.exec(cmd + suffix)
        logError(sh)
        assertTrue(sh.getRet() == 0)
        String expectedFile = SLIVE_OUTPUT_FILE + "/_SUCCESS"
        sh.exec("hadoop fs -ls " + expectedFile)
        boolean success = false
        sh.getOut().each { str ->
          if (str.contains(expectedFile)) {
            success = true
          }
        }
        assertTrue("File " + expectedFile + " was not found", success)
        cleanup()
      }
      suffix = "." + counter
    }
  }

  private void runFailureThread() {
    FailureExecutor failureExecutor = new FailureExecutor();
    Thread failureThread = new Thread(failureExecutor, "SLive");
    failureThread.start();
  }
}
