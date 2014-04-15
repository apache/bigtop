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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.bigtop.bigpetstore.generator.TransactionIteratorFactory.STATE;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

/**
 * What does an `InputSplit` actually do? From the Javadocs, it looks like ...
 * absolutely nothing.
 *
 * Note: for some reason, you *have* to implement Writable, even if your methods
 * do nothing, or you will got strange and un-debuggable null pointer
 * exceptions.
 */
public class PetStoreTransactionInputSplit extends InputSplit implements
        Writable {

    public PetStoreTransactionInputSplit() {
    }

    public int records;
    public STATE state;

    public PetStoreTransactionInputSplit(int records, STATE state) {
        this.records = records;
        this.state = state;
    }

    public void readFields(DataInput arg0) throws IOException {
        records = arg0.readInt();
        state = STATE.valueOf(arg0.readUTF());
    }

    public void write(DataOutput arg0) throws IOException {
        arg0.writeInt(records);
        arg0.writeUTF(state.name());
    }

    @Override
    public String[] getLocations() throws IOException, InterruptedException {
        return new String[] {};
    }

    @Override
    public long getLength() throws IOException, InterruptedException {
        return 100;
    }
}