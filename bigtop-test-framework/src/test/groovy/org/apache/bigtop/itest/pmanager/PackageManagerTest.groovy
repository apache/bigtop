/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.pmanager

import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.apache.bigtop.itest.pmanager.PackageManager.getPackageManager
import org.apache.bigtop.itest.posix.Service

class PackageManagerTest {
  PackageManager pmgr = getPackageManager("")

  @Test
  void searchForGcc() {
    List<PackageInstance> pkgs = pmgr.search("gcc")

    assertFalse("gcc non found in repository", pkgs.size() == 0)
    assertEquals("package name searched for differs from the result", "gcc", pkgs.get(0).name)
  }

  @Test
  void testLookupGcc() {
    List<PackageInstance> pkgs = pmgr.lookup("gcc");

    assertFalse("gcc non found in repository", pkgs.size() == 0);
    assertFalse("can not get description for the gcc package", pkgs.get(0).getMeta()["description"].length() == 0);
  }

  @Test
  void installBash() {
    PackageInstance bash_pkg = PackageInstance.getPackageInstance(pmgr, "bash");

    assertTrue("can not install pacakge bash", (bash_pkg.install() == 0));
  }

  @Test
  void isInstalledBash() {
    PackageInstance bash_pkg = PackageInstance.getPackageInstance(pmgr, "bash")

    assertTrue("bash is not installed on your system", pmgr.isInstalled(bash_pkg))
  }

  @Test
  void testGetServicesCron() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, "cron")
    Map<String, Service> svcs = pmgr.getServices(cron)

    assertTrue("cron package is expected to provide at least one service", svcs.size() != 0)
  }

  @Test
  void testGetContentList() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, "cron");
    List<String> list = pmgr.getContentList(cron);
    list.each { println it};

    assertTrue("cron package is expected to contain at least ten files", list.size() > 10);
  }

  @Test
  void testGetDocs() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, "cron");
    List<String> list = pmgr.getDocs(cron);
    list.each { println it};

    assertTrue("checking for docs in cron package",
               list.size() > ((pmgr.getType() == "apt") ? -1 : 0));
  }

  @Test
  void testGetDeps() {
    PackageInstance bash = PackageInstance.getPackageInstance(pmgr, "bash");
    Map<String, String> deps = bash.getDeps();

    assertTrue("package bash has 0 dependencies. weird.",
               deps.size() > 0);
  }


  @Test
  void testGetConfigs() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, "cron");
    List<String> list = pmgr.getConfigs(cron);
    list.each { println it};

    assertTrue("cron package is expected to contain at least a few config files", list.size() > 0);
  }

  @Test
  void testRepoManagement() {
    String repo_id = "test-repo";
    assertEquals("Can not add repo",
                 0, pmgr.addBinRepo(repo_id, "http://127.0.0.1", null, "random strings here"));
    assertEquals("Can not remove repo",
                 0, pmgr.removeBinRepo(repo_id));
  }

  @Test
  void testRepoFileManagement() {
    String repo_id = "test-repo";
    assertEquals("Can not add repo",
                 0, pmgr.addBinRepo(repo_id, "random strings here"));
    assertEquals("Can not remove repo",
                 0, pmgr.removeBinRepo(repo_id));
  }
}
