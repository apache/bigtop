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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.bigtop.bigpetstore.generator.BPSGenerator.props;
import org.apache.bigtop.bigpetstore.generator.util.State;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * run this test with vm options -Xms512m -Xmx1024m
 *
 */
public class TestPetStoreTransactionGeneratorJob {

    final static Logger log = LoggerFactory
            .getLogger(TestPetStoreTransactionGeneratorJob.class);

    @Test
    public void test() throws Exception {
        System.out.println("memory : " + Runtime.getRuntime().freeMemory()
                / 1000000);
        if (Runtime.getRuntime().freeMemory() / 1000000 < 75) {
            // throw new
            // RuntimeException("need more memory to run this test !");
        }
        int records = 20;
        /**
         * Setup configuration with prop.
         */
        Configuration c = new Configuration();
        c.setInt(props.bigpetstore_records.name(), records);

        /**
         * Run the job
         */
        Path output = new Path("petstoredata/" + (new Date()).toString());
        Job createInput = BPSGenerator.getCreateTransactionRecordsJob(output, c);
        createInput.submit();
        System.out.println(createInput);
        createInput.waitForCompletion(true);

        FileSystem fs = FileSystem.getLocal(new Configuration());

        /**
         * Read file output into string.
         */
        DataInputStream f = fs.open(new Path(output, "part-r-00000"));
        BufferedReader br = new BufferedReader(new InputStreamReader(f));
        String s;
        int recordsSeen = 0;
        boolean CTseen = false;
        boolean AZseen = false;

        // confirm that both CT and AZ are seen in the outputs.
        while (br.ready()) {
            s = br.readLine();
            System.out.println("===>" + s);
            recordsSeen++;
            if (s.contains(State.CT.name())) {
                CTseen = true;
            }
            if (s.contains(State.AZ.name())) {
                AZseen = true;
            }
        }

        // records seen should = 20
        assertEquals(records, recordsSeen);
        // Assert that a couple of the states are seen (todo make it
        // comprehensive for all states).
        assertTrue(CTseen);
        assertTrue(AZseen);
        log.info("Created " + records + " , file was "
                + fs.getFileStatus(new Path(output, "part-r-00000")).getLen()
                + " bytes.");
    }
}
