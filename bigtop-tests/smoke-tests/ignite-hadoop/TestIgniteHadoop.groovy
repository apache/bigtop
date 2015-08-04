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

package org.apache.bigtop.itest.hadoop.ignite

import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class TestIgniteHadoop {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")

  static String hadoopClassPath = "/usr/lib/ignite-hadoop/libs/*:/usr/lib/ignite-hadoop/libs/ignite-hadoop/*"

  static String cmdPrefix = "export HADOOP_CLASSPATH=$hadoopClassPath:\$HADOOP_CLASSPATH; " +
    "export HADOOP_CONF_DIR=\$(pwd)/conf;"


  private static void execCommand(String cmd) {
    LOG.info(cmd)

    sh.exec("$cmdPrefix $cmd")
  }

  @Before
  void cleanFileSystem() {
    execCommand("hadoop fs -rm -r /gh-input")
    execCommand("hadoop fs -rm -r /gh-output")
  }

  @Test
  void test() {
    execCommand("hadoop fs -mkdir /gh-input")
    execCommand("hadoop fs -put test.data /gh-input/")

    execCommand("hadoop jar \$HADOOP_MAPRED_HOME/hadoop-mapreduce-examples.jar wordcount /gh-input /gh-output")

    execCommand("hadoop fs -cat /gh-output/part-r-00000")

    String expected =
        "black\t5\n" +
        "blue\t11\n" +
        "green\t11\n" +
        "white\t5\n" +
        "yellow\t11";

    String actual = sh.out.join('\n')

    assertEquals("Incorrect output", expected, actual)
  }
}
