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
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Before
import org.junit.Test

public class TestDFSIO {
  static Shell sh = new Shell("/bin/bash -s");

  private static final String WRITE_CMD = "-write";
  private static final String APPEND_CMD = "-append";
  private static final String READ_CMD = "-read";

  private static final String BENCHMARKS_DIR = "/benchmarks/TestDFSIO";
  private static final String HADOOP_MAPRED_HOME = System.getenv('HADOOP_MAPRED_HOME');

  private final String NUM_FILES = System.getProperty("numFiles", "10");
  private final String SIZE_FILE = System.getProperty("sizeFile", "10MB");
  private final int NUM_ITERATIONS = Integer.getInteger("numOfIterations", 1);
  private final int TIMEOUT = 5000;

  private String DFSIO_TEMPLATE;
  private String result = "TestDFSIO_Results.log";

  final String hadoopExamplesJar =
    JarContent.getJarName(HADOOP_MAPRED_HOME, 'hadoop.*jobclient.*tests.*.jar');

  final String hadoopTestJar = HADOOP_MAPRED_HOME + '/' + hadoopExamplesJar;

  @BeforeClass
  static void setUp() throws IOException {
    assertNotNull("HADOOP_MAPRED_HOME has to be set to run this test", HADOOP_MAPRED_HOME)
  }

  @Before
  void configureVars() {
    def failureVars = new FailureVars();
  }

  @Before
  public void cleanup() {
    sh.exec("hadoop fs -rm -r " + BENCHMARKS_DIR);
    Thread.sleep(TIMEOUT);
    sh.exec("hadoop fs -mkdir -p " + BENCHMARKS_DIR);
    assertTrue("Failed to create " + BENCHMARKS_DIR, sh.getRet() == 0);
  }

  @Test
  public void testDFSIO() {
    if (FailureVars.instance.getRunFailures()
      || FailureVars.instance.getServiceRestart()
      || FailureVars.instance.getServiceKill()
      || FailureVars.instance.getNetworkShutdown()) {
      runFailureThread();
    }

    DFSIO_TEMPLATE = "hadoop jar " + hadoopTestJar + " TestDFSIO %s ";
    final String DFSIO_ARGS_TEMPLATE = "-nrFiles %s -fileSize %s -resFile %s";

    final String writeSuccess = BENCHMARKS_DIR + "/io_write/_SUCCESS";
    final String appendSuccess = BENCHMARKS_DIR + "/io_append/_SUCCESS";
    final String readSuccess = BENCHMARKS_DIR + "/io_read/_SUCCESS";
    String argStr = String.format(DFSIO_ARGS_TEMPLATE, NUM_FILES, SIZE_FILE, result);

    for (int counter = 0; counter < NUM_ITERATIONS; counter++) {
      executeCmd(String.format(DFSIO_TEMPLATE + argStr, WRITE_CMD), writeSuccess);
      executeCmd(String.format(DFSIO_TEMPLATE + argStr, APPEND_CMD), appendSuccess);
      executeCmd(String.format(DFSIO_TEMPLATE + argStr, READ_CMD), readSuccess);
      argStr = String.format(DFSIO_ARGS_TEMPLATE, NUM_FILES, SIZE_FILE, result + "." + counter);
    }
  }

  private void executeCmd(String cmd, String expectedFile) {
    sh.exec(cmd);
    logError(sh);
    assertTrue("Command " + cmd + " is unsuccessful", sh.getRet() == 0);
    sh.exec("hadoop fs -ls " + expectedFile);
    boolean success = false;
    sh.getOut().each { str ->
      if (str.contains(expectedFile)) {
        success = true;
      }
    }
    assertTrue("File " + expectedFile + " was not found", success);
  }

  private void runFailureThread() {
    FailureExecutor failureExecutor = new FailureExecutor();
    Thread failureThread = new Thread(failureExecutor, "DFSIO");
    failureThread.start();
  }
}
