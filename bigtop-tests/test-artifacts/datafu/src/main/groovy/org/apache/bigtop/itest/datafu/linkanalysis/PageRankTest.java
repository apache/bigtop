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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PageRankTest
{
  @Test
  public void wikipediaGraphInMemoryTest() throws Exception {
    System.out.println();
    System.out.println("Starting wikipediaGraphInMemoryTest");
    
    datafu.linkanalysis.PageRank graph = new datafu.linkanalysis.PageRank();
   
    String[] edges = getWikiExampleEdges();
    
    Map<String,Integer> nodeIdsMap = loadGraphFromEdgeList(graph, edges);
    
    // Without dangling node handling we will not get the true page rank since the total rank will
    // not add to 1.0.  Without dangling node handling some of the page rank drains out of the graph.
    graph.enableDanglingNodeHandling();
    
    performIterations(graph, 150, 1e-18f);
    
    String[] expectedRanks = getWikiExampleExpectedRanks();
    
    Map<String,Float> expectedRanksMap = parseExpectedRanks(expectedRanks);
    
    validateExpectedRanks(graph, nodeIdsMap, expectedRanksMap);
  }
  
  @Test
  public void wikipediaGraphDiskCacheTest() throws Exception {
    System.out.println();
    System.out.println("Starting wikipediaGraphDiskCacheTest");
    
    datafu.linkanalysis.PageRank graph = new datafu.linkanalysis.PageRank();
    
    String[] edges = getWikiExampleEdges();
    
    graph.enableEdgeDiskCaching();
    graph.setEdgeCachingThreshold(5);
    
    Map<String,Integer> nodeIdsMap = loadGraphFromEdgeList(graph, edges);
    
    assert graph.isUsingEdgeDiskCache() : "Expected disk cache to be used";
    
    // Without dangling node handling we will not get the true page rank since the total rank will
    // not add to 1.0.  Without dangling node handling some of the page rank drains out of the graph.
    graph.enableDanglingNodeHandling();
    
    performIterations(graph, 150, 1e-18f);
    
    String[] expectedRanks = getWikiExampleExpectedRanks();
    
    Map<String,Float> expectedRanksMap = parseExpectedRanks(expectedRanks);
    
    validateExpectedRanks(graph, nodeIdsMap, expectedRanksMap);
  }
  
  @Test
  public void hubAndSpokeInMemoryTest() throws Exception {
    System.out.println();
    System.out.println("Starting hubAndSpokeInMemoryTest");
    
    datafu.linkanalysis.PageRank graph = new datafu.linkanalysis.PageRank();
   
    String[] edges = getHubAndSpokeEdges();
    
    Map<String,Integer> nodeIdsMap = loadGraphFromEdgeList(graph, edges);
    
    graph.enableDanglingNodeHandling();
    
    performIterations(graph, 150, 1e-18f);
    
    // no need to validate, this is just a perf test for runtime comparison
  }
  
  @Test
  public void hubAndSpokeDiskCacheTest() throws Exception {
    System.out.println();
    System.out.println("Starting hubAndSpokeDiskCacheTest");
    
    datafu.linkanalysis.PageRank graph = new datafu.linkanalysis.PageRank();
   
    String[] edges = getHubAndSpokeEdges();
    
    graph.enableEdgeDiskCaching();
    graph.setEdgeCachingThreshold(5);
    
    Map<String,Integer> nodeIdsMap = loadGraphFromEdgeList(graph, edges);
    
    graph.enableDanglingNodeHandling();
    
    performIterations(graph, 150, 1e-18f);
    
    // no need to validate, this is just a perf test for runtime comparison
  }
  
  private String[] getHubAndSpokeEdges()
  {
    int count = 50000;
    String[] edges = new String[count];
    
    for (int i=0; i<count; i++)
    {
      edges[i] = String.format("S%d H", i);
    }
    return edges;
  }
  
  public static String[] getWikiExampleEdges()
  {
    // graph taken from:
    // http://en.wikipedia.org/wiki/PageRank
    String[] edges = {
        "B C",
        "C B",
        "D A",
        "D B",
        "E D",
        "E B",
        "E F",
        "F E",
        "F B",
        "P1 B",
        "P1 E",
        "P2 B",
        "P2 E",
        "P3 B",
        "P3 E",
        "P4 E",
        "P5 E"
      };
    return edges;
  }
  
  public static String[] getWikiExampleExpectedRanks()
  {
    // these ranks come from the Wikipedia page:
    // http://en.wikipedia.org/wiki/PageRank
    String[] expectedRanks = {
        "A 3.3",
        "B 38.4",
        "C 34.3",
        "D 3.9",
        "E 8.1",
        "F 3.9",
        "P1 1.6",
        "P2 1.6",
        "P3 1.6",
        "P4 1.6",
        "P5 1.6"      
      };
    return expectedRanks;
  }
  
  private Map<String,Integer> loadGraphFromEdgeList(datafu.linkanalysis.PageRank graph, String[] edges) throws IOException
  {
    Map<Integer,ArrayList<Map<String,Object>>> nodeEdgesMap = new HashMap<Integer,ArrayList<Map<String,Object>>>();
    Map<String,Integer> nodeIdsMap = new HashMap<String,Integer>();
    
    for (String edge : edges)
    {
      String[] parts = edge.split(" ");
      assert parts.length == 2 : "Expected two parts";
      
      int sourceId = getOrCreateId(parts[0], nodeIdsMap);
      int destId = getOrCreateId(parts[1], nodeIdsMap);
      
      Map<String,Object> edgeMap = new HashMap<String,Object>();
      edgeMap.put("weight", 1.0);
      edgeMap.put("dest", destId);
      
      ArrayList<Map<String,Object>> nodeEdges = null;
      
      if (nodeEdgesMap.containsKey(sourceId))
      {
        nodeEdges = nodeEdgesMap.get(sourceId);
      }
      else
      {
        nodeEdges = new ArrayList<Map<String,Object>>();
        nodeEdgesMap.put(sourceId, nodeEdges);
      }
      
      nodeEdges.add(edgeMap);
    }
    
    for (Map.Entry<Integer, ArrayList<Map<String,Object>>> e : nodeEdgesMap.entrySet())
    {
      graph.addEdges(e.getKey(), e.getValue());
    }
    
    return nodeIdsMap;
  }
  
  private void performIterations(datafu.linkanalysis.PageRank graph, int maxIters, float tolerance) throws IOException
  {
    System.out.println(String.format("Beginning iteration (maxIters = %d, tolerance=%e)", maxIters, tolerance));
    
    datafu.linkanalysis.PageRank.ProgressIndicator progressIndicator = getDummyProgressIndicator();
    
    System.out.println("Initializing graph");
    long startTime = System.nanoTime();
    graph.init(progressIndicator);
    System.out.println(String.format("Done, took %f ms", (System.nanoTime() - startTime)/10.0e6));
    
    float totalDiff;
    int iter = 0;
    
    System.out.println("Beginning iterations");
    startTime = System.nanoTime();
    do 
    {
      totalDiff = graph.nextIteration(progressIndicator);
      iter++;      
    } while(iter < maxIters && totalDiff > tolerance);
    System.out.println(String.format("Done, took %f ms", (System.nanoTime() - startTime)/10.0e6));
  }
  
  private datafu.linkanalysis.PageRank.ProgressIndicator getDummyProgressIndicator()
  {
    return new datafu.linkanalysis.PageRank.ProgressIndicator()
    {
      @Override
      public void progress()
      {
        // do nothing
      }     
    };
  }
  
  private void validateExpectedRanks(datafu.linkanalysis.PageRank graph, Map<String,Integer> nodeIds, Map<String,Float> expectedRanks)
  {
    System.out.println("Validating page rank results");
    
    for (Map.Entry<String,Integer> e : nodeIds.entrySet())
    {
      float rank = graph.getNodeRank(e.getValue());
      
      float expectedRank = expectedRanks.get(e.getKey());
      // require 0.1% accuracy
      assert (Math.abs(expectedRank - rank*100.0f) < 0.1) : String.format("Did not get expected rank for %s", e.getKey());      
    }
    
    System.out.println("All ranks match expected");
  }
  
  public static Map<String,Float> parseExpectedRanks(String[] expectedRanks)
  {
    Map<String,Float> expectedRanksMap = new HashMap<String,Float>();
    for (String expectedRankString : expectedRanks)
    {
      String[] parts = expectedRankString.split(" ");
      assert parts.length == 2 : "Expected two parts";
      String name = parts[0];
      Float expectedRank = Float.parseFloat(parts[1]);
      expectedRanksMap.put(name, expectedRank);
    }
    return expectedRanksMap;
  }

  private Integer getOrCreateId(String name, Map<String,Integer> nodeIds)
  {
    if (nodeIds.containsKey(name))
    {
      return nodeIds.get(name);
    }
    else
    {
      Integer id = nodeIds.size();
      nodeIds.put(name, id);
      return id;
    }
  }
}
