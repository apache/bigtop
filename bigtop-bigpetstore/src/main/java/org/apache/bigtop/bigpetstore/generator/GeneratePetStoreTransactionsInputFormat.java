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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.bigtop.bigpetstore.generator.TransactionIteratorFactory.KeyVal;
import org.apache.bigtop.bigpetstore.generator.TransactionIteratorFactory.STATE;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

/**
 * A simple input split that fakes input.
 */
public class GeneratePetStoreTransactionsInputFormat extends
        FileInputFormat<Text, Text> {

    @Override
    public RecordReader<Text, Text> createRecordReader(
            final InputSplit inputSplit, TaskAttemptContext arg1)
            throws IOException, InterruptedException {
        return new RecordReader<Text, Text>() {

            @Override
            public void close() throws IOException {

            }

            /**
             * We need the "state" information to generate records. - Each state
             * has a probability associated with it, so that our data set can be
             * realistic (i.e. Colorado should have more transactions than rhode
             * island).
             *
             * - Each state also will its name as part of the key.
             *
             * - This task would be distributed, for example, into 50 nodes on a
             * real cluster, each creating the data for a given state.
             */

            // String storeCode = ((Split) inputSplit).storeCode;
            int records = ((PetStoreTransactionInputSplit) inputSplit).records;
            Iterator<KeyVal<String, String>> data = (new TransactionIteratorFactory(
                    records, ((PetStoreTransactionInputSplit) inputSplit).state))
                    .getData();
            KeyVal<String, String> currentRecord;

            @Override
            public Text getCurrentKey() throws IOException,
                    InterruptedException {
                return new Text(currentRecord.key);
            }

            @Override
            public Text getCurrentValue() throws IOException,
                    InterruptedException {
                return new Text(currentRecord.val);
            }

            @Override
            public void initialize(InputSplit arg0, TaskAttemptContext arg1)
                    throws IOException, InterruptedException {
            }

            @Override
            public boolean nextKeyValue() throws IOException,
                    InterruptedException {
                if (data.hasNext()) {
                    currentRecord = data.next();
                    return true;
                }
                return false;
            }

            @Override
            public float getProgress() throws IOException, InterruptedException {
                return 0f;
            }

        };
    }

    public enum props {
        // bigpetstore_splits,
        bigpetstore_records
    }

    @Override
    public List<InputSplit> getSplits(JobContext arg) throws IOException {
        int num_records_desired = arg
                .getConfiguration()
                .getInt(GeneratePetStoreTransactionsInputFormat.props.bigpetstore_records
                        .name(), -1);
        if (num_records_desired == -1) {
            throw new RuntimeException(
                    "# of total records not set in configuration object: "
                            + arg.getConfiguration());
        }

        ArrayList<InputSplit> list = new ArrayList<InputSplit>();

        /**
         * Generator class will take a state as input and generate all the data
         * for that state.
         */
        for (TransactionIteratorFactory.STATE s : STATE.values()) {
            PetStoreTransactionInputSplit split = new PetStoreTransactionInputSplit(
                    (int) (Math.ceil(num_records_desired * s.probability)), s);
            System.out.println(s + " _ " + split.records);
            list.add(split);
        }
        return list;
    }
}