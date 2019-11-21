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

import org.junit.BeforeClass
import org.junit.AfterClass
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith

class TestTezSmoke {
  static Shell sh = new Shell("/bin/bash -s");

  static final String TEZ_HOME = System.getenv("TEZ_HOME");
  static final String HADDOP_CMD = "hadoop jar "
  static final String TEZ_EXMP_JAR = TEZ_HOME + "/tez-examples-0.9.1.jar "
  static final String TEZ_TEST_JAR = TEZ_HOME + "/tez-tests-0.9.1.jar "
  static final String SET_HADOOP_CLASSPATH = "export HADOOP_CLASSPATH=/etc/tez/conf:/usr/lib/tez/*:/usr/lib/tez/lib/*;"

  @BeforeClass
  static void TezSetUp() {
    /* Create test data set  */
    sh.exec("hadoop fs -mkdir input");
    sh.exec("hadoop fs -mkdir input1 input2 input3");

    sh.exec("echo 'Bigtop is a project for the development of packaging and tests of the Apache Hadoop ecosystem.' > testfile1");
    sh.exec("echo 'Tez smoke tests are running atop Hadoop.' > testfile2");
    sh.exec("echo 'Run separate DAG tasks within single Tez session.' > testfile3");

    sh.exec("hadoop fs -put testfile* input");

    sh.exec("hadoop fs -put testfile1 input1");
    sh.exec("hadoop fs -put testfile2 input2");
    sh.exec("hadoop fs -put testfile3 input3");
  }

  @AfterClass
  public static void TezCleanUp() {
    sh.exec("rm -f testfile*");
    sh.exec("hadoop fs -rm -r input* output*");
    assertTrue("Tez cleanup failed. ", sh.getRet() == 0);
  }

  @Test
  public void TezWordcountTest() {
    /* Basic ordered wordcount example of using a MR job in Tez. */
    sh.exec(SET_HADOOP_CLASSPATH
      + HADDOP_CMD
      + TEZ_EXMP_JAR
      + "orderedwordcount "
      + "input output"
    );
    assertTrue("Tez ordered wordcount test failed. " + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }

  @Test
  public void TezDAGsTest() {
    /* Run multiple DAGs serially on different inputs/outputs pairs. */
    sh.exec(SET_HADOOP_CLASSPATH
      + HADDOP_CMD
      + TEZ_TEST_JAR
      + "testorderedwordcount "
      + "-DUSE_TEZ_SESSION=true "
      + "input1 output1 input2 output2 input3 output3"
    );
    assertTrue("Tez DAGs test failed. " + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }
}
