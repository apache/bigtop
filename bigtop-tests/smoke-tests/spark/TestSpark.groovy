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
  static final String SPARK_MASTER_IP = System.getenv("SPARK_MASTER_IP")
  static final String SPARK_MASTER_PORT = System.getenv("SPARK_MASTER_PORT")
  static final String TEST_SPARKSQL_LOG = "/tmp/TestSpark_testSparkSQL.log"
  static final String TEST_SPARKR_LOG = "/tmp/TestSpark_testSparkR.log"

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
    sh.exec("hdfs dfs -rm -r examples")
    logError(sh)
  }

  @Test
  void testSparkSQL() {
    // Let's figure out the proper mode for the submission
    // If SPARK_MASTER_IP nor SPARK_MASTER_PORT are not set,
    // we'll assume 'yarn' as the master URL.
    String master = 'yarn'
    if (SPARK_MASTER_IP != null && SPARK_MASTER_PORT != null)
      master = "spark://$SPARK_MASTER_IP:$SPARK_MASTER_PORT"
    else
      println("SPARK_MASTER isn't set. yarn will be used as the master URL. " +
          "Refer to smoke-tests/README If this isn't what you you expect.")

    final String SPARK_SHELL = SPARK_HOME + "/bin/spark-shell --master $master"
    // Let's use time, 'cause the test has one job
    sh.exec("timeout 300 " + SPARK_SHELL +
        " --class org.apache.spark.examples.sql.SparkSQLExample " +
        " --jars " + SPARK_HOME + "/examples/jars/spark-examples*.jar > " +
        TEST_SPARKSQL_LOG + " 2>&1")
    logError(sh)
    assertTrue("Failed ...", sh.getRet() == 0);
  }

  @Test
  void testSparkR() {
    String master = 'yarn'
    if (SPARK_MASTER_IP != null && SPARK_MASTER_PORT != null)
      master = "spark://$SPARK_MASTER_IP:$SPARK_MASTER_PORT"
    else
      println("SPARK_MASTER isn't set. yarn will be used as the master URL. " +
          "Refer to smoke-tests/README If this isn't what you you expect.")

    new File('/tmp/dataframe.R').withWriter { writer ->
      new File(SPARK_HOME + "/examples/src/main/r/dataframe.R").eachLine { line ->
        writer << line.replace('file.path(Sys.getenv("SPARK_HOME"), ', 'file.path(') +
          System.getProperty("line.separator")
      }
    }

    final String SPARK_SUBMIT = SPARK_HOME + "/bin/spark-submit --master $master"
    sh.exec("timeout 300 " + SPARK_SUBMIT + " /tmp/dataframe.R > " + TEST_SPARKR_LOG + " 2>&1")
    logError(sh)
    assertTrue("Failed to execute SparkR script", sh.getRet() == 0);
  }

  // parse spark-defaults.conf to properties object
  def parseSparkDefaultsConf(String path) {

    def file = new File(path)
    def props = new Properties()
    props.load(file.newDataInputStream())
    return props

  }

  @Test
  void testSparkHistoryServer() {

    String SPARK_CONF_DIR = SPARK_HOME + File.separator + "conf"
    String SPARK_DEFAULTS_CONF = SPARK_CONF_DIR + File.separator + "spark-defaults.conf"

    def props = parseSparkDefaultsConf(SPARK_DEFAULTS_CONF)
    String spark_history_server_port = props.getProperty("spark.history.ui.port")

    sh.exec("hostname -f")
    logError(sh)
    String spark_history_server_host = sh.getOut()[0]

    sh.exec("curl -s -o /dev/null -w'%{http_code}' --negotiate -u: -k http://" + spark_history_server_host + 
                  ":" + spark_history_server_port + " | grep 200")
    logError(sh)
    assertTrue("Failed to check spark history server", sh.getOut()[0] == "200")

  }

}
