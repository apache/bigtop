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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bigtop.itest.hbase.util.HBaseTestUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.NMapInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestLoadAndVerify  extends Configured implements Tool {
  private static final String TEST_NAME = "TestLoadAndVerify";
  private static final byte[] TEST_FAMILY = Bytes.toBytes("f1");
  private static final byte[] TEST_QUALIFIER = Bytes.toBytes("q1");

  private static final String NUM_TO_WRITE_KEY =
    "loadmapper.num_to_write";
  private static final long NUM_TO_WRITE_DEFAULT = 100*1000;

  private static final String TABLE_NAME_KEY = "loadmapper.table";
  private static final String TABLE_NAME_DEFAULT = "table";

  private static final String NUM_BACKREFS_KEY = "loadmapper.backrefs";
  private static final int NUM_BACKREFS_DEFAULT = 50;

  private static final int NUM_TASKS = 200;
  private static final int NUM_REDUCE_TASKS = 35;

  private static final int SCANNER_CACHING = 5000;

  private enum Counters {
    ROWS_WRITTEN,
    REFERENCES_WRITTEN,
    REFERENCES_CHECKED;
  }

  /**
   * Converts a "long" value between endian systems.
   * Borrowed from Apache Commons IO
   * @param value value to convert
   * @return the converted value
   */
  public static long swapLong(long value)
  {
    return
      ( ( ( value >> 0 ) & 0xff ) << 56 ) +
      ( ( ( value >> 8 ) & 0xff ) << 48 ) +
      ( ( ( value >> 16 ) & 0xff ) << 40 ) +
      ( ( ( value >> 24 ) & 0xff ) << 32 ) +
      ( ( ( value >> 32 ) & 0xff ) << 24 ) +
      ( ( ( value >> 40 ) & 0xff ) << 16 ) +
      ( ( ( value >> 48 ) & 0xff ) << 8 ) +
      ( ( ( value >> 56 ) & 0xff ) << 0 );
  }

  public static class LoadMapper
      extends Mapper<NullWritable, NullWritable, NullWritable, NullWritable>
  {
    private long recordsToWrite;
    private HTable table;
    private Configuration conf;
    private int numBackReferencesPerRow;
    private String shortTaskId;

    private Random rand = new Random();

    private Counter rowsWritten, refsWritten;

    @Override
    public void setup(Context context) throws IOException {
      conf = context.getConfiguration();
      recordsToWrite = conf.getLong(NUM_TO_WRITE_KEY, NUM_TO_WRITE_DEFAULT);
      String tableName = conf.get(TABLE_NAME_KEY, TABLE_NAME_DEFAULT);
      numBackReferencesPerRow = conf.getInt(NUM_BACKREFS_KEY, NUM_BACKREFS_DEFAULT);
      table = new HTable(conf, tableName);
      table.setWriteBufferSize(4*1024*1024);
      table.setAutoFlush(false);

      String taskId = conf.get("mapred.task.id");
      Matcher matcher = Pattern.compile(".+_m_(\\d+_\\d+)").matcher(taskId);
      if (!matcher.matches()) {
        throw new RuntimeException("Strange task ID: " + taskId);
      }
      shortTaskId = matcher.group(1);

      rowsWritten = context.getCounter(Counters.ROWS_WRITTEN);
      refsWritten = context.getCounter(Counters.REFERENCES_WRITTEN);
    }

    @Override
    public void cleanup(Context context) throws IOException {
      table.flushCommits();
      table.close();
    }

    @Override
    protected void map(NullWritable key, NullWritable value, 
        Context context) throws IOException, InterruptedException {

      String suffix = "/" + shortTaskId;
      byte[] row = Bytes.add(new byte[8], Bytes.toBytes(suffix));

      int BLOCK_SIZE = (int)(recordsToWrite / 100);

      for (long i = 0; i < recordsToWrite;) {
        long blockStart = i;
        for (long idxInBlock = 0;
             idxInBlock < BLOCK_SIZE && i < recordsToWrite;
             idxInBlock++, i++) {

          long byteSwapped = swapLong(i);
          Bytes.putLong(row, 0, byteSwapped);

          Put p = new Put(row);
          p.add(TEST_FAMILY, TEST_QUALIFIER, HConstants.EMPTY_BYTE_ARRAY);
          if (blockStart > 0) {
            for (int j = 0; j < numBackReferencesPerRow; j++) {
              long referredRow = blockStart - BLOCK_SIZE + rand.nextInt(BLOCK_SIZE);
              Bytes.putLong(row, 0, swapLong(referredRow));
              p.add(TEST_FAMILY, row, HConstants.EMPTY_BYTE_ARRAY);
            }
            refsWritten.increment(1);
          }
          rowsWritten.increment(1);
          table.put(p);

          if (i % 100 == 0) {
            context.setStatus("Written " + i + "/" + recordsToWrite + " records");
            context.progress();
          }
        }
        // End of block, flush all of them before we start writing anything
        // pointing to these!
        table.flushCommits();
      }
    }
  }

  public static class VerifyMapper extends TableMapper<BytesWritable, BytesWritable> {
    static final BytesWritable EMPTY = new BytesWritable(HConstants.EMPTY_BYTE_ARRAY);

    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context)
        throws IOException, InterruptedException {
      BytesWritable bwKey = new BytesWritable(key.get());
      BytesWritable bwVal = new BytesWritable();
      for (KeyValue kv : value.list()) {
        if (Bytes.compareTo(TEST_QUALIFIER, 0, TEST_QUALIFIER.length,
                            kv.getBuffer(), kv.getQualifierOffset(), kv.getQualifierLength()) == 0) {
          context.write(bwKey, EMPTY);          
        } else {
          bwVal.set(kv.getBuffer(), kv.getQualifierOffset(), kv.getQualifierLength());
          context.write(bwVal, bwKey);
        }
      }
    }
  }

  public static class VerifyReducer extends Reducer<BytesWritable, BytesWritable, Text, Text> {
    private Counter refsChecked;

    @Override
    public void setup(Context context) throws IOException {
      refsChecked = context.getCounter(Counters.REFERENCES_CHECKED);
    }

    @Override
    protected void reduce(BytesWritable referredRow, Iterable<BytesWritable> referrers,
        VerifyReducer.Context ctx) throws IOException, InterruptedException {
      boolean gotOriginalRow = false;
      int refCount = 0;

      for (BytesWritable ref : referrers) {
        if (ref.getLength() == 0) {
          assert !gotOriginalRow;
          gotOriginalRow = true;
        } else {
          refCount++;
        }
      }
      refsChecked.increment(refCount);

      if (!gotOriginalRow) {
        String parsedRow = makeRowReadable(referredRow.getBytes(), referredRow.getLength());
        String binRow = Bytes.toStringBinary(referredRow.getBytes(), 0, referredRow.getLength()); 
        ctx.write(new Text(binRow), new Text(parsedRow));
      }
    }

    private String makeRowReadable(byte[] bytes, int length) {
      long rowIdx = swapLong(Bytes.toLong(bytes, 0));
      String suffix = Bytes.toString(bytes, 8, length - 8);

      return "Row #" + rowIdx + " suffix " + suffix;
    }
  }

  private void doLoad(Configuration conf, HTableDescriptor htd) throws Exception {
    Path outputDir = 
      new Path(HBaseTestUtil.getMROutputDir(TEST_NAME),
          "load-output");

    NMapInputFormat.setNumMapTasks(conf, NUM_TASKS);
    conf.set(TABLE_NAME_KEY, htd.getNameAsString());

    Job job = new Job(conf);
    job.setJobName(TEST_NAME + " Load for " + htd.getNameAsString());
    job.setJarByClass(this.getClass());
    job.setMapperClass(LoadMapper.class);
    job.setInputFormatClass(NMapInputFormat.class);
    job.setNumReduceTasks(0);
    FileOutputFormat.setOutputPath(job, outputDir);

    TableMapReduceUtil.addDependencyJars(job);
    TableMapReduceUtil.addDependencyJars(
        job.getConfiguration(), HTable.class, Lists.class);
    assertTrue(job.waitForCompletion(true));
  }

  private void doVerify(Configuration conf, HTableDescriptor htd) throws Exception {
    Path outputDir = 
      new Path(HBaseTestUtil.getMROutputDir(TEST_NAME),
          "verify-output");

    Job job = new Job(conf);
    job.setJarByClass(this.getClass());
    job.setJobName(TEST_NAME + " Verification for " + htd.getNameAsString());

    Scan scan = new Scan();

    TableMapReduceUtil.initTableMapperJob(
        htd.getNameAsString(), scan, VerifyMapper.class,
        BytesWritable.class, BytesWritable.class, job);
    TableMapReduceUtil.setScannerCaching(job, SCANNER_CACHING);

    job.setReducerClass(VerifyReducer.class);
    job.setNumReduceTasks(NUM_REDUCE_TASKS);
    FileOutputFormat.setOutputPath(job, outputDir);
    assertTrue(job.waitForCompletion(true));

    long numOutputRecords = job.getCounters().findCounter(
        org.apache.hadoop.mapred.Task.Counter.REDUCE_OUTPUT_RECORDS).getValue();
    assertEquals(0, numOutputRecords);
  }

  @Test
  public void testLoadAndVerify() throws Exception {
    HTableDescriptor htd =
      HBaseTestUtil.createTestTableDescriptor(TEST_NAME, TEST_FAMILY);
    HBaseAdmin admin = HBaseTestUtil.getAdmin();
    int numPreCreate = 40;
    admin.createTable(htd, Bytes.toBytes(0L), Bytes.toBytes(-1L), numPreCreate);

    Configuration conf = HBaseConfiguration.create();

    doLoad(conf, htd);
    doVerify(conf, htd);

    // Only disable and drop if we succeeded to verify - otherwise it's useful
    // to leave it around for post-mortem
    deleteTable(admin, htd);
  }

  private void deleteTable(HBaseAdmin admin, HTableDescriptor htd) 
    throws IOException, InterruptedException {
    // Use disableTestAsync because disable can take a long time to complete
    System.out.print("Disabling table " + htd.getNameAsString() +" ");
    admin.disableTableAsync(htd.getName());
    
    long start = System.currentTimeMillis();
    // NOTE tables can be both admin.isTableEnabled=false and 
    // isTableDisabled=false, when disabling must use isTableDisabled!
    while (!admin.isTableDisabled(htd.getName())) {
      System.out.print(".");
      Thread.sleep(1000);
    }
    long delta = System.currentTimeMillis() - start;
    System.out.println(" " + delta +" ms");
    System.out.println("Deleting table " + htd.getNameAsString() +" ");
    admin.deleteTable(htd.getName());
  }
  
  public void usage() {
    System.err.println(this.getClass().getSimpleName() + " [-Doptions] <load|verify|loadAndVerify>");
    System.err.println("  Loads a table with row dependencies and verifies the dependency chains");
    System.err.println("Options");
    System.err.println("  -Dloadmapper.table=<name>        Table to write/verify (default autogen)");
    System.err.println("  -Dloadmapper.backrefs=<n>        Number of backreferences per row (default 50)");
    System.err.println("  -Dloadmapper.num_to_write=<n>    Number of rows per mapper (default 100,000 per mapper)");
    System.err.println("  -Dloadmapper.deleteAfter=<bool>  Delete after a successful verify (default true)");
    System.err.println("  -Dloadmapper.numPresplits=<n>    Number of presplit regions to start with (default 40)");

  }
  
  public int run(String argv[]) throws Exception {
    if (argv.length < 1 || argv.length > 1) {
      usage();
      return 1;
    }

    boolean doLoad = false;
    boolean doVerify = false;
    boolean doDelete = getConf().getBoolean("loadmapper.deleteAfter",true);
    int numPresplits = getConf().getInt("loadmapper.numPresplits", 40);

    if (argv[0].equals("load")) {
      doLoad = true;
    } else if (argv[0].equals("verify")) {
      doVerify= true;
    } else if (argv[0].equals("loadAndVerify")) {
      doLoad=true;
      doVerify= true;
    } else {
      System.err.println("Invalid argument " + argv[0]);
      usage();
      return 1;
    }

    // create HTableDescriptor for specified table
    String table = getConf().get(TABLE_NAME_KEY, "");
    HTableDescriptor htd ;
    if ("".equals(table)) {
      // Just like the unit test.
      htd = HBaseTestUtil.createTestTableDescriptor(TEST_NAME, TEST_FAMILY);
    } else {
      // overridden by the user.
      htd = new HTableDescriptor(table);
      htd.addFamily(new HColumnDescriptor(TEST_FAMILY));
    }

    TestLoadAndVerify verify = new TestLoadAndVerify();

    HBaseAdmin admin = new HBaseAdmin(getConf());
    if (doLoad) {
      admin.createTable(htd, Bytes.toBytes(0L), Bytes.toBytes(-1L), numPresplits);
      verify.doLoad(getConf(), htd);
    }
    if (doVerify) {
      verify.doVerify(getConf(), htd);
      if (doDelete) {
        deleteTable(admin, htd);
      }
    }
    return 0;
  }

  public static void main(String argv[]) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    int ret = ToolRunner.run(conf, new TestLoadAndVerify(), argv);
    System.exit(ret);
  }
}
