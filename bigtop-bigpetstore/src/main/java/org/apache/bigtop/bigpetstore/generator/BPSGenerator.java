/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.bigpetstore.generator;

import java.io.IOException;
import java.util.Date;

import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants;
import org.apache.bigtop.bigpetstore.util.DeveloperTools;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.MultipleOutputs;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.bigtop.bigpetstore.generator.PetStoreTransactionsInputFormat.props;

/**
 * This is a mapreduce implementation of a generator of a large sentiment
 * analysis data set. The scenario is as follows:
 *
 * The number of records will (roughly) correspond to the output size - each
 * record is about 80 bytes.
 *
 * 1KB set bigpetstore_records=10 1MB set bigpetstore_records=10,000 1GB set
 * bigpetstore_records=10,000,000 1TB set bigpetstore_records=10,000,000,000
 */
public class BPSGenerator {

  public static final int DEFAULT_NUM_RECORDS = 100;

  final static Logger log = LoggerFactory.getLogger(BPSGenerator.class);

  public enum props {
    bigpetstore_records
  }

  public static Job createJob(Path output, int records) throws IOException {
    Configuration c = new Configuration();
    c.setInt(props.bigpetstore_records.name(), DEFAULT_NUM_RECORDS);
    return getCreateTransactionRecordsJob(output, c);
  }

  public static Job getCreateTransactionRecordsJob(Path outputDir, Configuration conf)
          throws IOException {
    Job job = new Job(conf, "PetStoreTransaction_ETL_" + System.currentTimeMillis());
    // recursively delete the data set if it exists.
    FileSystem.get(outputDir.toUri(), conf).delete(outputDir, true);
    job.setJarByClass(BPSGenerator.class);
    job.setMapperClass(MyMapper.class);
    // use the default reducer
    // job.setReducerClass(PetStoreTransactionGeneratorJob.Red.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);
    job.setInputFormatClass(PetStoreTransactionsInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    FileOutputFormat.setOutputPath(job, outputDir);
    return job;
  }

  public static class MyMapper extends Mapper<Text, Text, Text, Text> {
    @Override
    protected void setup(Context context) throws IOException,
    InterruptedException {
      super.setup(context);
    }

    protected void map(Text key, Text value, Context context)
            throws java.io.IOException, InterruptedException {
      context.write(key, value);
    }
  }

  public static void main(String args[]) throws Exception {
    if (args.length != 2) {
      System.err.println("USAGE : [number of records] [output path]");
      System.exit(0);
    } else {
      Configuration conf = new Configuration();
      DeveloperTools.validate(args, "# of records", "output path");
      conf.setInt(PetStoreTransactionsInputFormat.props.bigpetstore_records.name(),
              Integer.parseInt(args[0]));
      getCreateTransactionRecordsJob(new Path(args[1]), conf).waitForCompletion(true);
    }
  }
}