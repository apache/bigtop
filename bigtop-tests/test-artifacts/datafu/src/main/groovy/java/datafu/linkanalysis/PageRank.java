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
 
package datafu.linkanalysis;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.AbstractIterator;

/**
 * An implementation of {@link <a href="http://en.wikipedia.org/wiki/PageRank" target="_blank">PageRank</a>}.
 * This implementation is not distributed.  It is intended for graphs of a reasonable size which can be processed
 * on a single machine.  Nodes are stored in memory.  Edges are stored in memory and can optionally be spilled to
 * disk once a certain limit is reached.  
 */
public class PageRank
{    
  private float totalRankChange;
  private long edgeCount;
  private long nodeCount;
  
  // the damping factor
  private static float ALPHA = 0.85f;
  
  // edge weights (which are doubles) are multiplied by this value so they can be stored as integers internally
  private static float EDGE_WEIGHT_MULTIPLIER = 100000;
    
  private final Int2IntOpenHashMap nodeIndices = new Int2IntOpenHashMap();
  private final FloatArrayList nodeData = new FloatArrayList(); // rank, total weight, contribution, (repeat)
  
  private final IntArrayList danglingNodes = new IntArrayList();
  
  private final IntArrayList edges = new IntArrayList(); // source, dest node count... dest id, weight pos, (repeat)
  
  private boolean shouldHandleDanglingNodes = false;
  private boolean shouldCacheEdgesOnDisk = false;
  private long edgeCachingThreshold;
  
  private File edgesFile;
  private DataOutputStream edgeDataOutputStream;
  private boolean usingEdgeDiskCache;
  
  public interface ProgressIndicator
  {
    void progress();
  }
  
  public void clear() throws IOException
  {
    this.edgeCount = 0;
    this.nodeCount = 0;
    this.totalRankChange = 0.0f;
    
    this.nodeIndices.clear();
    this.nodeData.clear();
    this.edges.clear();
    this.danglingNodes.clear();
    
    if (edgeDataOutputStream != null)
    {
      this.edgeDataOutputStream.close();
      this.edgeDataOutputStream = null;
    }
    
    this.usingEdgeDiskCache = false;
    this.edgesFile = null;
  }
  
  /**
   * Gets whether disk is being used to cache edges.
   * @return True if the edges are cached on disk.
   */
  public boolean isUsingEdgeDiskCache()
  {
    return usingEdgeDiskCache;
  }
  
  /**
   * Enable disk caching of edges once there are too many (disabled by default).
   */
  public void enableEdgeDiskCaching()
  {
    shouldCacheEdgesOnDisk = true;
  }
  
  /**
   * Disable disk caching of edges once there are too many (disabled by default).
   */
  public void disableEdgeDiskCaching()
  {
    shouldCacheEdgesOnDisk = false;
  }
  
  /**
   * Gets whether edge disk caching is enabled.
   * @return True if edge disk caching is enabled.
   */
  public boolean isEdgeDiskCachingEnabled()
  {
    return shouldCacheEdgesOnDisk;
  }
  
  /**
   * Gets the number of edges past which they will be cached on disk instead of in memory.
   * Edge disk caching must be enabled for this to have any effect.
   * @return Edge count past which caching occurs
   */
  public long getEdgeCachingThreshold()
  {
    return edgeCachingThreshold;
  }

  /**
   * Set the number of edges past which they will be cached on disk instead of in memory.
   * Edge disk caching must be enabled for this to have any effect.
   * @param count Edge count past which caching occurs
   */
  public void setEdgeCachingThreshold(long count)
  {
    edgeCachingThreshold = count;
  }
  
  /**
   * Enables dangling node handling (disabled by default).
   */
  public void enableDanglingNodeHandling()
  {
    shouldHandleDanglingNodes = true;
  }
  
  /**
   * Disables dangling node handling (disabled by default).
   */
  public void disableDanglingNodeHandling()
  {
    shouldHandleDanglingNodes = false;
  }
  
