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
package org.apache.bigtop.itest.hadoop.hdfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;

public class TestDFSAdmin {

  // set debugging variable to true if you want error messages sent to stdout
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");

  @Test
  public void testDFSbasic() {
    // report
    System.out.println("-report");
    shHDFS.exec("hdfs dfsadmin -report");
    assertTrue("-report failed", shHDFS.getRet() == 0);

    // help
    System.out.println("-help");
    shHDFS.exec("hdfs dfsadmin -help");
    assertTrue("-help failed", shHDFS.getRet() == 0);

    // printTopology
    System.out.println("-printTopology");
    shHDFS.exec("hdfs dfsadmin -printTopology");
    assertTrue("-printTopology failed", shHDFS.getRet() == 0);

    // metasave
    System.out.println("-metasave");
    shHDFS.exec("hdfs dfsadmin -metasave metasave_test");
    assertTrue("-metasave failed", shHDFS.getRet() == 0);
  }

  @Test
  public void testDFSsafemode() {
    // safemode
    System.out.println("-safemode");
    shHDFS.exec("hdfs dfsadmin -safemode leave");
    assertTrue("-safemode leave failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -safemode get");
    assertTrue("-safemode get failed", shHDFS.getOut().get(0) == "Safe mode is OFF");
    assertTrue("-safemode get failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -safemode enter");
    assertTrue("-safemode enter failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -safemode get");
    assertTrue("-safemode get failed", shHDFS.getOut().get(0) == "Safe mode is ON");
    assertTrue("-safemode get failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -safemode leave");
    assertTrue("-safemode leave failed", shHDFS.getRet() == 0);
  }

  @Test
  public void testDFSnamespace() {
    // saveNamespace
    System.out.println("-saveNamespace");
    shHDFS.exec("hdfs dfsadmin -safemode enter");
    shHDFS.exec("hdfs dfsadmin -saveNamespace");
    assertTrue("-saveNamespace failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -safemode leave");
    shHDFS.exec("hdfs dfsadmin -saveNamespace");
    assertTrue("-saveNamespace worked in non safemode", shHDFS.getRet() != 0);
  }

  @Test
  public void testDFSrefreshcommands() {
    // refreshNodes
    System.out.println("-refreshNodes");
    shHDFS.exec("hdfs dfsadmin -refreshNodes");
    assertTrue("-refreshNodes failed", shHDFS.getRet() == 0);

    /*// refreshServiceAcl - does not work - shHDFS.getRet() = 255
    System.out.println("-refreshServiceAcl");
    shHDFS.exec("hdfs dfsadmin -refreshServiceAcl");
    System.out.println(shHDFS.getRet());
    assertTrue("-refreshServiceAcl failed", shHDFS.getRet() == 0); */

    // refreshUserToGroupsMappings
    System.out.println("-refreshUserToGroupsMappings");
    shHDFS.exec("hdfs dfsadmin -refreshUserToGroupsMappings");
    assertTrue("-refreshUserToGroupsMappings failed", shHDFS.getRet() == 0);

    // refreshSuperUserGroupsConfiguration
    System.out.println("-refreshSuperUserGroupsConfiguration");
    shHDFS.exec("hdfs dfsadmin -refreshSuperUserGroupsConfiguration");
    assertTrue("-refreshSuperUserGroupsConfiguration failed", shHDFS.getRet() == 0);
  }

  @Test
  public void testDFSstorage() {
    // restoreFailedStorage
    System.out.println("-restoreFailedStorage");
    shHDFS.exec("hdfs dfsadmin -restoreFailedStorage false");
    assertTrue("-restoreFailedStorage false failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -restoreFailedStorage check");
    assertTrue("-restoreFailedStorage check failed", shHDFS.getOut().get(0) == "restoreFailedStorage is set to false");
    assertTrue("-restoreFailedStorage check failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -restoreFailedStorage true");
    assertTrue("-restoreFailedStorage true failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -restoreFailedStorage check");
    assertTrue("-restoreFailedStorage check", shHDFS.getOut().get(0) == "restoreFailedStorage is set to true");
    assertTrue("-restoreFailedStorage check", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -restoreFailedStorage false");
    assertTrue("-restoreFailedStorage false failed", shHDFS.getRet() == 0);
  }

  @Test
  public void testDFSquotas() {
    // setQuota, clrQuota
    System.out.println("-setQuota, -clrQuota");
    shHDFS.exec("date");
    String quota_test = "quota_test" + shHDFS.getOut().get(0).replaceAll("\\s", "").replaceAll(":", "");
    shHDFS.exec("hadoop fs -test -e $quota_test");
    if (shHDFS.getRet() == 0) {
      shHDFS.exec("hadoop fs -rmr -skipTrash $quota_test");
      assertTrue("Deletion of previous testDistcpInputs from HDFS failed",
        shHDFS.getRet() == 0);
    }
    shHDFS.exec("hadoop fs -mkdir -p $quota_test");
    shHDFS.exec("hdfs dfsadmin -setQuota 1000 $quota_test");
    assertTrue("-setQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -clrQuota $quota_test");
    assertTrue("-clrQuota failed", shHDFS.getRet() == 0);

    // setSpaceQuota, clrSpaceQuota
    System.out.println("-setSpaceQuota, -clrSpaceQuota");
    shHDFS.exec("hdfs dfsadmin -setSpaceQuota 1000 $quota_test");
    assertTrue("-setSpaceQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hdfs dfsadmin -clrSpaceQuota $quota_test");
    assertTrue("-clrSpaceQuota failed", shHDFS.getRet() == 0);
    shHDFS.exec("hadoop fs -rmr $quota_test");
  }

}
