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

import java.net.URL;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.PerformanceEvaluation;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.mapreduce.NMapInputFormat;
import org.apache.hadoop.hbase.util.Bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;

import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;

public class TestHFileOutputFormat {
  private static final int ROWSPERSPLIT = 1024;

  private static final byte[][] FAMILIES =
      {Bytes.add(PerformanceEvaluation.FAMILY_NAME, Bytes.toBytes("-A")),
          Bytes.add(PerformanceEvaluation.FAMILY_NAME, Bytes.toBytes("-B"))};

  private static final String HBASE_HOME = System.getenv("HBASE_HOME");
  private static final String HBASE_CONF_DIR = System.getenv("HBASE_CONF_DIR");

  static {
    assertNotNull("HBASE_HOME has to be set to run this test",
        HBASE_HOME);
    assertNotNull("HBASE_CONF_DIR has to be set to run this test",
        HBASE_CONF_DIR);
  }

  private static String hbase_jar =
      JarContent.getJarName(HBASE_HOME, "hbase-.*(?<!tests).jar");
  private static String hbase_tests_jar =
      JarContent.getJarName(HBASE_HOME, "hbase-.*tests.jar");
  private static URL incrload_jar_url =
      JarContent.getJarURL(org.apache.bigtop.itest.hbase.smoke.IncrementalPELoad.class);

  static {
    assertNotNull("Can't find hbase.jar", hbase_jar);
    assertNotNull("Can't find hbase-tests.jar", hbase_tests_jar);
    assertNotNull("Can't find jar containing IncrementalPELoad class", incrload_jar_url);
  }

  private static final String HBASE_JAR = HBASE_HOME + "/" + hbase_jar;
  private static final String HBASE_TESTS_JAR = HBASE_HOME + "/" + hbase_tests_jar;
  private static final String ZOOKEEPER_JAR = HBASE_HOME + "/lib/zookeeper.jar";
  private static final String INCRLOAD_JAR = incrload_jar_url.getFile();
  private static final String INCRLOAD = "org.apache.bigtop.itest.hbase.smoke.IncrementalPELoad";
  private static final String USER = System.getProperty("user.name");
  private static Shell sh = new Shell("/bin/bash -s");

  @AfterClass
  public static void cleanUp() {
    // delete junk from HDFS
    sh.exec("hadoop fs -rmr -skipTrash /user/" + USER + "/partitions_*");
    assertTrue("HDFS cleanup failed", sh.getRet() == 0);
  }

  @Ignore("HBASE-1861")
  @Test
  public void testMRIncrementalLoad() throws Exception {
    doIncrementalLoadTest("testMRIncrementalLoad", false);
  }

  @Ignore("HBASE-1861")
  @Test
  public void testMRIncrementalLoadWithSplit() throws Exception {
    doIncrementalLoadTest("testMRIncrementalLoadWithSplit", true);
  }

  private byte[][] generateRandomSplitKeys(int numKeys) {
    Random random = new Random();
    byte[][] ret = new byte[numKeys][];
    for (int i = 0; i < numKeys; i++) {
      ret[i] = PerformanceEvaluation.generateData(random, 10);
    }
    return ret;
  }

  private void doIncrementalLoadTest(String testName, boolean shouldChangeRegions)
      throws Exception {
    FileSystem fs = HBaseTestUtil.getClusterFileSystem();
    Path testDir = HBaseTestUtil.getMROutputDir(testName);
    byte[][] splitKeys = generateRandomSplitKeys(4);

    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);
    final byte[] TABLE_NAME = HBaseTestUtil.getTestTableName(testName);
    HTableDescriptor tbldesc = new HTableDescriptor(TABLE_NAME);
    HColumnDescriptor coldesc1 = new HColumnDescriptor(FAMILIES[0]);
    HColumnDescriptor coldesc2 = new HColumnDescriptor(FAMILIES[1]);
    tbldesc.addFamily(coldesc1);
    tbldesc.addFamily(coldesc2);
    admin.createTable(tbldesc, splitKeys);
    HTable table = new HTable(conf, TABLE_NAME);
    assertEquals("Should start with empty table",
        0, HBaseTestUtil.countRows(table));

    // Generate the bulk load files
    runIncrementalPELoad(Bytes.toString(TABLE_NAME), testDir.toString());
    // This doesn't write into the table, just makes files
    assertEquals("HFOF should not touch actual table",
        0, HBaseTestUtil.countRows(table));

    // Make sure that a directory was created for every CF
    int dir = 0;
    for (FileStatus f : fs.listStatus(testDir)) {
      for (byte[] family : FAMILIES) {
        if (Bytes.toString(family).equals(f.getPath().getName())) {
          ++dir;
        }
      }
    }
    assertEquals("Column family not found in FS.", FAMILIES.length, dir);

    // handle the split case
    if (shouldChangeRegions) {
      admin.disableTable(TABLE_NAME);
      admin.deleteTable(TABLE_NAME);
      byte[][] newSplitKeys = generateRandomSplitKeys(14);
      admin.createTable(tbldesc, newSplitKeys);
    }

    // Before we can load the HFiles, we need to set the permissions so that
    // HBase has write access to testDir's contents
    chmod(testDir.toString());

    // Perform the actual load
    new LoadIncrementalHFiles(conf).doBulkLoad(testDir, table);

    // Ensure data shows up
    int expectedRows = NMapInputFormat.getNumMapTasks(conf) * ROWSPERSPLIT;
    assertEquals("LoadIncrementalHFiles should put expected data in table",
        expectedRows, HBaseTestUtil.countRows(table));
    Scan scan = new Scan();
    ResultScanner results = table.getScanner(scan);
    int count = 0;
    for (Result res : results) {
      count++;
      assertEquals(FAMILIES.length, res.raw().length);
      KeyValue first = res.raw()[0];
      for (KeyValue kv : res.raw()) {
        assertTrue(KeyValue.COMPARATOR.matchingRows(first, kv));
        assertTrue(Bytes.equals(first.getValue(), kv.getValue()));
      }
    }
    results.close();
    String tableDigestBefore = HBaseTestUtil.checksumRows(table);

    // Cause regions to reopen
    admin.disableTable(TABLE_NAME);
    admin.enableTable(TABLE_NAME);
    assertEquals("Data should remain after reopening of regions",
        tableDigestBefore, HBaseTestUtil.checksumRows(table));

    // cleanup
    // - disable and drop table
    admin.disableTable(TABLE_NAME);
    admin.deleteTable(TABLE_NAME);
    // - remove incremental load output
    fs.delete(testDir, true);
  }

  private void chmod(String uri) {
    sh.exec("hadoop fs -chmod -R 777 " + uri);
    assertEquals("chmod failed", 0, sh.getRet());
  }

  private void runIncrementalPELoad(String table, String outDir) {
    sh.exec("export HADOOP_CLASSPATH=" + HBASE_CONF_DIR + ":" + HBASE_JAR + ":" + HBASE_TESTS_JAR + ":" + ZOOKEEPER_JAR,
        "hadoop jar " + INCRLOAD_JAR + " " + INCRLOAD +
            " -libjars " + HBASE_JAR + "," + HBASE_TESTS_JAR +
            " " + table + " " + outDir);
    assertEquals("MR job failed", 0, sh.getRet());
  }

}
