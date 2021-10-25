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
package org.apache.bigtop.itest.hbase.smoke

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.util.Bytes
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import static org.junit.Assert.assertTrue
import org.apache.bigtop.itest.shell.Shell
import org.apache.bigtop.itest.hbase.util.HBaseTestUtil
import static org.apache.bigtop.itest.LogErrorsUtils.logError

/**
 * Validates the Import and Export utilities.
 */
class TestHBaseImportExport {
  private static Shell sh = new Shell("/bin/bash -s")

  private static final byte[] TEST_FAMILY = Bytes.toBytes("f1")
  private static final byte[] TEST_QUALIFIER = Bytes.toBytes("q1")
  private static final byte[] TEST_VALUE = Bytes.toBytes("v1")

  private static String orig
  private static String export
  private static HTable origTable
  private static HTable exportTable

  private static String importCmd =
    "hbase org.apache.hadoop.hbase.mapreduce.Import"
  private static String exportCmd =
    "hbase org.apache.hadoop.hbase.mapreduce.Export"

  private static int NUM_ROWS = 5000
  private static Configuration conf
  private static HBaseAdmin admin

  @BeforeClass
  public static void setUp() throws Exception {
    conf = HBaseConfiguration.create()
    admin = new HBaseAdmin(conf)

    HTableDescriptor htd_orig =
      HBaseTestUtil.createTestTableDescriptor("orig", TEST_FAMILY)
    admin.createTable(htd_orig)
    orig = new String(htd_orig.getName())
    origTable = new HTable(conf, orig)

    HTableDescriptor htd_export =
      HBaseTestUtil.createTestTableDescriptor("export", TEST_FAMILY)
    admin.createTable(htd_export)
    export = new String(htd_export.getName())
    exportTable = new HTable(conf, export)

    // Write some rows to the table.
    for (int i = 0; i < NUM_ROWS; i++) {
      byte[] row = Bytes.toBytes("row_" + i)
      Put p = new Put(row)
      for (HColumnDescriptor hcd : htd_orig.getFamilies()) {
        p.add(hcd.getName(), TEST_QUALIFIER, TEST_VALUE)
      }
      origTable.put(p)
    }
    origTable.flushCommits()
  }

  @AfterClass
  public static void tearDown() throws Exception {
    admin.disableTable(orig)
    admin.deleteTable(orig)
    admin.disableTable(export)
    admin.deleteTable(export)
    sh.exec("hadoop fs -rm -r -skipTrash 'orig_*'");
  }

  @Test
  public void testImportExport() throws Exception {
    // Export
    sh.exec(exportCmd + " " + orig + " " + orig)
    logError(sh)
    assertTrue(sh.getRet() == 0)

    // Import
    sh.exec(importCmd + " " + export + " " + orig)
    logError(sh)
    assertTrue(sh.getRet() == 0)

    // Verify both tables' contents are equal
    String origDigest = HBaseTestUtil.checksumRows(origTable)
    String exportDigest = HBaseTestUtil.checksumRows(exportTable)
    assertTrue("Original and exported tables contain different data",
      origDigest.equals(exportDigest))
  }

}