  public long nodeCount()
  {
    return this.nodeCount;
  }
  
  public long edgeCount()
  {
    return this.edgeCount;
  }

  public Int2IntMap.FastEntrySet getNodeIds()
  {
    return this.nodeIndices.int2IntEntrySet();
  }
  
  public float getNodeRank(int nodeId)
  {
    int nodeIndex = this.nodeIndices.get(nodeId);
    return nodeData.get(nodeIndex);
  }
  
  public float getTotalRankChange()
  {
    return this.totalRankChange;
  }
  
  private void maybeCreateNode(int nodeId)
  {
    // create from node if it doesn't already exist
    if (!nodeIndices.containsKey(nodeId))
    {      
      int index = this.nodeData.size();
      
      this.nodeData.add(0.0f); // rank
      this.nodeData.add(0.0f); // total weight
      this.nodeData.add(0.0f); // contribution
      
      this.nodeIndices.put(nodeId, index);
      
      this.nodeCount++;
    }
  }
  
  public void addEdges(Integer sourceId, ArrayList<Map<String,Object>> sourceEdges) throws IOException
  {
    int source = sourceId.intValue();
   
    maybeCreateNode(source);
    
    if (this.shouldCacheEdgesOnDisk && !usingEdgeDiskCache && (sourceEdges.size() + this.edgeCount) >= this.edgeCachingThreshold)
    {
      writeEdgesToDisk();
    }
    
    // store the source node id itself
    appendEdgeData(source);
    
    // store how many outgoing edges this node has
    appendEdgeData(sourceEdges.size());
    
    // store the outgoing edges
    for (Map<String,Object> edge : sourceEdges)
    {
      int dest = ((Integer)edge.get("dest")).intValue();
      float weight = ((Double)edge.get("weight")).floatValue();
            
      maybeCreateNode(dest);
      
      appendEdgeData(dest);
      
      // location of weight in weights array
      appendEdgeData(Math.max(1, (int)(weight * EDGE_WEIGHT_MULTIPLIER)));
      
      this.edgeCount++;
    }
  }
  
  private void appendEdgeData(int data) throws IOException
  {
    if (this.edgeDataOutputStream != null)
    {
      this.edgeDataOutputStream.writeInt(data);
    }
    else
    {
      this.edges.add(data);
    }
  }
    
  public void init(ProgressIndicator progressIndicator) throws IOException
  {
    if (this.edgeDataOutputStream != null)
    {
      this.edgeDataOutputStream.close();
      this.edgeDataOutputStream = null;
    }
    
    // initialize all nodes to an equal share of the total rank (1.0)
    float nodeRank = 1.0f / this.nodeCount;        
    for (int j=0; j<this.nodeData.size(); j+=3)
    {
      nodeData.set(j, nodeRank);      
      progressIndicator.progress();
    }      
    
    Iterator<Integer> edgeData = getEdgeData();
    
    while(edgeData.hasNext())
    {
      int sourceId = edgeData.next();
      int nodeEdgeCount = edgeData.next();
      
      while (nodeEdgeCount-- > 0)
      {
        // skip the destination node id
        edgeData.next();
        
        float weight = edgeData.next();
                
        int nodeIndex = this.nodeIndices.get(sourceId);
        
        float totalWeight = this.nodeData.getFloat(nodeIndex+1); 
        totalWeight += weight;
        this.nodeData.set(nodeIndex+1, totalWeight);
        
        progressIndicator.progress();
      }
    }
    
    // if handling dangling nodes, get a list of them by finding those nodes with no outgoing
    // edges (i.e. total outgoing edge weight is 0.0)
    if (shouldHandleDanglingNodes)
    {
      for (Map.Entry<Integer,Integer> e : nodeIndices.entrySet())
      {
        int nodeId = e.getKey();
        int nodeIndex = e.getValue();
        float totalWeight = nodeData.getFloat(nodeIndex+1);
        if (totalWeight == 0.0f)
        {
          danglingNodes.add(nodeId);
        }
      }
    }
  }
  
