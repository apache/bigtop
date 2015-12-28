/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hdfs

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import static org.apache.bigtop.itest.LogErrorsUtils.logError;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.EssentialTests;
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.NormalTests;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category ( NormalTests.class )
public class TestHDFSQuota {
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");
  private static Shell sh = new Shell("/bin/bash");
  private static final long LARGE = Long.MAX_VALUE - 1;
  private static final String USERNAME = System.getProperty("user.name");
  private static final String KERBEROS = System.getenv("KERBEROS"); 
  private static String quotaDate = shHDFS.exec("date").getOut().get(0).replaceAll("\\s","").replaceAll(":","");
  private static String testQuotaFolder = "/tmp/testQuotaFolder" + quotaDate;
  private static String testQuotaFolder1 = testQuotaFolder + "1";
  private static String testQuotaFolder2 = testQuotaFolder + "2";
  private static String testQuotaFolder3 = testQuotaFolder + "3";
  
  @BeforeClass
  public static void setUp() {
    // creating test folders
    if(KERBEROS == "on") {
      shHDFS = sh;
    } 
    shHDFS.exec("hdfs dfs -mkdir $testQuotaFolder1");
    assertTrue("Could not create input directory", shHDFS.getRet() == 0);

    sh.exec("hdfs dfs -mkdir $testQuotaFolder2");
    assertTrue("Could not create input directory", sh.getRet() == 0);
  }

