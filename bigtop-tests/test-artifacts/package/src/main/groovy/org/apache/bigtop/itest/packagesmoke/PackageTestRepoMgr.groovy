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
package org.apache.bigtop.itest.packagesmoke

import org.apache.bigtop.itest.pmanager.PackageManager
import static org.apache.bigtop.itest.shell.OS.linux_codename
import static org.apache.bigtop.itest.shell.OS.linux_flavor


class PackageTestRepoMgr {
  PackageManager pm = PackageManager.getPackageManager();

  String btRepoVersion;
  String btRepoFileURL;
  String btRepoURL;
  String btKeyURL;
  String btRepoHost;

  String repoName;

  public PackageTestRepoMgr(String _btRepoVersion, String _btRepoFileURL, String _btRepoURL, String _btKeyURL) {
    btRepoVersion = _btRepoVersion;
    btRepoFileURL = _btRepoFileURL;
    btRepoURL = _btRepoURL;
    btKeyURL = _btKeyURL;
  }

  public PackageTestRepoMgr(String prefix) {
    parseRepoSpec(prefix);
  }

  public PackageTestRepoMgr() {
    parseRepoSpec("bigtop.repo");
  }

  public parseRepoSpec(String prefix) {
    btRepoHost = System.getProperty("${prefix}.host", "www.apache.org/dist/bigtop/stable/repos");
    btRepoVersion = System.getProperty("${prefix}.version", "bigtop");

    Map btKeys = [yum: "http://${btRepoHost}/GPG-KEY-bigtop",
      zypper: null,
      apt: "http://${btRepoHost}/GPG-KEY-bigtop"];
    Map btRepos = [yum: "http://${btRepoHost}/centos6",
      zypper: "http://${btRepoHost}/sles11",
      apt: "http://${btRepoHost}/precise/"];

    btRepoFileURL = System.getProperty("${prefix}.file.url.${linux_flavor.replaceAll(/\s/, '_')}",
      System.getProperty("${prefix}.file.url",
        "http://does.not.exist"));

    btRepoURL = System.getProperty("${prefix}.url", btRepos[pm.getType()]);
    btKeyURL = System.getProperty("${prefix}.key.url", btKeys[pm.getType()]);
  }

  public boolean addRepo() {
    repoName = "bigtop${btRepoVersion}";
    pm.cleanup();
    try {
      String repoText = btRepoFileURL.toURL().text;
      if (pm.addBinRepo(repoName, repoText, btKeyURL) != 0) {
        return false;
      }

    } catch (Throwable ex) {
      if (pm.addBinRepo(repoName, btRepoURL, btKeyURL, "${linux_codename}-bt${btRepoVersion} contrib")) {
        return false;
      }
    }
    pm.refresh();
    return true;
  }

  public boolean removeRepo() {
    return (pm.removeBinRepo(repoName) == 0);
  }
}
