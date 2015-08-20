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

package org.apache.bigtop.itest.spark

import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestSpark {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")
  static final String SPARK_HOME = System.getenv("SPARK_HOME")
  static final String SPARK_SHELL = SPARK_HOME + "/bin/spark-shell --master yarn-client"
  static final String TEST_SPARKSQL_LOG = "/tmp/TestSpark_testSparkSQL.log"

  @BeforeClass
  static void setUp() {
    sh.exec("rm -f " + TEST_SPARKSQL_LOG)
    sh.exec("hdfs dfs -put " + SPARK_HOME + "/examples examples")
    logError(sh)
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -ls")
    logError(sh)
    sh.exec("hdfs dfs -rmr people* examples")
    logError(sh)
  }

  @Test
  void testSparkSQL() {
    sh.exec(SPARK_SHELL + " --class org.apache.spark.examples.sql.JavaSparkSQL " + " --jars " + SPARK_HOME + "/lib/spark-examples*.jar > " + TEST_SPARKSQL_LOG + " 2>&1")
    logError(sh)
    assertTrue("Failed ...", sh.getRet() == 0);
  }
}
