package org.apache.bigtop.itest.datafu.bags.sets;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class SetTests extends PigTests
{
  @Test
  public void setIntersectTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/sets/setIntersectTest.pig");
    
    String[] input = {
      "{(1,10),(2,20),(3,30),(4,40),(5,50),(6,60)}\t{(0,0),(2,20),(4,40),(8,80)}",
      "{(1,10),(1,10),(2,20),(3,30),(3,30),(4,40),(4,40)}\t{(1,10),(3,30)}"
    };
    
    String[] output = {
        "({(2,20),(4,40)})",
        "({(1,10),(3,30)})"
      };
    
    test.assertOutput("data",input,"data2",output);
  }
  
  @Test
  public void setIntersectOutOfOrderTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/sets/setIntersectTest.pig");
    
    this.writeLinesToFile("input", 
                          "{(1,10),(3,30),(2,20),(4,40),(5,50),(6,60)}\t{(0,0),(2,20),(4,40),(8,80)}");
        
    test.runScript();
    
    this.getLinesForAlias(test, "data2");
  }
  
  @Test
  public void setUnionTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/sets/setUnionTest.pig");
    
    String[] input = {
        "{(1,10),(1,20),(1,30),(1,40),(1,50),(1,60),(1,80)}\t{(1,1),(1,20),(1,25),(1,25),(1,25),(1,40),(1,70),(1,80)}"
    };
    
    String[] output = {
        "({(1,10),(1,20),(1,30),(1,40),(1,50),(1,60),(1,80),(1,1),(1,25),(1,70)})"
      };
    
    test.assertOutput("data",input,"data2",output);
  }
}
