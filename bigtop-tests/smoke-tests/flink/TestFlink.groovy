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

package org.apache.bigtop.itest.hadoop.flink

import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test

import static org.junit.Assert.assertEquals

class TestFlink {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")

  static String cmdPrefix = "export HADOOP_CONF_DIR=/etc/hadoop/conf;HADOOP_USER_NAME=hdfs"
  static private final String config_file = "/etc/flink/conf/flink-conf.yaml";


  private static void execCommand(String cmd) {
    LOG.info(cmd)

    sh.exec("$cmdPrefix $cmd")
  }

  @BeforeClass
  static void setupTest() {
    execCommand("hadoop fs -rm -r /flink");
    execCommand("hadoop fs -mkdir /flink")
    execCommand("hadoop fs -put test.data /flink/")
  }

  @AfterClass
  static void tearDown() {
    execCommand("hadoop fs -rm -r /flink")
  }

  @Test
  void testCheckRestfulAPI() {
    // read JM address and port from conf
    execCommand("awk '{if(/jobmanager.rpc.address:/) print \$2}' < "+ config_file);
    final String jmHost = sh.out.join('\n');
    execCommand("awk '{if(/rest.port:/) print \$2}' < "+config_file);
    final String webPort = sh.out.join('\n');
    // check web API
    execCommand("curl http://"+jmHost+":"+webPort+"/config");
    final String result = sh.out.join('\n');
    assert(result.contains("flink-version"));
  }

  @Test
  void testWordCountBatch() {
    execCommand("hdfs getconf -confKey fs.defaultFS");
    final String hdfsUri = sh.out.join('\n');
    execCommand("flink run \$FLINK_HOME/examples/batch/WordCount.jar --input "+hdfsUri+"/flink/test.data --output "+hdfsUri+"/tmp/result.txt")

    execCommand("hadoop fs -cat /tmp/result.txt")

    String expected =
        "black 5\n" +
        "blue 11\n" +
        "green 11\n" +
        "white 5\n" +
        "yellow 11";

    String actual = sh.out.join('\n')

    assertEquals("Incorrect output", expected, actual)
  }
}
