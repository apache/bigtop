package org.apache.bigtop.bigpetstore.qstream;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

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
public class FileLoadGen extends LoadGen{

    Path path;

    public FileLoadGen(int nStores, int nCustomers, double simulationLength, long seed, Path outputDir) throws Throwable {
        super(nStores, nCustomers, simulationLength, seed);
        path=outputDir;
    }

    public LinkedBlockingQueue<Transaction> startWriteQueue(final int milliseconds){
        if(! path.toFile().isDirectory()) {
            throw new RuntimeException("Input for the queue Should be a directory! Files will be transactions0.txt, transactions1.txt, and so on.");
        }

        /**
         * Write queue.
         */
        final LinkedBlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<Transaction>(getQueueSize());
        new Thread(){
            @Override
            public void run() {
                int fileNumber=0;
                while(true){
                    waitFor(milliseconds, transactionQueue);
                    System.out.println("Clearing " + transactionQueue.size() + " elements");
                    Stack<Transaction> transactionsToWrite = new Stack<Transaction>();
                    transactionQueue.drainTo(transactionsToWrite);
                    StringBuffer lines = new StringBuffer();
                    try{
                        while(!transactionsToWrite.isEmpty()){
                            lines.append(Utils.toJson(transactionsToWrite.pop())+"\n");
                            total++;
                        }
                        Path outputFile = Paths.get(path.toFile().getAbsolutePath(), "/transactions" + fileNumber++ + ".txt");
                        Files.write(outputFile, lines.toString().getBytes());
                        System.out.println("WRITING FILE to " + outputFile.toFile().length() + "bytes -> " + outputFile.toFile().getAbsolutePath());
                    }
                    catch(Throwable t){
                        t.printStackTrace();
                    }
                    System.out.println(
                            "TRANSACTIONS SO FAR " + total++ +" RATE " + (total/((System.currentTimeMillis()-startTime)/1000) + " per second "));
                }
            }
        }.start();
        return transactionQueue;
    }

}
