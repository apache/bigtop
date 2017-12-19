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
package org.apache.bigtop.itest.hbase.smoke;

import org.apache.hadoop.conf.Configuration

import static org.junit.Assert.assertTrue
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Ignore


import org.apache.bigtop.itest.shell.Shell

class TestHBaseCompression {
  private static final String OUTPUT = "snappy-output";
  private static final String TEST = "org.apache.hadoop.hbase.util.CompressionTest";
  private static Configuration conf;
  private static String HADOOP_OPTIONS;
  private static String HDFS_PATH;
  private static String JAVA_LIBRARY_PATH =
    System.getProperty("snappy.lib.path", "");
  private static Shell sh = new Shell('/bin/bash -s');

  @BeforeClass
  static void setUp() {
    conf = new Configuration();
    conf.addResource('mapred-site.xml');
    HADOOP_OPTIONS =
      "-fs ${conf.get('fs.default.name')} -jt ${conf.get('mapred.job.tracker')}";
    sh.exec("whoami");
    String user = sh.out[0];
    HDFS_PATH = "${conf.get('fs.default.name')}/user/$user/$OUTPUT";
    sh.exec("hadoop fs $HADOOP_OPTIONS -test -e $OUTPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs $HADOOP_OPTIONS -rmr -skipTrash $OUTPUT");
      assertTrue("Deletion of previous $OUTPUT from HDFS failed",
        sh.getRet() == 0);
    }
    sh.exec("hadoop fs $HADOOP_OPTIONS -mkdir $OUTPUT");
    assertTrue("Could not create $OUTPUT directory", sh.getRet() == 0);
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs $HADOOP_OPTIONS -test -e $OUTPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs $HADOOP_OPTIONS -rmr -skipTrash $OUTPUT");
      assertTrue("Deletion of $OUTPUT from HDFS failed",
        sh.getRet() == 0);
    }
  }

  void _testCompression(String codec) {
    // workaround for hbase; set HBASE_LIBRARY_PATH
    sh.exec("export HBASE_LIBRARY_PATH=$JAVA_LIBRARY_PATH",
      "hbase $TEST $HDFS_PATH/testfile.$codec $codec");
    assertTrue("test failed with codec: $codec", sh.getRet() == 0);
  }

  @Test
  void testNoCompression() {
    _testCompression("none");
  }

  @Test
  void testGzipCompression() {
    _testCompression("gz");
  }

  @Test
  void testSnappyCompression() {
    _testCompression("snappy");
  }
}
