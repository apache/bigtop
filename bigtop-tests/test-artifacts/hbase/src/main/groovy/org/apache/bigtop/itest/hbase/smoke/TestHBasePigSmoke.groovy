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

import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Ignore
import static junit.framework.Assert.assertEquals
import static org.junit.Assert.assertTrue

class TestHBasePigSmoke {
  private static final int ROW_CNT = 10;

  private static String extra_jars =
    System.getProperty("org.apache.bigtop.itest.hbase.smoke.TestHBasePigSmoke.extra_jars",
      "");
  private static String register_clause = "";
  private static String tmp = "TestHBasePigSmoke-${(new Date().getTime())}";
  private static String TABLE = "smoke-${tmp}";
  private static String FAM1 = 'family1';
  private static String FAM2 = 'family2';

  private static Shell shHBase = new Shell('hbase shell');
  private static Shell shPig = new Shell('pig');
  private static Shell sh = new Shell('/bin/bash -s');

  static {
    extra_jars.split(':').each {
      register_clause <<= "register ${it};"
    }
  }

  @BeforeClass
  static void setUp() {
    shHBase.exec("create '$TABLE', '$FAM1', '$FAM2'",
      "describe '$TABLE'",
      "quit\n");
    assertEquals("Creating of the ${TABLE} failed",
      0, shHBase.ret);
  }

  @AfterClass
  static void tearDown() {
    shHBase.exec("disable '$TABLE'",
      "drop '$TABLE'",
      "quit\n");

    sh.exec("hadoop fs -rmr $TABLE");
  }

  @Ignore("BIGTOP-219")
  @Test(timeout = 300000L)
  public void Pig2HBase() {
    def script = "\n";

    (1..ROW_CNT).each {
      script <<= String.format('%020d %d %s\n', it, it, 'localhost')
    }

    sh.exec("hadoop dfs -mkdir $TABLE",
      "hadoop dfs -put <(cat << __EOT__${script}__EOT__) ${TABLE}/data");
    assertEquals("Can't copy data to HDFS",
      0, sh.ret);

    shPig.exec("""
      ${register_clause}
      data = LOAD '$TABLE' using PigStorage(' ') as (cnt1, cnt2, name);
      store data into '$TABLE' using org.apache.pig.backend.hadoop.hbase.HBaseStorage(
             '$FAM1:cnt1 $FAM1:cnt2 $FAM2:name','-caster HBaseBinaryConverter');
      quit
      """);
    assertEquals("Failed loading data via PIG",
      0, shPig.ret);

    shHBase.exec("scan '$TABLE'",
      "quit\n");
    assertTrue("Scanning the table returned wrong # of rows",
      (shHBase.out.get(shHBase.out.size() - 3) =~ "^$ROW_CNT row.s. in .* seconds").find());
  }

  @Test(timeout = 300000L)
  @Ignore("BIGTOP-219")
  public void HBase2Pig() {
    def script = "\n";

    (1..10).each {
      script <<= String.format("put '$TABLE', '%020d', '$FAM1:f1', '%s'\n", it, 'localhost');
      script <<= String.format("put '$TABLE', '%020d', '$FAM2:f2', '%s'\n", it, 'localhost');
    }
    script << "quit\n\n";

    shHBase.exec(script);

    shPig.exec("""
      ${register_clause}
      hbaseData = LOAD '$TABLE' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage(
            '$FAM1:f1 $FAM2:f2', '-loadKey') as (rowkey, count, name) ;
      store hbaseData into '${TABLE}/pig' using PigStorage(',');
      quit
      """);

    sh.exec("hadoop fs -cat $TABLE/pig/part* | wc -l");
    assertEquals("Scanning the PIG output returned wrong # of rows",
      ROW_CNT, sh.out.get(0).toInteger());
  }
}
