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
import org.junit.Assert;
import org.junit.Test;

import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;

public class TestLoadIncrementalHFiles {
  private static final byte[] FAMILY = Bytes.toBytes("f1");
  private static final byte[] QUALIFIER = Bytes.toBytes("q1");
  private static final byte[][] SPLIT_KEYS = new byte[][] {
    Bytes.toBytes("ddd"),
    Bytes.toBytes("ppp")
  };

  /**
   * Test case that creates some regions and loads
   * HFiles that fit snugly inside those regions
   */
  @Test
  public void testSimpleLoad() throws Exception {
    runTest("testSimpleLoad",
        new byte[][][] {
          new byte[][]{ Bytes.toBytes("aaaa"), Bytes.toBytes("cccc") },
          new byte[][]{ Bytes.toBytes("ddd"), Bytes.toBytes("ooo") },
    });
  }

  /**
   * Test case that creates some regions and loads
   * HFiles that cross the boundaries of those regions
   */
  @Test
  public void testRegionCrossingLoad() throws Exception {
    runTest("testRegionCrossingLoad",
        new byte[][][] {
          new byte[][]{ Bytes.toBytes("aaaa"), Bytes.toBytes("eee") },
          new byte[][]{ Bytes.toBytes("fff"), Bytes.toBytes("zzz") },
    });
  }

  private void runTest(String testName, byte[][][] hfileRanges)
      throws Exception {
    FileSystem fs = HBaseTestUtil.getClusterFileSystem();
    Path dir = HBaseTestUtil.getMROutputDir(testName);
    Path familyDir = new Path(dir, Bytes.toString(FAMILY));

    int hfileIdx = 0;
    for (byte[][] range : hfileRanges) {
      byte[] from = range[0];
      byte[] to = range[1];
      HBaseTestUtil.createHFile(fs, new Path(familyDir, "hfile_" + hfileIdx++),
          FAMILY, QUALIFIER, from, to, 1000);
    }
    int expectedRows = hfileIdx * 1000;

    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);
    final byte[] TABLE = HBaseTestUtil.getTestTableName(testName);
    HTableDescriptor htd = new HTableDescriptor(TABLE);
    htd.addFamily(new HColumnDescriptor(FAMILY));

    admin.createTable(htd, SPLIT_KEYS);

    HTable table = new HTable(conf, TABLE);
    LoadIncrementalHFiles loader = new LoadIncrementalHFiles(conf);
    loader.doBulkLoad(dir, table);

    Assert.assertEquals(expectedRows, HBaseTestUtil.countRows(table));

    // disable and drop if we succeeded to verify
    admin.disableTable(TABLE);
    admin.deleteTable(TABLE);
    fs.delete(dir, true);
  }

}
