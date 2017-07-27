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

import java.util.jar.JarFile
import java.util.zip.ZipInputStream

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
  static final String TEST_SPARKSQL_LOG = "/tmp/TestSpark_testSparkSQL.log"

  @BeforeClass
  static void setUp() {
    sh.exec("rm -f " + TEST_SPARKSQL_LOG)
    // create HDFS examples/src/main/resources
    sh.exec("hdfs dfs -mkdir -p examples/src/main/resources")
    // extract people.txt file into it
    String examplesJar = JarContent.getJarName("$SPARK_HOME/examples/jars", 'spark-examples.*jar')
    assertNotNull(examplesJar, "spark-examples.jar file wasn't found")
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("$SPARK_HOME/examples/jars/$examplesJar"))
    File examplesDir = new File('examples')
    examplesDir.mkdirs()
    zipInputStream.unzip(examplesDir.getName(), 'people')
    sh.exec("hdfs dfs -put examples/* examples/src/main/resources")
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
    // Let's figure out the proper mode for the submission
    // If SPARK_MASTER_IP nor SPARK_MASTER_PORT are set, we'll assume
    // 'yarn-client' mode
    String masterMode = 'yarn-client'
    if (System.env.SPARK_MASTER_IP != null && System.env.SPARK_MASTER_PORT != null)
      masterMode = "spark://$MASTER_IP:$MASTER_PORT"
    else
      println("SPARK_MASTER isn't set. yarn-client submission will be used. " +
          "Refer to smoke-tests/README If this isn't what you you expect.")

    final String SPARK_SHELL = SPARK_HOME + "/bin/spark-shell --master $masterMode"
    // Let's use time, 'cause the test has one job
    sh.exec("timeout 120 " + SPARK_SHELL +
        " --class org.apache.spark.examples.sql.JavaSparkSQLExample " +
        " --jars " + SPARK_HOME + "/examples/jars/spark-examples*.jar > " +
        TEST_SPARKSQL_LOG + " 2>&1")
    logError(sh)
    assertTrue("Failed ...", sh.getRet() == 0);
  }
}
