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

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.PerformanceEvaluation;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat;
import org.apache.hadoop.hbase.mapreduce.NMapInputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class IncrementalPELoad extends Configured implements Tool {

  /**
   * Simple mapper that makes KeyValue output.
   */
  public static class RandomKVGeneratingMapper
      extends Mapper<NullWritable, NullWritable, ImmutableBytesWritable, KeyValue> {

    private static final int ROWSPERSPLIT = 1024;
    private static final byte[][] FAMILIES
        = {Bytes.add(PerformanceEvaluation.FAMILY_NAME, Bytes.toBytes("-A")),
        Bytes.add(PerformanceEvaluation.FAMILY_NAME, Bytes.toBytes("-B"))};

    private int keyLength;
    private static final int KEYLEN_DEFAULT = 10;
    private static final String KEYLEN_CONF = "randomkv.key.length";

    private int valLength;
    private static final int VALLEN_DEFAULT = 10;
    private static final String VALLEN_CONF = "randomkv.val.length";

    @Override
    protected void setup(Context context)
        throws IOException, InterruptedException {
      super.setup(context);

      Configuration conf = context.getConfiguration();
      keyLength = conf.getInt(KEYLEN_CONF, KEYLEN_DEFAULT);
      valLength = conf.getInt(VALLEN_CONF, VALLEN_DEFAULT);
    }

    protected void map(NullWritable n1, NullWritable n2,
                       Mapper<NullWritable, NullWritable,
                           ImmutableBytesWritable, KeyValue>.Context context)
        throws java.io.IOException, InterruptedException {

      byte keyBytes[] = new byte[keyLength];
      byte valBytes[] = new byte[valLength];

      int taskId = context.getTaskAttemptID().getTaskID().getId();
      assert taskId < Byte.MAX_VALUE : "Unit tests dont support > 127 tasks!";

      Random random = new Random();
      for (int i = 0; i < ROWSPERSPLIT; i++) {

        random.nextBytes(keyBytes);
        // Ensure that unique tasks generate unique keys
        keyBytes[keyLength - 1] = (byte) (taskId & 0xFF);
        random.nextBytes(valBytes);
        ImmutableBytesWritable key = new ImmutableBytesWritable(keyBytes);

        for (byte[] family : FAMILIES) {
          KeyValue kv = new KeyValue(keyBytes, family,
              PerformanceEvaluation.QUALIFIER_NAME, valBytes);
          context.write(key, kv);
        }
      }
    }
  }

  public int run(String[] args) throws Exception {
    Configuration conf = getConf();
    Job job = new Job(conf, "testMRIncrementalLoad");
    job.setJarByClass(IncrementalPELoad.class);
    job.setInputFormatClass(NMapInputFormat.class);
    job.setMapperClass(IncrementalPELoad.RandomKVGeneratingMapper.class);
    job.setMapOutputKeyClass(ImmutableBytesWritable.class);
    job.setMapOutputValueClass(KeyValue.class);
    HTable table = new HTable(conf, Bytes.toBytes(args[0]));
    HFileOutputFormat.configureIncrementalLoad(job, table);
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    return job.waitForCompletion(true) ? 0 : 1;
  }

  public static void main(String[] args) throws Exception {
    int exitCode = ToolRunner.run(new IncrementalPELoad(), args);
    System.exit(exitCode);
  }
}
