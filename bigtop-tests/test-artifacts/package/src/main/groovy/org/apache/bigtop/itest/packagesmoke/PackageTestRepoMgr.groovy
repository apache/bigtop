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

  String cdhRepoVersion;
  String cdhRepoFileURL;
  String cdhRepoURL;
  String cdhKeyURL;
  String cdhRepoHost;

  String repoName;

  public PackageTestRepoMgr(String _cdhRepoVersion, String _cdhRepoFileURL, String _cdhRepoURL, String _cdhKeyURL) {
    cdhRepoVersion = _cdhRepoVersion;
    cdhRepoFileURL = _cdhRepoFileURL;
    cdhRepoURL = _cdhRepoURL;
    cdhKeyURL = _cdhKeyURL;
  }

  public PackageTestRepoMgr(String prefix) {
    parseRepoSpec(prefix);
  }

  public PackageTestRepoMgr() {
    parseRepoSpec("cdh.repo");
  }

  public parseRepoSpec(String prefix) {
    cdhRepoHost = System.getProperty("${prefix}.host", "nightly.cloudera.com");
    cdhRepoVersion = System.getProperty("${prefix}.version", "bigtop");

    Map cdhKeys  = [ yum    : "http://${cdhRepoHost}/redhat/cdh/RPM-GPG-KEY-cloudera",
                     zypper : null,
                     apt    : "http://${cdhRepoHost}/debian/archive.key" ];
    Map cdhRepos = [ yum    : "http://${cdhRepoHost}/redhat/cdh/${cdhRepoVersion}",
                     zypper : "http://${cdhRepoHost}/sles/11/x86_64/cdh/${cdhRepoVersion}",
                     apt    : "http://${cdhRepoHost}/debian/" ];

    cdhRepoFileURL = System.getProperty("${prefix}.file.url.${linux_flavor.replaceAll(/\s/,'_')}",
                       System.getProperty("${prefix}.file.url",
                         "http://does.not.exist"));

    cdhRepoURL = System.getProperty("${prefix}.url", cdhRepos[pm.getType()]);
    cdhKeyURL = System.getProperty("${prefix}.key.url", cdhKeys[pm.getType()]);
  }

  public boolean addRepo() {
    repoName = "cloudera-cdh${cdhRepoVersion}";
    pm.cleanup();
    try {
      String repoText = cdhRepoFileURL.toURL().text;
      if (pm.addBinRepo(repoName, repoText, cdhKeyURL)) {
        return false;
      }

    } catch (Throwable ex) {
      if (pm.addBinRepo(repoName, cdhRepoURL, cdhKeyURL, "${linux_codename}-cdh${cdhRepoVersion} contrib")) {
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
