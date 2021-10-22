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
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;

public class TestImportTsv {

  private static final String USER = System.properties["user.name"];
  private static final String TABLE = "movies";
  private static final String TABLE_COPY = "movies_copy";
  private static final String COLUMNS =
    "HBASE_ROW_KEY,general:title,release:date,release:video_date,IMDb:URL,genre:unknown,genre:Action,genre:Adventure,genre:Animation,genre:Children,genre:Comedy,genre:Crime,genre:Documentary,genre:Drama,genre:Fantasy,genre:Film-Noir,genre:Horror,genre:Musical,genre:Mystery,genre:Romance,genre:Sci-Fi,genre:Thriller,genre:War,genre:Western";
  private static final String DATADIR1 = "/user/$USER/movies_tsv";
  private static final String DATADIR2 = "/user/$USER/movies_psv";
  private static final String OUTDIR = "/user/$USER/import_movies_output";

  private static final String HBASE_HOME = System.getenv("HBASE_HOME");
  private static final String HBASE_RESOURCE = System.properties["test.resources.dir"]?
			"${System.properties['test.resources.dir']}": ".";

  static {
    assertNotNull("HBASE_HOME has to be set to run this test",
      HBASE_HOME);
  }
  private static String hbase_jar =
    JarContent.getJarName(HBASE_HOME, "hbase-.*(?<!tests).jar");
  static {
    assertNotNull("Can't find hbase.jar", hbase_jar);
  }
  private static final String HBASE_JAR = HBASE_HOME + "/" + hbase_jar;

  private static Shell sh = new Shell('/bin/bash -s');

  @BeforeClass
  public static void loadData() {
    sh.exec("hadoop fs -test -e $DATADIR1");
    if (sh.getRet() != 0) {
      sh.exec("hadoop fs -mkdir $DATADIR1");
      assertTrue("Unable to create directory $DATADIR1",
        sh.getRet() == 0);
    }
    sh.exec("hadoop fs -test -e $DATADIR2");
    if (sh.getRet() != 0) {
      sh.exec("hadoop fs -mkdir $DATADIR2");
      assertTrue("Unable to create directory $DATADIR2",
        sh.getRet() == 0);
    }
    // load data into HDFS
    sh.exec("hadoop fs -put $HBASE_RESOURCE/movies.tsv $DATADIR1/items",
      "hadoop fs -put $HBASE_RESOURCE/movies.psv $DATADIR2/items");
    assertTrue("setup failed", sh.getRet() == 0);
  }

  @AfterClass
  public static void cleanUp() {
    // delete data and junk from HDFS
    sh.exec("hadoop fs -rm -r -skipTrash $DATADIR1",
      "hadoop fs -rm -r -skipTrash $DATADIR2",
      "hadoop fs -rm -r -skipTrash '/user/$USER/hbase*'");
    assertTrue("teardown failed", sh.getRet() == 0);
  }

  @Before
  public void createTable() {
    // create the HBase table
    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);
    byte[] table = Bytes.toBytes(TABLE);
    byte[] family1 = Bytes.toBytes("general");
    byte[] family2 = Bytes.toBytes("release");
    byte[] family3 = Bytes.toBytes("IMDb");
    byte[] family4 = Bytes.toBytes("genre");
    HTableDescriptor htd = new HTableDescriptor(table);
    htd.addFamily(new HColumnDescriptor(family1));
    htd.addFamily(new HColumnDescriptor(family2));
    htd.addFamily(new HColumnDescriptor(family3));
    htd.addFamily(new HColumnDescriptor(family4));
    admin.createTable(htd);
  }

  @After
  public void deleteTablesAndOutDir() {
    // delete the HBase tables
    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);
    admin.disableTable(TABLE);
    admin.deleteTable(TABLE);
    if (admin.tableExists(TABLE_COPY)) {
      admin.disableTable(TABLE_COPY);
      admin.deleteTable(TABLE_COPY);
    }
    sh.exec("hadoop fs -test -e $OUTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $OUTDIR");
      assertTrue("Deletion of $OUTDIR from HDFS failed",
        sh.getRet() == 0);
    }
  }

  /**
   * Test vanilla use of importtsv with importtsv.bulk.output option
   * followed by completebulkload.
   */
  @Test
  public void testBulkImportTsv() throws Exception {
    String opts = "";
    String table = TABLE;
    String datadir = DATADIR1;
    doImport(opts, datadir);
    chmod();
    doLoad(table);
    countRows(table);
    checkRows(table);
  }

  /**
   * Test vanilla use of importtsv with importtsv.bulk.output option
   * followed by completebulkload to a new (non-existing) table.
   */
  @Test
  public void testLoadToNewTable() throws Exception {
    String opts = "";
    String table = TABLE_COPY;
    String datadir = DATADIR1;
    doImport(opts, datadir);
    chmod();
    doLoad(table);
    countRows(table);
    checkRows(table);
  }

  /**
   * Test use of importtsv with importtsv.bulk.output and importtsv.separator
   * options followed by completebulkload.
   */
  @Test
  public void testBulkImportNonTsv() throws Exception {
    String opts = "-Dimporttsv.separator='|'";
    String table = TABLE;
    String datadir = DATADIR2;
    doImport(opts, datadir);
    chmod();
    doLoad(table);
    countRows(table);
    checkRows(table);
  }

  // completebulkload will fail if we don't explicitly set up permissions
  // on the output dir of the importtsv job so that HBase can access its
  // contents
  private void chmod() {
    sh.exec("hadoop fs -chmod -R 777 $OUTDIR");
    assertEquals("chmod failed", 0, sh.getRet());
  }

  private void doImport(String opts, String datadir) {
    sh.exec("$HBASE_HOME/bin/hbase org.apache.hadoop.hbase.mapreduce.ImportTsv -Dimporttsv.bulk.output=$OUTDIR $opts -Dimporttsv.columns=$COLUMNS $TABLE $datadir");
    assertTrue("importtsv failed", sh.getRet() == 0);
  }

  private void doLoad(String table) {
    sh.exec("$HBASE_HOME/bin/hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles $OUTDIR $table");
    assertTrue("completebulkload failed", sh.getRet() == 0);
  }

  private void checkRows(String tableName) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    HBaseAdmin admin = new HBaseAdmin(conf);
    HTable table = new HTable(conf, Bytes.toBytes(tableName));
    new File("$HBASE_RESOURCE/movies.tsv").eachLine { line ->
      String[] tokens = line.split("\t");
      byte[] row = Bytes.toBytes(tokens[0]);
      Get g = new Get(row);
      Result result = table.get(g);
      byte[] value = result.getValue(Bytes.toBytes("general"), Bytes.toBytes("title"));
      String id = tokens[0];
      String title = tokens[1];
      String actual = Bytes.toString(value);
      System.out.println("$id: $title [$actual]");
      int i = 0;
      if (!title.equals(actual)) {
        System.out.println("!!! MISMATCH !!! expected $title, got $actual");
        i++;
      }
      assertEquals("checkRows failed", 0, i);
    }
  }

  private void countRows(String tableName) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    HTable table = new HTable(conf, Bytes.toBytes(tableName));
    int count = org.apache.bigtop.itest.hbase.util.HBaseTestUtil.countRows(table);
    assertEquals(1682, count);
  }
}
