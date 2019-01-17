/*
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
package org.apache.bigtop.itest.giraph.smoke

import static org.junit.Assert.assertEquals

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell
import org.junit.Test

public class TestGiraphSmoke {
  static String testDir = "test.giraphsmoke." + (new Date().getTime())
  static Shell sh = new Shell("/bin/bash -s")

  @Test(timeout = 300000L)
  public void testPageRankBenchmark() {
    sh.exec("hadoop jar /usr/lib/giraph/giraph-examples*with-dependencies.jar"
      + " org.apache.giraph.benchmark.PageRankBenchmark"
      + " -v  "        // verbose
      + " -e 1"        // edges per vertex
      + " -s 3"        // number of supersteps
      + " -V 100000"   // aggregate vertices
      + " -w 3"        // workers
    )
    assertEquals("running PageRankBenchmark failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testRandomMessageBenchmark() {
    sh.exec("hadoop jar /usr/lib/giraph/giraph-examples*with-dependencies.jar"
      + " org.apache.giraph.benchmark.RandomMessageBenchmark"
      + " -v  "        // verbose
      + " -e 1"        // edges per vertex
      + " -s 3"        // number of supersteps
      + " -V 100000"   // aggregate vertices
      + " -w 3"        // workers
      + " -n 10"       // Number of messages per edge
      + " -b 100"      // size of each message in bytes
    )
    assertEquals("running RandomMessageBenchmark failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testSimpleShortestPathsComputation() {
    sh.exec("echo -e '[0,0,[[1,1],[3,3]]]\n[1,0,[[0,1],[2,2],[3,1]]]\n[2,0,[[1,2],[4,4]]]\n[3,0,[[0,3],[1,1],[4,4]]]\n[4,0,[[3,4],[2,4]]]' > ./giraphTest.txt");

    sh.exec("hadoop fs -mkdir ${testDir}");

    sh.exec("hadoop fs -copyFromLocal ./giraphTest.txt ${testDir}/giraphTest.txt");

    sh.exec("hadoop jar /usr/lib/giraph/giraph-examples*with-dependencies.jar"
      + " org.apache.giraph.GiraphRunner"
      + " org.apache.giraph.examples.SimpleShortestPathsComputation"
      + " -vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat"
      + " -vip ${testDir}/giraphTest.txt"
      + " -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat"
      + " -op ${testDir}/shortestpaths"
      + " -w 3"
    )
    assertEquals("running SimpleShortestPathsComputation failed", 0, sh.getRet());
  }
}
