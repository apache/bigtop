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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This program scans a table a configurable number of times. Uses
 * the table record reader.
 */
public class Scanner {
  public static final Log LOG = LogFactory.getLog(Scanner.class);

  public static int doScan(HTable table, int val) throws IOException,
      InterruptedException {
    Scan s = new Scan();
    byte[] start = {};
    byte[] stop = {};
    byte[] value = Bytes.toBytes(String.format("%010d", val));
    s.setStartRow(start);
    s.setStopRow(stop);
    SingleColumnValueFilter filter = new SingleColumnValueFilter(
        Bytes.toBytes("f1"), Bytes.toBytes("qual"), CompareOp.EQUAL, value);
    s.setFilter(filter);

    // Keep track of gathered elements.
    Multimap<String, String> mm = ArrayListMultimap.create();

    // Counts
    int cnt = 0;
    long i = 0;
    ResultScanner rs = table.getScanner(s);
    for (Result r : rs) {
      if (r.getRow() == null) {
        continue;
      }

      NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long,
          byte[]>>> columnFamilyMap = r.getMap();

      // Output time to show if flush related.
      String k = Bytes.toStringBinary(r.getRow());
      if (mm.get(k).size() >= 1) {
        System.out.println("Duplicate rowkey " + k);
        LOG.error("Duplicate rowkey " + k);
      }

      mm.put(Bytes.toStringBinary(r.getRow()), i + ": " + r);
      cnt++;
      i++;
    }

    System.out.println("scan items counted: " + cnt + " for scan " +
        s.toString() + " with filter f1:qual == " + Bytes.toString(value));

    // Print out dupes.
    int dupes = 0;
    for (Entry<String, Collection<String>> e : mm.asMap().entrySet()) {
      if (e.getValue().size() > 1) {
        dupes++;
        System.out.print("Row " + e.getKey() + " had time stamps: ");
        String[] tss = e.getValue().toArray(new String[0]);
        System.out.println(Arrays.toString(tss));
      }
    }

    return dupes;
  }

  public static void main(String argv[]) throws IOException {
    if (argv.length < 2) {
      System.err.println("usage: " + Scanner.class.getSimpleName() +
          " <table> <value>");
      System.err.println(" <value>: a numeric value [0,500)");
      System.exit(1);
    }
    Configuration conf = HBaseConfiguration.create();

    byte[] tableName = Bytes.toBytes(argv[0]);
    int val = Integer.parseInt(argv[1]);
    int loops = 1;
    for (int i = 1; i < argv.length; i++) {
      if (argv[i].equals("-l")) {
        i++;
        loops = Integer.parseInt(argv[i]);
      }
    }

    HTable table = new HTable(conf, tableName);
    int exitVal = 0;
    for (int i = 0; i < loops; i++) {
      try {
        exitVal = doScan(table, val);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      if (exitVal != 0) {
        break;
      }
    }
    System.exit(exitVal);
  }
}
