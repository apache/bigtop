package org.apache.bigtop.itest.datafu.hash;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class HashTests  extends PigTests
{
  @Test
  public void md5Test() throws Exception
  {
    PigTest test = createPigTest("datafu/hash/md5Test.pig");
    
    writeLinesToFile("input", 
                     "ladsljkasdglk",
                     "lkadsljasgjskdjks",
                     "aladlasdgjks");
            
    test.runScript();
        
    assertOutput(test, "data_out",
                 "(d9a82575758bb4978949dc0659205cc6)",
                 "(9ec37f02fae0d8d6a7f4453a62272f1f)",
                 "(cb94139a8b9f3243e68a898ec6bd9b3d)");
  }
  
  @Test
  public void md5Base64Test() throws Exception
  {
    PigTest test = createPigTest("datafu/hash/md5Base64Test.pig");
    
    writeLinesToFile("input", 
                     "ladsljkasdglk",
                     "lkadsljasgjskdjks",
                     "aladlasdgjks");
            
    test.runScript();
        
    assertOutput(test, "data_out",
                 "(2agldXWLtJeJSdwGWSBcxg==)",
                 "(nsN/Avrg2Nan9EU6YicvHw==)",
                 "(y5QTmoufMkPmiomOxr2bPQ==)");
  }
}
