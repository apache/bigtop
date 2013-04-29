package org.apache.bigtop.itest.datafu.stats;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class WilsonBinConfTests extends PigTests
{
  @Test
  public void wilsonTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/wilsonBinConfTests.pig",
                                 "alpha=0.05"); // alpha is 0.05 for 95% confidence
    
    writeLinesToFile("input",
                     "1\t1",
                     "1\t2",
                     "50\t100",
                     "500\t1000",
                     "999\t1000",
                     "1000\t1000",
                     "998\t1000");
        
    test.runScript();
    
    /* Add expected values, computed using R:
     * 
     * e.g.
     * 
     * library(Hmisc)
     * 
     * binconf(50,100)
     * binconf(500,1000)
     * 
     */
    List<String> expectedOutput = new ArrayList<String>();
    expectedOutput.add("0.05129,1.00000");
    expectedOutput.add("0.02565,0.97435");
    expectedOutput.add("0.40383,0.59617");
    expectedOutput.add("0.46907,0.53093");
    expectedOutput.add("0.99436,0.99995");
    expectedOutput.add("0.99617,1.00000");
    expectedOutput.add("0.99274,0.99945");
    
    List<Tuple> output = this.getLinesForAlias(test, "data_out");
    Iterator<String> expectationIterator = expectedOutput.iterator();
    for (Tuple t : output)
    {
      assertTrue(expectationIterator.hasNext());
      Double lower = (Double)t.get(0);
      Double upper = (Double)t.get(1);
      assertEquals(String.format("%.5f,%.5f",lower,upper),expectationIterator.next());
    }
  }
}
