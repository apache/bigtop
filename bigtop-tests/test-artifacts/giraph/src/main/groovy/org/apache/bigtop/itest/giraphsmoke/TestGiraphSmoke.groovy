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
  static String runnerScript = "HADOOP_CLASSPATH=/etc/giraph/conf hadoop jar"
  static String testDir = "test.giraphsmoke." + (new Date().getTime())

  static String giraphHome = System.getProperty('GIRAPH_HOME', '/usr/lib/giraph')
  static String giraphJar = "${giraphHome}/giraph-jar-with-dependencies.jar"

  static Shell sh = new Shell("/bin/bash -s")

  @Test(timeout = 300000L)
  public void testPageRankBenchmark() {
    sh.exec("${runnerScript} ${giraphJar}"
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
    sh.exec("${runnerScript} ${giraphJar}"
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
  public void testSimpleCheckpointVertex() {
    sh.exec("hadoop fs -rmr ${testDir}");
    sh.exec("${runnerScript} ${giraphJar}"
      + " org.apache.giraph.examples.SimpleCheckpointVertex"
      + " -v  "        // verbose
      + " -s 3"        // number of supersteps
      + " -w 3"        // workers
      + " -o ${testDir}"
    )
    assertEquals("running SimpleCheckpointVertex failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testSimpleVertexWithWorkerContext() {
    sh.exec("hadoop fs -rmr ${testDir}");
    sh.exec("${runnerScript} ${giraphJar}"
      + " org.apache.giraph.examples.SimpleVertexWithWorkerContext"
      + " ${testDir} 3"
    )
    assertEquals("running SimpleCheckpointVertex failed", 0, sh.getRet());
  }

  @Test(timeout = 300000L)
  public void testSimpleShortestPathsVertex() {
    // A graph definition: 
    //   [vertex id, vertex value, [[edge1, value1], .. [edgeN, valueN]]] 
    List graphDescription = [[0, 0, [[1, 1], [2, 2]]],
      [1, 1, [[2, 2], [3, 3]]],
      [2, 2, [[3, 3], [4, 4]]],
      [3, 3, [[4, 4], [5, 5]]],
      [4, 4, [[5, 5], [0, 0]]],
      [5, 5, [[0, 0], [1, 1]]]];
    int partitionSize = 2;

    sh.exec("hadoop fs -rmr ${testDir}",
      "hadoop fs -mkdir ${testDir}/input");

    for (int i = 0; i < graphDescription.size(); i += partitionSize) {
      String part = graphDescription[i..(i + partitionSize - 1)].join("\n");
      int partId = i / partitionSize;
      sh.exec("hadoop fs -put <(echo '${part}') ${testDir}/input/part-m-${partId}");
    }

    sh.exec("${runnerScript} ${giraphJar}"
      + " org.apache.giraph.examples.SimpleShortestPathsVertex"
      + " ${testDir}/input"
      + " ${testDir}/output"
      + " 0 ${graphDescription.size() / partitionSize}"
    )
    assertEquals("running SimpleShortestPathsVertex failed", 0, sh.getRet());
  }
}
