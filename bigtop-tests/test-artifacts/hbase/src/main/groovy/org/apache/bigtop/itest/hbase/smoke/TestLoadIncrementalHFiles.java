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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;
import org.apache.bigtop.itest.shell.Shell;

public class TestLoadIncrementalHFiles {
  private static final byte[] FAMILY = Bytes.toBytes("f1");
  private static final byte[] QUALIFIER = Bytes.toBytes("q1");
  private static final byte[][] SPLIT_KEYS = new byte[][]{
      Bytes.toBytes("ddd"),
      Bytes.toBytes("ppp")
  };
  private static Shell sh = new Shell("/bin/bash -s");

  /**
   * Test case that creates some regions and loads
   * HFiles that fit snugly inside those regions
   */
  @Test
  public void testSimpleLoad() throws Exception {
    runTest("testSimpleLoad",
        new byte[][][]{
            new byte[][]{Bytes.toBytes("aaaa"), Bytes.toBytes("cccc")},
            new byte[][]{Bytes.toBytes("ddd"), Bytes.toBytes("ooo")},
        });
  }

  /**
   * Test case that creates some regions and loads
   * HFiles that cross the boundaries of those regions
   */
  @Test
  public void testRegionCrossingLoad() throws Exception {
    runTest("testRegionCrossingLoad",
        new byte[][][]{
            new byte[][]{Bytes.toBytes("aaaa"), Bytes.toBytes("eee")},
            new byte[][]{Bytes.toBytes("fff"), Bytes.toBytes("zzz")},
        });
  }

  private void chmod(String uri) {
    sh.exec("hadoop fs -chmod -R 777 " + uri);
    assertEquals("chmod failed", 0, sh.getRet());
  }

  private void runTest(String testName, byte[][][] hfileRanges)
      throws Exception {
    FileSystem fs = HBaseTestUtil.getClusterFileSystem();
    Path dir = HBaseTestUtil.getMROutputDir(testName);
    Path familyDir = new Path(dir, Bytes.toString(FAMILY));
    Configuration conf = HBaseConfiguration.create();

    int hfileIdx = 0;
    for (byte[][] range : hfileRanges) {
      byte[] from = range[0];
      byte[] to = range[1];
      HBaseTestUtil.createHFile(conf, fs, new Path(familyDir, "hfile_" + hfileIdx++),
          FAMILY, QUALIFIER, from, to, 1000);
    }
    int expectedRows = hfileIdx * 1000;

    HBaseAdmin admin = new HBaseAdmin(conf);
    final byte[] TABLE = HBaseTestUtil.getTestTableName(testName);
    HTableDescriptor htd = new HTableDescriptor(TABLE);
    htd.addFamily(new HColumnDescriptor(FAMILY));

    admin.createTable(htd, SPLIT_KEYS);

    // Before we can load the HFiles, we need to set the permissions so that
    // HBase has write access to familyDir's contents
    chmod(familyDir.toString());

    HTable table = new HTable(conf, TABLE);
    LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
    loader.doBulkLoad(dir, table);

    assertEquals(expectedRows, HBaseTestUtil.countRows(table));

    // disable and drop if we succeeded to verify
    admin.disableTable(TABLE);
    admin.deleteTable(TABLE);
    fs.delete(dir, true);
  }

}
