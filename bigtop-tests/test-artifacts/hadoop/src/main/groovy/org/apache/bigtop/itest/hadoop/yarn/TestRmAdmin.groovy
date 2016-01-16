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
package org.apache.bigtop.itest.hadoop.yarn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;

public class TestRmAdmin {

  // set debugging variable to true if you want error messages sent to stdout
  private static Shell sh = new Shell("/bin/bash");

  @Test (timeout = 0x45000l)
  public void testRmAdminBasic() {
    // help
    System.out.println("-help");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -help");
    assertTrue("-help failed", sh.getRet() == 0);

    // getGroups
    System.out.println("-getGroups");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -getGroups");
    assertTrue("-getGroups failed", sh.getRet() == 0);
  }

  @Test (timeout = 0x45000l)
  public void testRmAdminRefreshcommands() {
    // refreshQueues
    System.out.println("-refreshQueues");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshQueues");
    assertTrue("-refreshQueues failed", sh.getRet() == 0);

    // refreshNodes
    System.out.println("-refreshNodes");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshNodes");
    assertTrue("-refreshNodes failed", sh.getRet() == 0);

    // refreshUserToGroupsMappings
    System.out.println("-refreshUserToGroupsMappings");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshUserToGroupsMappings");
    assertTrue("-refreshUserToGroupsMappings failed", sh.getRet() == 0);

    // refreshSuperUserGroupsConfiguration
    System.out.println("-refreshSuperUserGroupsConfiguration");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshSuperUserGroupsConfiguration");
    assertTrue("-refreshSuperUserGroupsConfiguration failed", sh.getRet() == 0);

    // refreshAdminAcls
    System.out.println("-refreshAdminAcls");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshAdminAcls");
    assertTrue("-refreshAdminAcls failed", sh.getRet() == 0);

    /*// refreshServiceAcl - does not work - shHDFS.getRet() = 255
    System.out.println("-refreshServiceAcl");
    sh.exec("YARN_ROOT_LOGGER=WARN,console yarn rmadmin -refreshServiceAcl");
    assertTrue("-refreshServiceAcl failed", sh.getRet() == 0);*/

  }

}
