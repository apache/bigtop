/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.spark

import org.apache.bigtop.itest.shell.Shell
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import org.junit.Test
import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull

import static org.apache.bigtop.itest.LogErrorsUtils.logError

public class TestSparkExample {

  private static String SPARK_HOME = System.getenv("SPARK_HOME");
  private static String SPARK_MASTER = System.getenv("SPARK_MASTER");
  static {
    assertNotNull("SPARK_HOME has to be set to run this test",  SPARK_HOME);
    assertNotNull("SPARK_MASTER has to be set to run this test", SPARK_MASTER);
  }
  static final String SPARK_EXAMPLES_DIR = SPARK_HOME + "/examples";
  static final String sparkExamplesJarFile = "spark-examples.jar";
  static final String SPARK_EXAMPLES_JAR = SPARK_HOME + "/lib/" + sparkExamplesJarFile;

  static Shell sh = new Shell("/bin/bash -s");

  @BeforeClass
  static void setUp() {

  }

  @AfterClass
  public static void tearDown() {

  }

  @Test
  void testSparkExample() {
    def examples = ["SparkPi", "JavaSparkPi"];
    examples.each() {
      String exampleClass = "org.apache.spark.examples.${it}"
      sh.exec("cd ${SPARK_HOME} && ./bin/spark-submit --class " + exampleClass + " --master ${SPARK_MASTER} " +  SPARK_EXAMPLES_JAR);
      logError(sh);
      assertTrue("Running Spark example ${it} failed", sh.getRet() == 0);
    }
  }

  @Test
  void testSparkPythonExample() {
    def pyExamples = ["pi.py"];
    pyExamples.each() {
      sh.exec("cd ${SPARK_HOME} && ./bin/spark-submit --master ${SPARK_MASTER} " + SPARK_EXAMPLES_DIR + "/src/main/python/${it}");
      logError(sh);
      assertTrue("Running Spark Python example {it} failed", sh.getRet() == 0);
    }
  }

}
