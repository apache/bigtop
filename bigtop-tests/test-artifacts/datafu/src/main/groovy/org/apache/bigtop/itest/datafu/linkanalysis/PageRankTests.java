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

package org.apache.bigtop.itest.datafu.linkanalysis;


import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.junit.Test;


import org.apache.bigtop.itest.datafu.linkanalysis.PageRankTest;
import org.apache.bigtop.itest.datafu.PigTests;

public class PageRankTests extends PigTests
{
  @Test
  public void pigPageRankTest() throws Exception
  {
    PigTest test = createPigTest("datafu/linkanalysis/pageRankTest.pig");

    String[] edges = PageRankTest.getWikiExampleEdges();

    Map<String,Integer> nodeIds = new HashMap<String,Integer>();
    Map<Integer,String> nodeIdsReversed = new HashMap<Integer,String>();
    Map<String,Float> expectedRanks = PageRankTest.parseExpectedRanks(PageRankTest.getWikiExampleExpectedRanks());

    File f = new File(System.getProperty("user.dir"), "input").getAbsoluteFile();
    if (f.exists())
    {
      f.delete();
    }

    FileWriter writer = new FileWriter(f);
    BufferedWriter bufferedWriter = new BufferedWriter(writer);

    for (String edge : edges)
    {
      String[] edgeParts = edge.split(" ");
      String source = edgeParts[0];
      String dest = edgeParts[1];
      if (!nodeIds.containsKey(source))
      {
        int id = nodeIds.size();
        nodeIds.put(source,id);
        nodeIdsReversed.put(id, source);
      }
      if (!nodeIds.containsKey(dest))
      {
        int id = nodeIds.size();
        nodeIds.put(dest,id);
        nodeIdsReversed.put(id, dest);
      }
      Integer sourceId = nodeIds.get(source);
      Integer destId = nodeIds.get(dest);

      StringBuffer sb = new StringBuffer();

      sb.append("1\t"); // topic
      sb.append(sourceId.toString() + "\t");
      sb.append(destId.toString() + "\t");
      sb.append("1.0\n"); // weight

      bufferedWriter.write(sb.toString());
    }

    bufferedWriter.close();

    test.runScript();
    Iterator<Tuple> tuples = test.getAlias("data_grouped3");

    System.out.println("Final node ranks:");
    int nodeCount = 0;
    while (tuples.hasNext())
    {
      Tuple nodeTuple = tuples.next();

      Integer topic = (Integer)nodeTuple.get(0);
      Integer nodeId = (Integer)nodeTuple.get(1);
      Float nodeRank = (Float)nodeTuple.get(2);

      assertEquals(1, topic.intValue());

      System.out.println(String.format("%d => %f", nodeId, nodeRank));

      Float expectedNodeRank = expectedRanks.get(nodeIdsReversed.get(nodeId));

      assertTrue(String.format("expected: %f, actual: %f", expectedNodeRank, nodeRank),
                 Math.abs(expectedNodeRank - nodeRank * 100.0f) < 0.1);

      nodeCount++;
    }

    assertEquals(nodeIds.size(),nodeCount);
  }
}
