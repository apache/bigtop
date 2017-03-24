/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hive;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hive.hcatalog.data.DefaultHCatRecord;
import org.apache.hive.hcatalog.data.HCatRecord;
import org.apache.hive.hcatalog.data.schema.HCatSchema;
import org.apache.hive.hcatalog.data.schema.HCatSchemaUtils;
import org.apache.hive.hcatalog.mapreduce.HCatInputFormat;
import org.apache.hive.hcatalog.mapreduce.HCatOutputFormat;
import org.apache.hive.hcatalog.mapreduce.OutputJobInfo;

import java.io.IOException;
import java.net.URI;
import java.util.StringTokenizer;

public class HCatalogMR extends Configured implements Tool {
    private final static String INPUT_SCHEMA = "bigtop.test.hcat.schema.input";
    private final static String OUTPUT_SCHEMA = "bigtop.test.hcat.schema.output";

    @Override
    public int run(String[] args) throws Exception {
        String inputTable = null;
        String outputTable = null;
        String inputSchemaStr = null;
        String outputSchemaStr = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-it")) {
                inputTable = args[i + 1];
            } else if (args[i].equalsIgnoreCase("-ot")) {
                outputTable = args[i + 1];
            } else if (args[i].equalsIgnoreCase("-is")) {
                inputSchemaStr = args[i + 1];
            } else if (args[i].equalsIgnoreCase("-os")) {
                outputSchemaStr = args[i + 1];
            }
        }

        Configuration conf = getConf();
        args = new GenericOptionsParser(conf, args).getRemainingArgs();

        conf.set(INPUT_SCHEMA, inputSchemaStr);
        conf.set(OUTPUT_SCHEMA, outputSchemaStr);

        Job job = new Job(conf, "bigtop_hcat_test");
        HCatInputFormat.setInput(job, "default", inputTable);

        job.setInputFormatClass(HCatInputFormat.class);
        job.setJarByClass(HCatalogMR.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(WritableComparable.class);
        job.setOutputValueClass(HCatRecord.class);
        HCatOutputFormat.setOutput(job, OutputJobInfo.create("default", outputTable, null));
        HCatOutputFormat.setSchema(job, HCatSchemaUtils.getHCatSchema(outputSchemaStr));
        job.setOutputFormatClass(HCatOutputFormat.class);

        return job.waitForCompletion(true) ? 0 : 1;


    }

    public static class Map extends Mapper<WritableComparable,
            HCatRecord, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private HCatSchema inputSchema = null;

        @Override
        protected void map(WritableComparable key, HCatRecord value, Context context)
                throws IOException, InterruptedException {
            if (inputSchema == null) {
                inputSchema =
                        HCatSchemaUtils.getHCatSchema(context.getConfiguration().get(INPUT_SCHEMA));
            }
            String line = value.getString("line", inputSchema);
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken());
                context.write(word, one);
            }
        }
    }

    public static class Reduce extends Reducer<Text, IntWritable, WritableComparable, HCatRecord> {
        private HCatSchema outputSchema = null;

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws
                IOException, InterruptedException {
            if (outputSchema == null) {
                outputSchema =
                        HCatSchemaUtils.getHCatSchema(context.getConfiguration().get(OUTPUT_SCHEMA));
            }
            int sum = 0;
            for (IntWritable i : values) {
                sum += i.get();
            }
            HCatRecord output = new DefaultHCatRecord(2);
            output.set("word", outputSchema, key);
            output.set("count", outputSchema, sum);
            context.write(null, output);
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new HCatalogMR(), args);
        System.exit(exitCode);
    }
}
