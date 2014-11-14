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
package org.apache.bigtop.bigpetstore.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.mapreduce.Job;

/**
 * Dev utilities for testing arguments etc...
 */
public class DeveloperTools {

    /**
     * Validates that the expected args are present in the "args" array.
     * Just some syntactic sugar for good arg error handling.
     * @param args
     * @param expected arguments.
     */
    public static void validate(String[] args, String... expected) {
        int i=-1;
        try{
            for(i = 0 ; i < expected.length ; i++) {
                System.out.println("VALUE OF " + expected[i] + " = " + args[i]);
            }
        }
        catch(Throwable t) {
            System.out.println("Argument " + i + " not available.");
            System.out.println("We expected " + expected.length + " arguments for this phase");
        }


    }
    public static void main(String[] args) throws Exception {
        Log LOG = LogFactory.getLog(Job.class);
    }

}