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

package org.apache.bigtop.itest.hadoop.mapreduce

import org.apache.bigtop.itest.failures.FailureExecutor
import org.apache.bigtop.itest.failures.FailureVars
import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.runner.RunWith

@RunWith(OrderedParameterized.class)
class TestHadoopExamples {
  static private Log LOG = LogFactory.getLog(Object.class);

  private static final String HADOOP_MAPRED_HOME = System.getenv('HADOOP_MAPRED_HOME');
  private static final String HADOOP_CONF_DIR = System.getenv('HADOOP_CONF_DIR');

  private static String hadoopExamplesJar =
    JarContent.getJarName(HADOOP_MAPRED_HOME, 'hadoop.*examples.*.jar');
  static {
    assertNotNull("HADOOP_MAPRED_HOME has to be set to run this test",
      HADOOP_MAPRED_HOME);
    assertNotNull("HADOOP_CONF_DIR has to be set to run this test",
      HADOOP_CONF_DIR);
    assertNotNull("Can't find hadoop-examples.jar file", hadoopExamplesJar);
  }
  static final String HADOOP_EXAMPLES_JAR =
    HADOOP_MAPRED_HOME + "/" + hadoopExamplesJar;

  static Shell sh = new Shell("/bin/bash -s");

  /**
   * Public so that we can run these tests as scripts
   * and the scripts can manually copy resoruces into DFS
   * See BIGTOP-1222 for example.
   */
  public static final String SOURCE = "bigtop-tests/test-artifacts/hadoop/src/main/resources/"
  private static final String EXAMPLES = "examples";
  private static final String EXAMPLES_OUT = "examples-output";
  private static Configuration conf;

  private static String mr_version = System.getProperty("mr.version", "mr2");

  static final String RANDOMTEXTWRITER_TOTALBYTES = (mr_version == "mr1") ?
    "test.randomtextwrite.total_bytes" : "mapreduce.randomtextwriter.totalbytes";

  @AfterClass
  public static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash ${EXAMPLES}",
      "hadoop fs -rmr -skipTrash ${EXAMPLES_OUT}");
  }


  @BeforeClass
  static void setUp() {
    conf = new Configuration();
    try {
      //copy examples/ int /user/root/ and
      //then create examples-output directory
      TestUtils.unpackTestResources(TestHadoopExamples.class, EXAMPLES, null, EXAMPLES_OUT);
    }
    catch (java.lang.Throwable t) {
      LOG.info("Failed to unpack jar resources.  Attemting to use bigtop sources");
      def source = System.getenv("BIGTOP_HOME") + "/" + SOURCE;

      assertNotNull("Can't copy test input files from bigtop source dir," +
        "and jar specific attempt failed also", examples);

      LOG.info("MAKING DIRECTORIES ..................... ${EXAMPLES} ${EXAMPLES_OUT}");

      //add the files in resources/
      sh.exec("hadoop fs -put ${source}/*.* .");
      //add the directories under resources (like examples/)
      sh.exec("hadoop fs -put ${source}/${EXAMPLES} ${EXAMPLES}");
      sh.exec("hadoop fs -mkdir -p ${EXAMPLES_OUT}");
    }
    sh.exec("hadoop fs -ls ${EXAMPLES}");
    assertTrue("Failed asserting that 'examples' were created in the DFS", sh.getRet() == 0);
  }

  static long terasortid = System.currentTimeMillis();

  //Number of rows for terasort ~ number of splits 
  public static String terasort_rows = System.getProperty("terasort_rows", "1000");

  //Number of maps and samples for pi
  public static String pi_maps = System.getProperty("pi_maps", "2");
  public static String pi_samples = System.getProperty("pi_samples", "1000");
  static LinkedHashMap examples =
    [
      pi: "${pi_maps} ${pi_samples}",
      wordcount: "$EXAMPLES/text $EXAMPLES_OUT/wordcount",
      multifilewc: "$EXAMPLES/text $EXAMPLES_OUT/multifilewc",
      aggregatewordcount: "$EXAMPLES/text $EXAMPLES_OUT/aggregatewordcount 2 textinputformat",
      aggregatewordhist: "$EXAMPLES/text $EXAMPLES_OUT/aggregatewordhist 2 textinputformat",
      grep: "$EXAMPLES/text $EXAMPLES_OUT/grep '[Cc]uriouser'",
      secondarysort: "$EXAMPLES/ints $EXAMPLES_OUT/secondarysort",
      randomtextwriter: "-D $RANDOMTEXTWRITER_TOTALBYTES=1073741824 $EXAMPLES_OUT/randomtextwriter"
    ];

  // The following example MR jobs are enabled only when running without QFS,
  // which doesn't seem to work with TeraOutputFormat. See BIGTOP-3413 for details.
  static LinkedHashMap additional_examples =
    [
      teragen: "${terasort_rows} teragen${terasortid}",
      terasort: "teragen${terasortid} terasort${terasortid}",
      teravalidate: "terasort${terasortid} tervalidate${terasortid}"
    ];

  private String testName;
  private String testJar;
  private String testArgs;

  @Parameters
  public static LinkedHashMap<String, Object[]> generateTests() {
    LinkedHashMap<String, Object[]> res = [:];
    examples.each { k, v -> res[k] = [k.toString(), v.toString()] as Object[]; }
    additional_examples.each { k, v -> res[k] = [k.toString(), v.toString()] as Object[]; }
    return res;
  }

  public TestHadoopExamples(String name, String args) {
    testName = name;
    testArgs = args;
    testJar = HADOOP_EXAMPLES_JAR;
  }

  @Test (timeout = 0x1620000l)
  void testMRExample() {
    if (FailureVars.instance.getRunFailures()
      || FailureVars.instance.getServiceRestart()
      || FailureVars.instance.getServiceKill()
      || FailureVars.instance.getNetworkShutdown()) {
      runFailureThread();
    }
    sh.exec("hadoop jar $testJar $testName $testArgs");
    assertTrue("Example $testName $testJar $testName $testArgs failed", sh.getRet() == 0);
  }

  private void runFailureThread() {
    FailureExecutor failureExecutor = new FailureExecutor();
    Thread failureThread = new Thread(failureExecutor, "TestHadoopExamples");
    failureThread.start();
  }
}