  public float nextIteration(ProgressIndicator progressIndicator) throws IOException
  {
    distribute(progressIndicator);
    commit(progressIndicator);
    
    return getTotalRankChange();
  }
  
  public void distribute(ProgressIndicator progressIndicator) throws IOException
  {    
    Iterator<Integer> edgeData = getEdgeData();
    
    while(edgeData.hasNext())
    {
      int sourceId = edgeData.next();
      int nodeEdgeCount = edgeData.next();
      
      while (nodeEdgeCount-- > 0)
      {
        int toId = edgeData.next();
        float weight = edgeData.next();
                
        int fromNodeIndex = this.nodeIndices.get(sourceId);
        int toNodeIndex = this.nodeIndices.get(toId);
        
        float contributionChange = weight * this.nodeData.getFloat(fromNodeIndex) / this.nodeData.getFloat(fromNodeIndex+1);
        
        float currentContribution = this.nodeData.getFloat(toNodeIndex+2);
        this.nodeData.set(toNodeIndex+2, currentContribution + contributionChange);
        
        progressIndicator.progress();
      }      
    }
    
    if (shouldHandleDanglingNodes)
    {
      // get the rank from each of the dangling nodes
      float totalRank = 0.0f;
      for (int nodeId : danglingNodes)
      {
        int nodeIndex = nodeIndices.get(nodeId);
        float rank = nodeData.get(nodeIndex);
        totalRank += rank;
      }
      
      // distribute the dangling node ranks to all the nodes in the graph
      // note: the alpha factor is applied in the commit stage
      float contributionIncrease = totalRank / this.nodeCount;
      for (int i=2; i<nodeData.size(); i += 3)
      {
        float contribution = nodeData.getFloat(i);
        contribution += contributionIncrease;
        nodeData.set(i, contribution);
      }
    }
  }
  
  public void commit(ProgressIndicator progressIndicator)
  {
    this.totalRankChange = 0.0f;
    
    for (int id : nodeIndices.keySet())
    {
      int nodeIndex = this.nodeIndices.get(id);
      
      float alpha = datafu.linkanalysis.PageRank.ALPHA;
      float newRank = (1.0f - alpha)/nodeCount + alpha * this.nodeData.get(nodeIndex+2);
      
      this.nodeData.set(nodeIndex+2, 0.0f);
      
      float lastRankDiff = newRank - this.nodeData.get(nodeIndex);
      
      this.nodeData.set(nodeIndex, newRank);
      
      this.totalRankChange += Math.abs(lastRankDiff);
      
      progressIndicator.progress();
    }
  }
  
  private void writeEdgesToDisk() throws IOException
  { 
    this.edgesFile = File.createTempFile("fastgraph", null);
    
    FileOutputStream outStream = new FileOutputStream(this.edgesFile);
    BufferedOutputStream bufferedStream = new BufferedOutputStream(outStream);
    this.edgeDataOutputStream = new DataOutputStream(bufferedStream);
    
    for (int edgeData : edges)
    {
      this.edgeDataOutputStream.writeInt(edgeData);
    }
    
    this.edges.clear();
    usingEdgeDiskCache = true;
  }
  
  private Iterator<Integer> getEdgeData() throws IOException
  {
    if (!usingEdgeDiskCache)
    {
      return this.edges.iterator();
    }
    else
    {
      FileInputStream fileInputStream = new FileInputStream(this.edgesFile);
      BufferedInputStream inputStream = new BufferedInputStream(fileInputStream);
      final DataInputStream dataInputStream = new DataInputStream(inputStream);
      
      return new AbstractIterator<Integer>() {
        
        @Override
        protected Integer computeNext()
        {
          try
          {
            return dataInputStream.readInt();
          }
          catch (IOException e)
          {
            return endOfData();
          }
        }
        
      };
    }
  }
}

