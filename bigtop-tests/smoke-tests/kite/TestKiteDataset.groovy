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


package org.apache.bigtop.itest.kite

import static org.junit.Assert.assertTrue

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import java.lang.reflect.Constructor;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.runner.RunWith


class TestKiteDataset {
  static private Log LOG = LogFactory.getLog(Object.class);

  static Shell sh = new Shell("/bin/bash -s");


  @AfterClass
  public static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash /tmp/kite");
    sh.exec("rm -rf *.avsc");

  }

  @BeforeClass
  static void setUp() {

  }

  @Test
  void test() {
    // Infer the Schema
    sh.exec("kite-dataset csv-schema sandwiches.csv --class Sandwich -o sandwich.avsc");

    // kite-dataset create sandwiches -s sandwich.avsc
    sh.exec("kite-dataset create dataset:hdfs:/tmp/kite/sandwiches -s sandwich.avsc");

    // Import the CSV Data
    sh.exec("kite-dataset csv-import sandwiches.csv dataset:hdfs:/tmp/kite/sandwiches");

    // Show the Results
    sh.exec("kite-dataset show dataset:hdfs:/tmp/kite/sandwiches");
    sh.exec("kite-dataset show dataset:hdfs:/tmp/kite/sandwiches -n 1");

    String r = sh.out;
    LOG.info(r);
    assertTrue("Incorrect record", r.contains("Reuben"));

  }
}
