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

package org.apache.bigtop.itest.hadoopexamples

import org.junit.BeforeClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.runner.RunWith

@RunWith(OrderedParameterized.class)
class TestHadoopExamples {
  static private Log LOG = LogFactory.getLog(TestHadoopExamples.class);

  private static final String HADOOP_HOME = System.getenv('HADOOP_HOME');
  private static final String HADOOP_CONF_DIR = System.getenv('HADOOP_CONF_DIR');
  private static String hadoopExamplesJar =
    JarContent.getJarName(HADOOP_HOME, 'hadoop.*examples.*.jar');
  static {
    assertNotNull("HADOOP_HOME has to be set to run this test",
        HADOOP_HOME);
    assertNotNull("HADOOP_CONF_DIR has to be set to run this test",
        HADOOP_CONF_DIR);
    assertNotNull("Can't find hadoop-examples.jar file", hadoopExamplesJar);
  }
  static final String HADOOP_EXAMPLES_JAR =
    HADOOP_HOME + "/" + hadoopExamplesJar;

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
    // Unpack resource
    JarContent.unpackJarContainer(TestHadoopExamples.class, '.' , null)

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
        pi                :'2 1000',
        wordcount         :"$EXAMPLES/text $EXAMPLES_OUT/wordcount",
        multifilewc       :"$EXAMPLES/text $EXAMPLES_OUT/multifilewc",
        aggregatewordcount:"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordcount 2 textinputformat",
        aggregatewordhist :"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordhist 2 textinputformat",
        grep              :"$EXAMPLES/text $EXAMPLES_OUT/grep '[Cc]uriouser'",
//        sleep             :"-m 10 -r 10",
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
    sh.exec("hadoop jar $testJar $testName $testArgs");

    assertTrue("Example $testName failed", 
               sh.getRet() == 0);
  }
}
