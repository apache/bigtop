/*
 * Copyright 2010 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
 
package datafu.pig.linkanalysis;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pig.Accumulator;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import datafu.linkanalysis.PageRank.ProgressIndicator;


/**
 * A UDF which implements {@link <a href="http://en.wikipedia.org/wiki/PageRank" target="_blank">PageRank</a>}.  
 * Each graph is stored in memory while running the algorithm, with edges optionally 
 * spilled to disk to conserve memory.  This can be used to distribute the execution of PageRank on a large number of 
 * reasonable sized graphs.  It does not distribute execuion of PageRank on a single graph.  Each graph is identified
 * by an integer valued topic ID.
 * <p>
 * Example:
 * <pre>
 * {@code
 * 
 * topic_edges = LOAD 'input_edges' as (topic:INT,source:INT,dest:INT,weight:DOUBLE);
 * 
 * topic_edges_grouped = GROUP topic_edges by (topic, source) ;
 * topic_edges_grouped = FOREACH topic_edges_grouped GENERATE
 *    group.topic as topic,
 *    group.source as source,
 *    topic_edges.(dest,weight) as edges;
 * 
 * topic_edges_grouped_by_topic = GROUP topic_edges_grouped BY topic; 
 * 
 * topic_ranks = FOREACH topic_edges_grouped_by_topic GENERATE
 *    group as topic,
 *    FLATTEN(PageRank(topic_edges_grouped.(source,edges))) as (source,rank);
 *
 * skill_ranks = FOREACH skill_ranks GENERATE
 *    topic, source, rank;
 * 
 * }
 * </pre> 
 */
public class PageRank extends EvalFunc<DataBag> implements Accumulator<DataBag>
{
  private final datafu.linkanalysis.PageRank graph = new datafu.linkanalysis.PageRank();

  private int maxNodesAndEdges = 100000000;
  private int maxEdgesInMemory = 30000000;
  private double tolerance = 1e-16;
  private int maxIters = 150;
  private boolean useEdgeDiskStorage = false;
  private boolean enableDanglingNodeHandling = false;
  private boolean aborted = false;

  TupleFactory tupleFactory = TupleFactory.getInstance();
  BagFactory bagFactory = BagFactory.getInstance();
  
  public PageRank()
  {
    initialize();
  }

  public PageRank(String... parameters)
  {
    if (parameters.length % 2 != 0)
    {
      throw new RuntimeException("Invalid parameters list");
    }

    for (int i=0; i<parameters.length; i+=2)
    {
      String parameterName = parameters[i];
      String value = parameters[i+1];
      if (parameterName.equals("max_nodes_and_edges"))
      {
        maxNodesAndEdges = Integer.parseInt(value);
      }
      else if (parameterName.equals("max_edges_in_memory"))
      {
        maxEdgesInMemory = Integer.parseInt(value);
      }
      else if (parameterName.equals("tolerance"))
      {
        tolerance = Double.parseDouble(value);
      }
      else if (parameterName.equals("max_iters"))
      {
        maxIters = Integer.parseInt(value);
      }
      else if (parameterName.equals("spill_to_edge_disk_storage"))
      {
        useEdgeDiskStorage = Boolean.parseBoolean(value);
      }
      else if (parameterName.equals("dangling_nodes"))
      {
        enableDanglingNodeHandling = Boolean.parseBoolean(value);
      }
    }

    initialize();
  }

  private void initialize()
  {
    long heapSize = Runtime.getRuntime().totalMemory();
    long heapMaxSize = Runtime.getRuntime().maxMemory();
    long heapFreeSize = Runtime.getRuntime().freeMemory();
//    System.out.println(String.format("Heap size: %d, Max heap size: %d, Heap free size: %d", heapSize, heapMaxSize, heapFreeSize));

    if (useEdgeDiskStorage)
    {
      this.graph.enableEdgeDiskCaching();
    }
    else
    {
      this.graph.disableEdgeDiskCaching();
    }

    if (enableDanglingNodeHandling)
    {
      this.graph.enableDanglingNodeHandling();
    }
    else
    {
      this.graph.disableDanglingNodeHandling();
    }

    this.graph.setEdgeCachingThreshold(maxEdgesInMemory);
  }

  @Override
  public void accumulate(Tuple t) throws IOException
  {
    if (aborted)
    {
      return;
    }
    
    DataBag bag = (DataBag) t.get(0);
    if (bag == null || bag.size() == 0)
      return;
    
    for (Tuple sourceTuple : bag) 
    {
      Integer sourceId = (Integer)sourceTuple.get(0);
      DataBag edges = (DataBag)sourceTuple.get(1);

      ArrayList<Map<String,Object>> edgesMapList = new ArrayList<Map<String, Object>>();

      for (Tuple edgeTuple : edges)
      {
        Integer destId = (Integer)edgeTuple.get(0);
        Double weight = (Double)edgeTuple.get(1);
        HashMap<String,Object> edgeMap = new HashMap<String, Object>();
        edgeMap.put("dest",destId);
        edgeMap.put("weight",weight);
        edgesMapList.add(edgeMap);
      }

      graph.addEdges(sourceId, edgesMapList);

      if (graph.nodeCount() + graph.edgeCount() > maxNodesAndEdges)
      {
        System.out.println(String.format("There are too many nodes and edges (%d + %d > %d). Aborting.", graph.nodeCount(), graph.edgeCount(), maxNodesAndEdges));
        aborted = true;
      }

      reporter.progress();
    }
  }

