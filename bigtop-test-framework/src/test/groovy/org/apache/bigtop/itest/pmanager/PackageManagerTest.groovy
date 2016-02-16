/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.pmanager

import org.apache.bigtop.itest.TestUtils
import org.apache.bigtop.itest.shell.OS
import org.junit.Assume
import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import org.apache.bigtop.itest.posix.Service
import static org.apache.bigtop.itest.pmanager.PackageManager.getPackageManager

class PackageManagerTest {
  PackageManager pmgr = getPackageManager("")
  private final String CRON_RPM

  {
    switch (OS.linux_flavor) {
      case ~/(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/:
        CRON_RPM = "cronie"
        break
      default:
        CRON_RPM = "cron"
    }
  }

  @Test
  void searchForGcc() {
    List<PackageInstance> pkgs = pmgr.search("gcc")

    assertFalse("gcc not found in repository", pkgs.findAll({
      return it.name =~ /^gcc.*/
    }).size() == 0)
  }

  @Test
  void testLookupGcc() {
    List<PackageInstance> pkgs = pmgr.lookup("gcc");

    assertFalse("gcc non found in repository", pkgs.size() == 0);
    assertFalse("can not get size for the gcc package", pkgs.get(0).getMeta()["size"]?.size() == 0);
  }

  @Test
  void installBash() {
    Assume.assumeTrue("Password-less sudo should be enabled", TestUtils.noPassSudo())
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
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, CRON_RPM)
    Map<String, Service> svcs = pmgr.getServices(cron)

    assertTrue("cron package is expected to provide at least one service", svcs.size() != 0)
  }

  @Test
  void testGetContentList() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, CRON_RPM);
    List<String> list = pmgr.getContentList(cron);
    list.each { println it };

    assertTrue("cron package is expected to contain at least ten files", list.size() > 10);
  }

  @Test
  void testGetDocs() {
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, CRON_RPM);
    List<String> list = pmgr.getDocs(cron);
    list.each { println it };

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
    PackageInstance cron = PackageInstance.getPackageInstance(pmgr, CRON_RPM);
    List<String> list = pmgr.getConfigs(cron);
    list.each { println it };

    assertTrue("cron package is expected to contain at least a few config files", list.size() > 0);
  }

  @Test
  void testRepoManagement() {
    Assume.assumeTrue("Password-less sudo should be enabled", TestUtils.noPassSudo())
    String repo_id = "test-repo";
    assertEquals("Can not add repo",
      0, pmgr.addBinRepo(repo_id, "http://127.0.0.1", null, "random strings here"));
    assertEquals("Can not remove repo",
      0, pmgr.removeBinRepo(repo_id));
  }

  @Test
  void testRepoFileManagement() {
    Assume.assumeTrue("Password-less sudo should be enabled", TestUtils.noPassSudo())
    String repo_id = "test-repo";
    assertEquals("Can not add repo",
      0, pmgr.addBinRepo(repo_id, "random strings here"));
    assertEquals("Can not remove repo",
      0, pmgr.removeBinRepo(repo_id));
  }
}
