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
import java.util.ArrayList;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;

import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;

/**
 * This program tests concurrent scans and writes. In HBASE-4570,
 * when scanning a table during concurrent writes, rows that have
 * multiple column families sometimes get split into two rows.
 */
public class TestConcurrentScanAndPut {
  public static Shell scanSh = new Shell("/bin/bash -s");
  public static Shell putSh = new Shell("/bin/bash -s");

  public static HBaseAdmin admin;
  public static byte[] tableName;
  public static String putter_pid;

  public static int scannerLoops;
  public static int putterLoops;

  @BeforeClass
  public static void setUp() throws ClassNotFoundException,
      InterruptedException, IOException {
    System.out.println("Unpacking resources");
    JarContent.unpackJarContainer(Scanner.class, ".", null);
    JarContent.unpackJarContainer(Putter.class, ".", null);

    Configuration conf = HBaseConfiguration.create();
    try {
      HBaseAdmin.checkHBaseAvailable(conf);
    } catch (Exception e) {
      System.err.println("Hbase is not up. Bailing out.");
      System.exit(1);
    }

    tableName =
        Bytes.toBytes(new String(HBaseTestUtil.getTestTableName("concurrentScanAndPut")));
    HTableDescriptor htd = new HTableDescriptor(tableName);
    for (int i = 0; i < 10; i++) {
      htd.addFamily(new HColumnDescriptor("f" + i));
    }
    admin = new HBaseAdmin(conf);
    admin.createTable(htd);

    HTable table = new HTable(conf, tableName);
    ArrayList<Put> puts = new ArrayList<Put>(1000);

    Random rnd = new Random();
    int size = 25000;
    int batch = 2000;

    System.out.println("Creating table with 10 column families and 25k rows");
    for (int i = 0; i < size; i++) {
      String r = String.format("row%010d", i);
      Put p = new Put(Bytes.toBytes(r));
      for (int j = 0; j < 10; j++) {
        String value = String.format("%010d", rnd.nextInt(500));
        p.add(Bytes.toBytes("f" + j),
            Bytes.toBytes("qual"),
            Bytes.toBytes(value));
        String bigvalue = String.format("%0100d%0100d%0100d%0100d%0100d" +
            "%0100d%0100d%0100d%0100d%0100d",
            i, i, i, i, i, i, i, i, i, i);
        p.add(Bytes.toBytes("f" + j),
            Bytes.toBytes("data"),
            Bytes.toBytes(bigvalue));
      }
      puts.add(p);
      if (i % batch == (batch - 1)) {
        table.put(puts);
        puts.clear();
        System.out.println("put " + i);
      }
    }
    table.put(puts);
    table.flushCommits();
    table.close();

    try {
      scannerLoops = Integer.parseInt(System.getProperty(
          "concurrentScanAndPut.scanner.loops"));
    } catch (NumberFormatException e) {
      scannerLoops = 100;
    }

    try {
      putterLoops = Integer.parseInt(System.getProperty(
          "concurrentScanAndPut.putter.loops"));
    } catch (NumberFormatException e) {
      putterLoops = 100;
    }
  }

  @AfterClass
  public static void tearDown() throws IOException {
    System.out.println("Killing putter process");
    putSh.exec("kill -9 " + putter_pid);

    System.out.println("Removing test table " + Bytes.toString(tableName));
    admin.disableTable(tableName);
    admin.deleteTable(tableName);
  }

  @Test
  public void testConcurrentScanAndPut() {
    String tableNameStr = Bytes.toString(tableName);
    System.out.println("Starting puts to test table " + tableNameStr);
    putSh.exec("(HBASE_CLASSPATH=. " +
        "hbase org.apache.bigtop.itest.hbase.system.Putter " +
        tableNameStr + " 13 -l " + putterLoops +
        " > /dev/null 2>&1 & echo $! ) 2> /dev/null");
    putter_pid = putSh.getOut().get(0);

    System.out.println("Starting concurrent scans of test table " +
        tableNameStr);
    scanSh.exec("HBASE_CLASSPATH=. hbase " +
        "org.apache.bigtop.itest.hbase.system.Scanner " +
        tableNameStr + " 13 -l " + scannerLoops + " 2>/dev/null");

    int splitRows = scanSh.getRet();
    System.out.println("Split rows: " + splitRows);
    assertTrue("Rows were split when scanning table with concurrent writes",
        splitRows == 0);
  }
}