  @Override
  public DataBag getValue()
  {
    if (aborted)
    {
      return null;
    }
    
    System.out.println(String.format("Nodes: %d, Edges: %d", graph.nodeCount(), graph.edgeCount()));
    
    ProgressIndicator progressIndicator = getProgressIndicator();
    System.out.println("Finished loading graph.");
    long startTime = System.nanoTime();
    System.out.println("Initializing.");
    try
    {
      graph.init(progressIndicator);
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return null;
    }
    System.out.println(String.format("Done, took %f ms", (System.nanoTime() - startTime)/10.0e6));

    float totalDiff;
    int iter = 0;

    System.out.println("Beginning iterations");
    startTime = System.nanoTime();
    do
    {
      // TODO log percentage complete every 5 minutes
      try
      {
        totalDiff = graph.nextIteration(progressIndicator);
      }
      catch (IOException e)
      {
        e.printStackTrace();
        return null;
      }
      iter++;
    } while(iter < maxIters && totalDiff > tolerance);
    System.out.println(String.format("Done, %d iterations took %f ms", iter, (System.nanoTime() - startTime)/10.0e6));

    DataBag output = bagFactory.newDefaultBag();

    for (Int2IntMap.Entry node : graph.getNodeIds())
    {
      int nodeId = node.getIntKey();
      float rank = graph.getNodeRank(nodeId);
      List nodeData = new ArrayList(2);
      nodeData.add(nodeId);
      nodeData.add(rank);
      output.add(tupleFactory.newTuple(nodeData));
    }

    return output;
  }

  @Override
  public void cleanup()
  {
    try
    {
      aborted = false;
      this.graph.clear();
    }
    catch (IOException e)
    { 
      e.printStackTrace();
    }
  }

  @Override
  public DataBag exec(Tuple input) throws IOException
  {
    try
    {
      accumulate(input);
      
      return getValue();
    }
    finally
    {
      cleanup();
    }
  }

  private ProgressIndicator getProgressIndicator()
  {
    return new ProgressIndicator()
        {
          @Override
          public void progress()
          {
            reporter.progress();
          }
        };
  }

  @Override
  public Schema outputSchema(Schema input)
  {
    try
    {
      Schema.FieldSchema inputFieldSchema = input.getField(0);

      if (inputFieldSchema.type != DataType.BAG)
      {
        throw new RuntimeException("Expected a BAG as input");
      }

      Schema inputBagSchema = inputFieldSchema.schema;

      if (inputBagSchema.getField(0).type != DataType.TUPLE)
      {
        throw new RuntimeException(String.format("Expected input bag to contain a TUPLE, but instead found %s",
                                                 DataType.findTypeName(inputBagSchema.getField(0).type)));
      }
      
      Schema inputTupleSchema = inputBagSchema.getField(0).schema;
      
      if (inputTupleSchema.getField(0).type != DataType.INTEGER)
      {
        throw new RuntimeException(String.format("Expected source to be an INTEGER, but instead found %s",
                                                 DataType.findTypeName(inputTupleSchema.getField(0).type)));
      }

      if (inputTupleSchema.getField(1).type != DataType.BAG)
      {
        throw new RuntimeException(String.format("Expected edges to be represented with a BAG"));
      }

      Schema.FieldSchema edgesFieldSchema = inputTupleSchema.getField(1);

      if (edgesFieldSchema.schema.getField(0).type != DataType.TUPLE)
      {
        throw new RuntimeException(String.format("Expected edges field to contain a TUPLE, but instead found %s",
                                                 DataType.findTypeName(edgesFieldSchema.schema.getField(0).type)));
      }
      
      Schema edgesTupleSchema = edgesFieldSchema.schema.getField(0).schema;
      
      if (edgesTupleSchema.getField(0).type != DataType.INTEGER)
      {
        throw new RuntimeException(String.format("Expected destination edge ID to an INTEGER, but instead found %s",
                                                 DataType.findTypeName(edgesFieldSchema.schema.getField(0).type)));
      }

      if (edgesTupleSchema.getField(1).type != DataType.DOUBLE)
      {
        throw new RuntimeException(String.format("Expected destination edge weight to a DOUBLE, but instead found %s",
                                                 DataType.findTypeName(edgesFieldSchema.schema.getField(1).type)));
      }

      Schema tupleSchema = new Schema();
      tupleSchema.add(new Schema.FieldSchema("node",DataType.INTEGER));
      tupleSchema.add(new Schema.FieldSchema("rank",DataType.FLOAT));

      return new Schema(new Schema.FieldSchema(getSchemaName(this.getClass()
                                                                 .getName()
                                                                 .toLowerCase(), input),
                                               tupleSchema,
                                               DataType.BAG));
    }
    catch (FrontendException e)
    {
      throw new RuntimeException(e);
    }
  }
}
