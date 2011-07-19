/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.itest.pmanager

import com.cloudera.itest.posix.Service

class YumCmdLinePackageManager extends PackageManager {
  static String type  = "yum";
  static String repository_registry = "/etc/yum.repos.d/%s.repo";

  public void setDefaults(String defaults) {}

  public int addBinRepo(String record, String url, String key, String cookie) {
    String descr = """[${cookie.replaceAll(/\s+/, '-')}]
name="${cookie}"
baseurl=${url}
gpgkey=${key}
gpgcheck=${(key!=null)?1:0}""";

    return addBinRepo(record, descr);
  }

  public int refresh() {
    // FIXME: really?
    return 0;
  }

   public int cleanup() {
    shRoot.exec("yum clean all");
    return shRoot.getRet();
  }

  public List<PackageInstance> search(String name) {
    def packages = new ArrayList<PackageInstance>();
    shUser.exec("yum --color=never -d 0 search $name").out.each {
      if (!(it =~ /^(===================| +: )/)) {
        packages.add(PackageInstance.getPackageInstance (this, it.replaceAll(/\.(noarch|i386|x86_64).*$/, '')))
      }
    }
    return packages
  }

  public List<PackageInstance> lookup(String name) {
    shUser.exec("yum --color=never -d 0 info $name");
    return (shUser.getRet() == 0) ? RPMPackage.parseMetaOutput(null, shUser.out, this) : [];
  }

  public int install(PackageInstance pkg) {
    shRoot.exec("yum -y install ${pkg.name}");
    pkg.installMessages = shRoot.getOut().join('\n');
    return shRoot.getRet();
  }

  public int remove(PackageInstance pkg) {
    shRoot.exec("yum -y erase ${pkg.name}");
    return shRoot.getRet();
  }

  public boolean isInstalled(PackageInstance pkg) {
    def text = shUser.exec("yum --color=never -d 0 info ${pkg.name}").out.join('\n')
    return (text =~ /(?m)^Repo\s*:\s*installed/).find()
  }
}
