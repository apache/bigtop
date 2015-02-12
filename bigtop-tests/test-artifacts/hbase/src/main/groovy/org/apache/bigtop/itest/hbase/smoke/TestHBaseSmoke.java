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
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;

public class TestHBaseSmoke {
  private static final byte[] TEST_FAMILY = Bytes.toBytes("f1");
  private static final byte[] TEST_QUALIFIER = Bytes.toBytes("q1");
  private static final byte[] TEST_VALUE = Bytes.toBytes("v1");

  private static int NUM_ROWS = 100;

  /**
   * Test case that creates a table, writes a small number of rows,
   * disables the table, and exits.
   */
  @Test
  public void testSimplePutGet() throws Exception {
    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);

    HTableDescriptor htd =
        HBaseTestUtil.createTestTableDescriptor("testSimplePutGet", TEST_FAMILY);
    admin.createTable(htd);

    byte[] tableName = htd.getName();
    try {
      HTable table = new HTable(conf, tableName);
      // Write some rows
      for (int i = 0; i < NUM_ROWS; i++) {
        byte[] row = Bytes.toBytes("row_" + i);
        Put p = new Put(row);
        for (HColumnDescriptor hcd : htd.getFamilies()) {
          p.add(hcd.getName(), TEST_QUALIFIER, TEST_VALUE);
        }
        table.put(p);
      }

      table.flushCommits();

      // Read some rows
      for (int i = 0; i < NUM_ROWS; i++) {
        byte[] row = Bytes.toBytes("row_" + i);
        Get g = new Get(row);
        Result result = table.get(g);
        for (HColumnDescriptor hcd : htd.getFamilies()) {
          byte[] value = result.getValue(hcd.getName(), TEST_QUALIFIER);
          Assert.assertArrayEquals(TEST_VALUE, value);
        }
      }
    } finally {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }
  }
}
