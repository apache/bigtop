/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bigtop.bigpetstore.qstream;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Transaction;
import org.apache.http.HttpResponse;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * HTTP Consumer.  provides a write queue and empties it every so often.
 * TODO, make the queue size configurable.
 */
public class HttpLoadGen extends LoadGen {

    String path; // i.e.  "http://localhost:3000";

    public HttpLoadGen(int nStores, int nCustomers, double simulationLength, long seed, URL u) throws Throwable{
        super(nStores, nCustomers, simulationLength, seed);
        path=u.toString();
    }

    /**
     * Appends via REST calls.
     */
    public LinkedBlockingQueue<Transaction> startWriteQueue(final int milliseconds) {
        /**
         * Write queue.   Every 5 seconds, write
         */
        final LinkedBlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<Transaction>(getQueueSize());
        new Thread() {
            @Override
            public void run() {
                int fileNumber = 0;
                while (true) {
                    waitFor(milliseconds, transactionQueue);
                    System.out.println("CLEARING " + transactionQueue.size() + " elements from queue.");
                    Stack<Transaction> transactionsToWrite = new Stack<Transaction>();

                    transactionQueue.drainTo(transactionsToWrite);

                    /**
                     * pop transactions from the queue, and sent them over http as json.
                     */
                    while (!transactionsToWrite.isEmpty()) {
                        try {
                            String trAsJson = URLEncoder.encode(Utils.toJson(transactionsToWrite.pop()));

                            /**
                             * i.e. wget http://localhost:3000/rpush/guestbook/{"name":"cos boudnick", "state":"...",...}
                             */
                            HttpResponse resp=Utils.get(path + "/" + trAsJson);
                            if(total%20==0) System.out.println("wrote customer " + trAsJson);
                            total++;
                        }
                        catch (Throwable t) {
                            System.err.println("transaction failed.... !");
                            t.printStackTrace();
                        }
                        System.out.println("TRANSACTIONS SO FAR " + total++ + " RATE " + total / ((System.currentTimeMillis() - startTime) / 1000));
                    }
                }
            }
        }.start();

        return transactionQueue;
    }
}