  @AfterClass
  public static void tearDown() {
    // clean up of existing folders
    shHDFS.exec("hdfs dfs -test -e $testQuotaFolder1");
    if (shHDFS.getRet() == 0) {
      shHDFS.exec("hdfs dfs -rm -r -skipTrash $testQuotaFolder1");
      assertTrue("Deletion of previous testQuotaFolder1 from HDFS failed",
          shHDFS.getRet() == 0);
    }
    shHDFS.exec("hdfs dfs -test -e $testQuotaFolder2");
    if (shHDFS.getRet() == 0) {
      shHDFS.exec("hdfs dfs -rm -r -skipTrash $testQuotaFolder2");
      assertTrue("Deletion of previous testQuotaFolder2 from HDFS failed",
          shHDFS.getRet() == 0);
    }
    sh.exec("hdfs dfs -test -e $testQuotaFolder1");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $testQuotaFolder1");
      assertTrue("Deletion of previous testQuotaFolder1 from HDFS failed",
          sh.getRet() == 0);
    }
  }

  @Category ( EssentialTests.class )
  @Test
  public void t1_testNewlyCreatedDir() { 
    // newly created dir should have no name quota, no space quota   
    shHDFS.exec("hdfs dfs -count -q $testQuotaFolder1");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] output = shHDFS.getOut().get(0).trim().split();
    assertTrue("Newly created directory had a set name quota", output[0].equals("none"));
    assertTrue("Newly created directory had a set name quota left", output[1].equals("inf"));
    assertTrue("Newly created directory had a set space quota", output[2].equals("none"));
    assertTrue("Newly created directory had a set space quota left", output[3].equals("inf"));
  } 

  @Category ( EssentialTests.class )
  @Test
  public void t2_testAdminPermissions() { 
    assumeFalse(KERBEROS == 'on')
    // admin setting quotas should succeed
    shHDFS.exec("hdfs dfsadmin -setQuota 10 $testQuotaFolder1");
    assertTrue("setQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1000000 $testQuotaFolder1");
    assertTrue("setSpaceQuota failed", shHDFS.getRet() == 0);

    // non-admin setting/clearing quotas should fail
    sh.exec("hdfs dfsadmin -setQuota 10 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", sh.getRet() != 0);
    sh.exec("hdfs dfsadmin -setSpaceQuota 1000000 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", sh.getRet() != 0);
    sh.exec("hdfs dfsadmin -clrQuota $testQuotaFolder1");
    assertTrue("clrQuota should not have worked", sh.getRet() != 0);
    sh.exec("hdfs dfsadmin -clrSpaceQuota $testQuotaFolder1");
    assertTrue("clrSpaceQuota should not have worked", sh.getRet() != 0);

    // admin clearing quotas should succeed
    shHDFS.exec("hdfs dfsadmin -clrQuota $testQuotaFolder1");
    assertTrue("clrQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -clrSpaceQuota $testQuotaFolder1");
    assertTrue("clrSpaceQuota failed", shHDFS.getRet() == 0);
  } 

  @Category ( EssentialTests.class )
  @Test
  public void t3_testRename() { 
    // name and space quotas stick after rename
    shHDFS.exec("hdfs dfs -count -q $testQuotaFolder1");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] status1 = shHDFS.getOut().get(0).trim().split();
    shHDFS.exec("hdfs dfs -mv $testQuotaFolder1" + " $testQuotaFolder3");
    assertTrue("Could not use move command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -count -q $testQuotaFolder3");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] status2 = shHDFS.getOut().get(0).trim().split();
    for (int i = 0; i < status1.length - 1; i++) {
      assertTrue("quotas changed after folder rename", status1[i].equals(status2[i]));
    }
    shHDFS.exec("hdfs dfs -mv $testQuotaFolder3" + " $testQuotaFolder1");
    assertTrue("Could not use move command", shHDFS.getRet() == 0);
  }

  @Test
  public void t4_testInputValues() { 
    assumeFalse(KERBEROS == 'on')
    // the largest allowable quota size is Long.Max_Value and must be greater than zero
    shHDFS.exec("hdfs dfsadmin -setQuota -1 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota -1 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);  
    shHDFS.exec("hdfs dfsadmin -setQuota 1.04 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1.04 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);        
    shHDFS.exec("hdfs dfsadmin -setQuota 0 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 0 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -setQuota $LARGE $testQuotaFolder1");
    assertTrue("setQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota $LARGE $testQuotaFolder1");
    assertTrue("setSpaceQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setQuota 9223372036854775808 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 9223372036854775808 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);
  }

  @Test
  public void t5_testForceDirEmpty() {
    assumeFalse(KERBEROS == 'on')
    // setting the name quota to 1 for an empty dir will cause the dir to remain empty
    shHDFS.exec("hdfs dfsadmin -setQuota 1 $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -mkdir $testQuotaFolder1" + "/sample1");
    assertTrue("mkdir should not have worked due to quota of 1", shHDFS.getRet() != 0);
  }

  @Test
  public void t6_testQuotasPostViolation() {  
    assumeFalse(KERBEROS == 'on')
    // quota can be set even if it violates
    shHDFS.exec("hdfs dfsadmin -setQuota $LARGE $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -put - $testQuotaFolder1" + "/testString1", "-------TEST STRING--------"); 
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -mkdir $testQuotaFolder1" + "/sample1");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -mkdir $testQuotaFolder1" + "/sample2");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setQuota 2 $testQuotaFolder1");
    assertTrue("setQuota should have worked", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1 $testQuotaFolder1");
    assertTrue("setSpaceQuota should have worked", shHDFS.getRet() == 0);
  }

  @Test
  public void t7_testQuotas() {
    assumeFalse(KERBEROS == 'on')
    // dir creation should fail - name quota
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 10000000000 $testQuotaFolder1");
    assertTrue("Could not setSpaceQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -mkdir $testQuotaFolder1" + "/sample3");
    assertTrue("mkdir should not have worked", shHDFS.getRet() != 0);

    // file creation should fail - name quota
    shHDFS.exec("hdfs dfs -rm -r $testQuotaFolder1" + "/testString1"); 
    shHDFS.exec("hdfs dfs -put - $testQuotaFolder1" + "/testString2", "-------TEST STRING--------"); 
    assertTrue("put should not have worked", shHDFS.getRet() != 0);

    // file creation should fail - space quota
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 10 $testQuotaFolder1");
    assertTrue("Could not setSpaceQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setQuota 1000 $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -put - $testQuotaFolder1"  + "/testString3", "-------TEST STRING--------"); 
    assertTrue("put should not have worked", shHDFS.getRet() != 0); 
  }

  //@Test - can be reinstated upon resolution of BIGTOP-635 due to restarting of hdfs service
  public void testLogEntries() {
    assumeFalse(KERBEROS == 'on')
    // Log entry created when nodes are started with both quota violations
    String date = "logTest" + quotaDate;
    shHDFS.exec("hdfs dfs -mkdir $date");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -put - $date" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setQuota 1 $date");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0); 
    shHDFS.exec("date");
    String date1 = "logTest" + shHDFS.getOut().get(0).replaceAll("\\s","").replaceAll(":","");
    shHDFS.exec("hdfs dfs -mkdir $date1");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -put - $date1"  + "/testString2", "-------TEST STRING--------"); 
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1 $date1");
    assertTrue("Could not setSpaceQuota", shHDFS.getRet() == 0); 
    shHDFS.exec("for service in /etc/init.d/hadoop-hdfs-*; do sudo \$service stop; done");
    shHDFS.exec("for service in /etc/init.d/hadoop-hdfs-*; do sudo \$service start; done");
    shHDFS.exec("grep \"Quota violation in image for //user/hdfs/$date\" /var/log/hadoop-hdfs/hadoop-hdfs-namenode*.log");
    if (shHDFS.getOut().isEmpty()) {
      assertTrue("Log was not written", 1 == 0);
    } else {
      assertTrue(shHDFS.getOut().get(0).contains(date));
    }
    shHDFS.exec("grep \"Quota violation in image for //user/hdfs/$date1\" /var/log/hadoop-hdfs/hadoop-hdfs-namenode*.log");
    if (shHDFS.getOut().isEmpty()) {
      assertTrue("Log was not written", 1 == 0);
    } else {
      assertTrue(shHDFS.getOut().get(0).contains(date1));
    }
    
    shHDFS.exec("hdfs dfs -rm -r $date1");
    // following while loop is due to namenode going into safemode for about 15 seconds after being restarted
    while (shHDFS.getErr().get(0).contains("safe mode") || (shHDFS.getErr().size() > 1 && shHDFS.getErr().get(1).contains("safe mode"))) {
          shHDFS.exec("hdfs dfs -rm -r $date1");
    } 
  }

  @Test
  public void t8_testQuotasShouldFail() {
    String date = "/tmp/failTest" + quotaDate;
    shHDFS.exec("hdfs dfs -mkdir $date");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfs -put - $date" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    // Errors when setting quotas on a file
    shHDFS.exec("hdfs dfsadmin -setQuota 1000 $date/testString1");
    assertTrue("setting quota on a file should not have worked", shHDFS.getRet() != 0);
    if (KERBEROS == 'on') {
      String err_msg = shHDFS.getErr().toString()
      String exp_msg = "Superuser privilege is required"
      assertTrue("Unexpected error message recieved: $err_msg",err_msg.contains(exp_msg))
    }
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1000 $date/testString1");
    assertTrue("setting quota on a file should not have worked", shHDFS.getRet() != 0); 

    // Errors when clearing quotas on a file
    shHDFS.exec("hdfs dfsadmin -clrQuota $date/testString1");
    if (KERBEROS == 'on') {
      String err_msg = shHDFS.getErr().toString()
      String exp_msg = "Superuser privilege is required"
      assertTrue("Unexpected error message recieved: $err_msg",err_msg.contains(exp_msg))
    }
    assertTrue("clearing quota on a file should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hdfs dfsadmin -clrSpaceQuota $date/testString1");
    assertTrue("clearing quota on a file should not have worked", shHDFS.getRet() != 0);

    // set/clr quota on nonexistant directory
    shHDFS.exec("hdfs dfsadmin -setQuota 100 DIRECTORYDOESNOTEXIST" + date);
    if (KERBEROS == 'on') {
      String err_msg = shHDFS.getErr().toString()
      String exp_msg = "Superuser privilege is required"
      assertTrue("Unexpected error message recieved: $err_msg",err_msg.contains(exp_msg))
    }

    assertTrue("setting quota on non-existant directory should not have worked", shHDFS.getRet() != 0); 
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 100 DIRECTORYDOESNOTEXIST" + date);
    assertTrue("setting quota on non-existant directory should not have worked", shHDFS.getRet() != 0); 
    shHDFS.exec("hdfs dfsadmin -clrQuota DIRECTORYDOESNOTEXIST" + date);
    assertTrue("clearing quota on non-existant directory should not have worked", shHDFS.getRet() != 0); 
    shHDFS.exec("hdfs dfsadmin -clrSpaceQuota DIRECTORYDOESNOTEXIST" + date);
    assertTrue("clearing quota on non-existant directory should not have worked", shHDFS.getRet() != 0); 

    shHDFS.exec("hdfs dfs -rm -r $date"); 
  }

  @Test
  public void t9_testReplicationFactor() {
    assumeFalse(KERBEROS == 'on')
    // increasing/decreasing replication factor of a file should debit/credit quota
    String repFolder = "/tmp/repFactorTest" + quotaDate;
    shHDFS.exec("hdfs dfs -mkdir $repFolder");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);    
    shHDFS.exec("hdfs dfs -put - $repFolder" + "/testString1" , "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1000 $repFolder");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0); 
    shHDFS.exec("hdfs dfs -setrep 2 $repFolder/testString1");
    shHDFS.exec("hdfs dfs -count -q $repFolder");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] output = shHDFS.getOut().get(0).trim().split();   
    int size_of_two = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    shHDFS.exec("hdfs dfs -setrep 3 $repFolder/testString1");
    shHDFS.exec("hdfs dfs -count -q $repFolder");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    output = shHDFS.getOut().get(0).trim().split();   
    int size_of_three = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    assertTrue("Quota not debited correctly", size_of_two * 3 == size_of_three * 2);
    // shHDFS.exec("hdfs dfs -setrep 6 $repFolder/testString1");
    //shHDFS.exec("hdfs dfs -count -q $repFolder");
    //assertTrue("Could not use count command", shHDFS.getRet() == 0);
    // output = shHDFS.getOut().get(0).trim().split();   
    // int size_of_three = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    // assertTrue("Quota not credited correctly", size_of_one * 3 == size_of_three);
    shHDFS.exec("hdfs dfs -rm -r $repFolder"); 
  }
}
