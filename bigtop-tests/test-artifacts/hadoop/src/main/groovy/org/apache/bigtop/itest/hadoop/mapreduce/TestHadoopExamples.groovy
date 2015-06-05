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

import org.junit.BeforeClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import static org.apache.bigtop.itest.LogErrorsUtils.logError

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.runner.RunWith
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.NormalTests;
import net.jcip.annotations.NotThreadSafe

@Category ( NormalTests.class )
@NotThreadSafe
@RunWith(OrderedParameterized.class)
class TestHadoopExamples {
  static private Log LOG = LogFactory.getLog(TestHadoopExamples.class);

  private static final String YARN_EXAMPLES = System.getenv('YARN_EXAMPLES');  
  private static final String HADOOP_CONF_DIR = System.getenv('HADOOP_CONF_DIR');
  private static String hadoopExamplesJar =
    JarContent.getJarName(YARN_EXAMPLES, 'hadoop-mapreduce-examples-.*.jar');
  static {
    assertNotNull("YARN_EXAMPLES has to be set to run this test",
        YARN_EXAMPLES);
    assertNotNull("HADOOP_CONF_DIR has to be set to run this test",
        HADOOP_CONF_DIR);
    assertNotNull("Can't find hadoop-examples.jar file", hadoopExamplesJar);
  }
  static final String HADOOP_EXAMPLES_JAR =
    YARN_EXAMPLES + "/" + hadoopExamplesJar;

  static Shell sh = new Shell("/bin/bash -s");
  private static final String EXAMPLES = "examples";
  private static final String EXAMPLES_OUT = "examples-output";
  private static Configuration conf;

  private static String mr_version = System.getProperty("mr.version", "mr2");
  
    static final String RANDOMTEXTWRITER_TOTALBYTES = (mr_version == "mr1") ?
      "test.randomtextwrite.total_bytes" : "mapreduce.randomtextwriter.totalbytes";

  @BeforeClass
  static void setUp() {
    conf = new Configuration();
    TestUtils.unpackTestResources(TestHadoopExamples.class, EXAMPLES, null, EXAMPLES_OUT);
  }

  static long terasortid = System.currentTimeMillis();

  //Number of rows for terasort ~ number of splits 
  public static String terasort_rows = System.getProperty("terasort_rows", "10");
  
  static Map examples =
    [
        teragen           :"${terasort_rows} teragen${terasortid}",
        terasort          :"teragen${terasortid} terasort${terasortid}",
        teravalidate      :"terasort${terasortid} tervalidate${terasortid}",
        multifilewc       :"$EXAMPLES/text $EXAMPLES_OUT/multifilewc",
        aggregatewordcount:"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordcount 2 textinputformat",
        aggregatewordhist :"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordhist 2 textinputformat",
        grep              :"$EXAMPLES/text $EXAMPLES_OUT/grep '[Cc]uriouser'",
        secondarysort     :"$EXAMPLES/ints $EXAMPLES_OUT/secondarysort",
        randomtextwriter  :"-D $RANDOMTEXTWRITER_TOTALBYTES=1073741824 $EXAMPLES_OUT/randomtextwriter"
    ];

  private String testName;
  private String testJar;
  private String testArgs;

  @Parameters
  public static Map<String, Object[]> generateTests() {
    Map<String, Object[]> res = [:];
    examples.each { k, v -> res[k] = [k.toString(), v.toString()] as Object[]; }
    return res;
  }

  public TestHadoopExamples(String name, String args) {
    testName = name;
    testArgs = args;
    testJar = HADOOP_EXAMPLES_JAR;
  }

  @Test
  void testMRExample() {
   if(this.testName =="terasort")
   {
   sh.exec("hadoop jar $testJar $testName -Dmapreduce.terasort.output.replication=2 -Dmapreduce.terasort.simplepartitioner=true $testArgs"); 
   }
   else
  { 
    sh.exec("hadoop jar $testJar $testName $testArgs");
  }
    logError(sh);
   
    assertTrue("Example $testName $testJar $testName $testArgs failed", sh.getRet() == 0);
  }
}

