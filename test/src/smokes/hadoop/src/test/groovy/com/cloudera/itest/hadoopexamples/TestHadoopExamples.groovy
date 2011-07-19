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

package com.cloudera.itest.hadoopexamples

import org.junit.Before
import static org.junit.Assert.assertNotNull
import com.cloudera.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import com.cloudera.itest.JarContent
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

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
  private static final String hadoop = "$HADOOP_HOME/bin/hadoop";

  static Shell sh = new Shell("/bin/bash -s");
  private static final String EXAMPLES = "examples";
  private static final String EXAMPLES_OUT = "examples-output";
  private static Configuration conf;
  private static String HADOOP_OPTIONS;

  @Before
  void setUp() {
    conf = new Configuration();
    conf.addResource('mapred-site.xml');
    HADOOP_OPTIONS =
      "-fs ${conf.get('fs.default.name')} -jt ${conf.get('mapred.job.tracker')}";
    // Unpack resource
    JarContent.unpackJarContainer(TestHadoopExamples.class, '.' , null)

    sh.exec("$hadoop fs $HADOOP_OPTIONS -test -e $EXAMPLES");
    if (sh.getRet() == 0) {
      sh.exec("$hadoop fs $HADOOP_OPTIONS -rmr -skipTrash $EXAMPLES");
      assertTrue("Deletion of previous $EXAMPLES from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("$hadoop fs $HADOOP_OPTIONS -test -e $EXAMPLES_OUT");
    if (sh.getRet() == 0) {
      sh.exec("$hadoop fs $HADOOP_OPTIONS -rmr -skipTrash $EXAMPLES_OUT");
      assertTrue("Deletion of previous examples output from HDFS failed",
          sh.getRet() == 0);
    }

// copy test files to HDFS
    sh.exec("hadoop fs $HADOOP_OPTIONS -put $EXAMPLES $EXAMPLES",
        "hadoop fs $HADOOP_OPTIONS -mkdir $EXAMPLES_OUT");
    assertTrue("Could not create output directory", sh.getRet() == 0);
  }

  def failures = [];

  def examples =
    [
        pi                :'20 10',
        wordcount         :"$EXAMPLES/text $EXAMPLES_OUT/wordcount",
        multifilewc       :"$EXAMPLES/text $EXAMPLES_OUT/multifilewc",
//        aggregatewordcount:"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordcount 5 textinputformat",
//        aggregatewordhist :"$EXAMPLES/text $EXAMPLES_OUT/aggregatewordhist 5 textinputformat",
        grep              :"$EXAMPLES/text $EXAMPLES_OUT/grep '[Cc]uriouser'",
        sleep             :"-m 10 -r 10",
        secondarysort     :"$EXAMPLES/ints $EXAMPLES_OUT/secondarysort",
        randomtextwriter  :"-Dtest.randomtextwrite.total_bytes=1073741824 $EXAMPLES_OUT/randomtextwriter"
    ];

  @Test
  void testMRExamples() {
    examples.each { testName, args ->
      sh.exec("$hadoop jar $HADOOP_EXAMPLES_JAR $testName $HADOOP_OPTIONS $args");

      if (sh.getRet()) {
        failures.add(testName);
      }
    }
    assertTrue("The following tests have failed: " + failures, failures.size() == 0);
  }
}
