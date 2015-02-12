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
package org.apache.bigtop.itest.hbase.system;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * This program scans a table for rows with a specified column
 * ("f1:qual") value, and updates the column with the same value.
 */
public class Putter {
  public static Put convert(Result result, int versions) {
    Put put = null;
    if (result != null) {
      NavigableMap<byte[], NavigableMap<byte[], byte[]>> cfmap =
          result.getNoVersionMap();

      if (result.getRow() != null && cfmap != null) {
        put = new Put(result.getRow());
        for (byte[] family : cfmap.keySet()) {
          NavigableMap<byte[], byte[]> qualifierMap = cfmap.get(family);

          if (qualifierMap != null) {
            for (Map.Entry<byte[], byte[]> e : qualifierMap.entrySet()) {
              byte[] qual = e.getKey();
              byte[] value = e.getValue();

              if (value != null && value.length > 0) {
                put.add(family, qual, value);
              }
            }
          }
        }
      }
    }

    return put;
  }

  public static int doScanAndPut(HTable table, int val) throws IOException {
    return doScanAndPut(table, val, true);
  }

  public static int doScanAndPut(HTable table, int val, boolean autoflush)
      throws IOException {
    Scan s = new Scan();
    byte[] start = {};
    byte[] stop = {};
    byte[] value = Bytes.toBytes(String.format("%010d", val));
    s.setStartRow(start);
    s.setStopRow(stop);
    SingleColumnValueFilter filter = new SingleColumnValueFilter(
        Bytes.toBytes("f1"), Bytes.toBytes("qual"), CompareOp.EQUAL, value);
    s.setFilter(filter);

    table.setAutoFlush(autoflush);
    ResultScanner sc = table.getScanner(s);
    int cnt = 0;
    for (Result r : sc) {
      Put p = convert(r, 0);
      table.put(p);
      cnt++;
    }
    return cnt;
  }

  public static void main(String argv[]) throws IOException {
    if (argv.length < 2) {
      System.err.println("usage: " + Putter.class.getSimpleName() +
          " <table> <value>");
      System.err.println(" <value>: a numeric value [0,500)");
      System.exit(1);
    }

    boolean autoflush = true;
    int loops = 1;
    for (int i = 1; i < argv.length; i++) {
      if (argv[i].equals("-f")) {
        autoflush = false;
      } else if (argv[i].equals("-l")) {
        i++;
        loops = Integer.parseInt(argv[i]);
      }
    }
    Configuration conf = HBaseConfiguration.create();

    byte[] tableName = Bytes.toBytes(argv[0]);
    int val = Integer.parseInt(argv[1]);
    HTable table = new HTable(conf, tableName);
    for (int i = 0; i < loops; i++) {
      try {
        doScanAndPut(table, val, autoflush);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
