package org.apache.bigtop.itest.datafu.sessions;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class SessionTests extends PigTests
{
  @Test
  public void sessionizeTest() throws Exception
  {
    PigTest test = createPigTest("datafu/sessions/sessionizeTest.pig",
                                 "TIME_WINDOW=30m",
                                 "JAR_PATH=" + getJarPath());

    this.writeLinesToFile("input", 
                          "2010-01-01T01:00:00Z\t1\t10",
                          "2010-01-01T01:15:00Z\t1\t20",
                          "2010-01-01T01:31:00Z\t1\t10",
                          "2010-01-01T01:35:00Z\t1\t20",
                          "2010-01-01T02:30:00Z\t1\t30",

                          "2010-01-01T01:00:00Z\t2\t10",
                          "2010-01-01T01:31:00Z\t2\t20",
                          "2010-01-01T02:10:00Z\t2\t30",
                          "2010-01-01T02:40:30Z\t2\t40",
                          "2010-01-01T03:30:00Z\t2\t50",

                          "2010-01-01T01:00:00Z\t3\t10",
                          "2010-01-01T01:01:00Z\t3\t20",
                          "2010-01-01T01:02:00Z\t3\t5",
                          "2010-01-01T01:10:00Z\t3\t25",
                          "2010-01-01T01:15:00Z\t3\t50",
                          "2010-01-01T01:25:00Z\t3\t30",
                          "2010-01-01T01:30:00Z\t3\t15");
    
    test.runScript();
    
    HashMap<Integer,HashMap<Integer,Boolean>> userValues = new HashMap<Integer,HashMap<Integer,Boolean>>();
    
    for (Tuple t : this.getLinesForAlias(test, "max_value"))
    {
      Integer userId = (Integer)t.get(0);
      Integer max = (Integer)t.get(1);
      if (!userValues.containsKey(userId))
      {
        userValues.put(userId, new HashMap<Integer,Boolean>());
      }
      userValues.get(userId).put(max, true);
    }
    
    assertEquals(userValues.get(1).size(), 2);
    assertEquals(userValues.get(2).size(), 5);
    assertEquals(userValues.get(3).size(), 1);    
    
    assertTrue(userValues.get(1).containsKey(20));
    assertTrue(userValues.get(1).containsKey(30));
    
    assertTrue(userValues.get(2).containsKey(10));
    assertTrue(userValues.get(2).containsKey(20));
    assertTrue(userValues.get(2).containsKey(30));
    assertTrue(userValues.get(2).containsKey(40));
    assertTrue(userValues.get(2).containsKey(50));    

    assertTrue(userValues.get(3).containsKey(50));
  }
}

