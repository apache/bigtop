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

import org.junit.After
import org.junit.Before
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;

public class TestHDFSQuota {

  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");
  private static Shell sh = new Shell("/bin/bash");
  private static final long LARGE = Long.MAX_VALUE - 1;
  private static final String USERNAME = System.getProperty("user.name");
  private static String quotaDate = shHDFS.exec("date").getOut().get(0).replaceAll("\\s", "").replaceAll(":", "");
  private static String testQuotaFolder = "/tmp/testQuotaFolder" + quotaDate;
  private static String testQuotaFolder1 = testQuotaFolder + "1";
  private static String testQuotaFolder2 = testQuotaFolder + "2";
  private static String testQuotaFolder3 = testQuotaFolder + "3";

  @Before
  public void setUp() {
    // creating test folders
    shHDFS.exec("hadoop fs -mkdir $testQuotaFolder1");
    assertTrue("Could not create input directory", shHDFS.getRet() == 0);

    sh.exec("hadoop fs -mkdir $testQuotaFolder2");
    assertTrue("Could not create input directory", sh.getRet() == 0);
  }

  @After
  public void tearDown() {
    // clean up of existing folders
    shHDFS.exec("hadoop fs -test -e $testQuotaFolder1");
    if (shHDFS.getRet() == 0) {
      shHDFS.exec("hadoop fs -rmr -skipTrash $testQuotaFolder1");
      assertTrue("Deletion of previous testQuotaFolder1 from HDFS failed",
        shHDFS.getRet() == 0);
    }
    shHDFS.exec("hadoop fs -test -e $testQuotaFolder2");
    if (shHDFS.getRet() == 0) {
      shHDFS.exec("hadoop fs -rmr -skipTrash $testQuotaFolder2");
      assertTrue("Deletion of previous testQuotaFolder2 from HDFS failed",
        shHDFS.getRet() == 0);
    }
    sh.exec("hadoop fs -test -e $testQuotaFolder1");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $testQuotaFolder1");
      assertTrue("Deletion of previous testQuotaFolder1 from HDFS failed",
        sh.getRet() == 0);
    }
  }

  @Test
  public void testNewlyCreatedDir() {
    // newly created dir should have no name quota, no space quota   
    shHDFS.exec("hadoop fs -count -q $testQuotaFolder1");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] output = shHDFS.getOut().get(0).trim().split();
    assertTrue("Newly created directory had a set name quota", output[0].equals("none"));
    assertTrue("Newly created directory had a set name quota left", output[1].equals("inf"));
    assertTrue("Newly created directory had a set space quota", output[2].equals("none"));
    assertTrue("Newly created directory had a set space quota left", output[3].equals("inf"));
  }

  @Test
  public void testAdminPermissions() {
    // admin setting quotas should succeed
    shHDFS.exec("hadoop dfsadmin -setQuota 10 $testQuotaFolder1");
    assertTrue("setQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1000000 $testQuotaFolder1");
    assertTrue("setSpaceQuota failed", shHDFS.getRet() == 0);

    // non-admin setting/clearing quotas should fail
    sh.exec("hadoop dfsadmin -setQuota 10 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", sh.getRet() != 0);
    sh.exec("hadoop dfsadmin -setSpaceQuota 1000000 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", sh.getRet() != 0);
    sh.exec("hadoop dfsadmin -clrQuota $testQuotaFolder1");
    assertTrue("clrQuota should not have worked", sh.getRet() != 0);
    sh.exec("hadoop dfsadmin -clrSpaceQuota $testQuotaFolder1");
    assertTrue("clrSpaceQuota should not have worked", sh.getRet() != 0);

    // admin clearing quotas should succeed
    shHDFS.exec("hadoop dfsadmin -clrQuota $testQuotaFolder1");
    assertTrue("clrQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -clrSpaceQuota $testQuotaFolder1");
    assertTrue("clrSpaceQuota failed", shHDFS.getRet() == 0);
  }

  @Test
  public void testRename() {
    // name and space quotas stick after rename
    shHDFS.exec("hadoop fs -count -q $testQuotaFolder1");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] status1 = shHDFS.getOut().get(0).trim().split();
    shHDFS.exec("hadoop fs -mv $testQuotaFolder1" + " $testQuotaFolder3");
    assertTrue("Could not use move command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -count -q $testQuotaFolder3");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] status2 = shHDFS.getOut().get(0).trim().split();
    for (int i = 0; i < status1.length - 1; i++) {
      assertTrue("quotas changed after folder rename", status1[i].equals(status2[i]));
    }
    shHDFS.exec("hadoop fs -mv $testQuotaFolder3" + " $testQuotaFolder1");
    assertTrue("Could not use move command", shHDFS.getRet() == 0);
  }

  @Test
  public void testInputValues() {
    // the largest allowable quota size is Long.Max_Value and must be greater than zero
    shHDFS.exec("hadoop dfsadmin -setQuota -1 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota -1 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 1.04 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1.04 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 0 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setQuota $LARGE $testQuotaFolder1");
    assertTrue("setQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota $LARGE $testQuotaFolder1");
    assertTrue("setSpaceQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 9223372036854775808 $testQuotaFolder1");
    assertTrue("setQuota should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 9223372036854775808 $testQuotaFolder1");
    assertTrue("setSpaceQuota should not have worked", shHDFS.getRet() != 0);
  }

  @Test
  public void testForceDirEmpty() {
    // setting the name quota to 1 for an empty dir will cause the dir to remain empty
    shHDFS.exec("hadoop dfsadmin -setQuota 1 $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -mkdir $testQuotaFolder1" + "/sample1");
    assertTrue("mkdir should not have worked due to quota of 1", shHDFS.getRet() != 0);
  }

  @Test
  public void testQuotasPostViolation() {
    // quota can be set even if it violates
    shHDFS.exec("hadoop dfsadmin -setQuota $LARGE $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $testQuotaFolder1" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -mkdir $testQuotaFolder1" + "/sample1");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -mkdir $testQuotaFolder1" + "/sample2");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 2 $testQuotaFolder1");
    assertTrue("setQuota should have worked", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1 $testQuotaFolder1");
    assertTrue("setSpaceQuota should have worked", shHDFS.getRet() == 0);
    //testQuotas
    // dir creation should fail - name quota
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 10000000000 $testQuotaFolder1");
    assertTrue("Could not setSpaceQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -mkdir $testQuotaFolder1" + "/sample3");
    assertTrue("mkdir should not have worked", shHDFS.getRet() != 0);

    // file creation should fail - name quota
    shHDFS.exec("hadoop fs -rmr $testQuotaFolder1" + "/testString1");
    shHDFS.exec("hadoop fs -put - $testQuotaFolder1" + "/testString2", "-------TEST STRING--------");
    assertTrue("put should not have worked", shHDFS.getRet() != 0);

    // file creation should fail - space quota
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 10 $testQuotaFolder1");
    assertTrue("Could not setSpaceQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 1000 $testQuotaFolder1");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $testQuotaFolder1" + "/testString3", "-------TEST STRING--------");
    assertTrue("put should not have worked", shHDFS.getRet() != 0);
  }

  //@Test - can be reinstated upon resolution of BIGTOP-635 due to restarting of hdfs service
  public void testLogEntries() {
    // Log entry created when nodes are started with both quota violations
    String date = "logTest" + quotaDate;
    shHDFS.exec("hadoop fs -mkdir $date");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $date" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setQuota 1 $date");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("date");
    String date1 = "logTest" + shHDFS.getOut().get(0).replaceAll("\\s", "").replaceAll(":", "");
    shHDFS.exec("hadoop fs -mkdir $date1");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $date1" + "/testString2", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1 $date1");
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

    shHDFS.exec("hadoop fs -rmr $date1");
    // following while loop is due to namenode going into safemode for about 15 seconds after being restarted
    while (shHDFS.getErr().get(0).contains("safe mode") || (shHDFS.getErr().size() > 1 && shHDFS.getErr().get(1).contains("safe mode"))) {
      shHDFS.exec("hadoop fs -rmr $date1");
    }
  }

  @Test
  public void testQuotasShouldFail() {
    String date = "/tmp/failTest" + quotaDate;
    shHDFS.exec("hadoop fs -mkdir $date");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $date" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    // Errors when setting quotas on a file
    shHDFS.exec("hadoop dfsadmin -setQuota 1000 $date/testString1");
    assertTrue("setting quota on a file should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1000 $date/testString1");
    assertTrue("setting quota on a file should not have worked", shHDFS.getRet() != 0);

    // Errors when clearing quotas on a file
    shHDFS.exec("hadoop dfsadmin -clrQuota $date/testString1");
    assertTrue("clearing quota on a file should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -clrSpaceQuota $date/testString1");
    assertTrue("clearing quota on a file should not have worked", shHDFS.getRet() != 0);

    // set/clr quota on nonexistant directory
    shHDFS.exec("hadoop dfsadmin -setQuota 100 DIRECTORYDOESNOTEXIST" + date);
    assertTrue("setting quota on non-existant directory should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 100 DIRECTORYDOESNOTEXIST" + date);
    assertTrue("setting quota on non-existant directory should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -clrQuota DIRECTORYDOESNOTEXIST" + date);
    assertTrue("clearing quota on non-existant directory should not have worked", shHDFS.getRet() != 0);
    shHDFS.exec("hadoop dfsadmin -clrSpaceQuota DIRECTORYDOESNOTEXIST" + date);
    assertTrue("clearing quota on non-existant directory should not have worked", shHDFS.getRet() != 0);

    shHDFS.exec("hadoop fs -rmr $date");
  }

  @Test
  public void testReplicationFactor() {
    // increasing/decreasing replication factor of a file should debit/credit quota
    String repFolder = "/tmp/repFactorTest" + quotaDate;
    shHDFS.exec("hadoop fs -mkdir $repFolder");
    assertTrue("Could not use mkdir command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -put - $repFolder" + "/testString1", "-------TEST STRING--------");
    assertTrue("Could not use put command", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop dfsadmin -setSpaceQuota 1000 $repFolder");
    assertTrue("Could not setQuota", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -setrep 1 $repFolder/testString1");
    shHDFS.exec("hadoop fs -count -q $repFolder");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    String[] output = shHDFS.getOut().get(0).trim().split();
    int size_of_one = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    shHDFS.exec("hadoop fs -setrep 5 $repFolder/testString1");
    shHDFS.exec("hadoop fs -count -q $repFolder");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    output = shHDFS.getOut().get(0).trim().split();
    int size_of_five = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    assertTrue("Quota not debited correctly", size_of_one * 5 == size_of_five);
    shHDFS.exec("hadoop fs -setrep 3 $repFolder/testString1");
    shHDFS.exec("hadoop fs -count -q $repFolder");
    assertTrue("Could not use count command", shHDFS.getRet() == 0);
    output = shHDFS.getOut().get(0).trim().split();
    int size_of_three = Integer.parseInt(output[2]) - Integer.parseInt(output[3]);
    assertTrue("Quota not credited correctly", size_of_one * 3 == size_of_three);
    shHDFS.exec("hadoop fs -rmr $repFolder");
  }

}

