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

import org.apache.bigtop.itest.posix.Service

class UrpmiCmdLinePackageManager extends PackageManager {
  String type = "urpmi";

  public void setDefaults(String defaults) {}

  public int addBinRepo(String record, String url, String key, String cookie) {
    shRoot.exec("urpmi.addmedia '${record}' $url");
    return shRoot.getRet();
  }

  public int removeBinRepo(String record) {
    shRoot.exec("urpmi.removemedia -q '${record}'");
    return shRoot.getRet();
  }

  public int refresh() {
    shRoot.exec("urpmi.update -a");
    return shRoot.getRet();
  }

  public int cleanup() {
    shRoot.exec("urpmi --clean");
    return shRoot.getRet();
  }

  public List<PackageInstance> search(String name) {
    def packages = new ArrayList<PackageInstance>();
    shUser.exec("urpmq ${name} | sed -e 's/|/\\n/g' | uniq").out.each {
      packages.add(PackageInstance.getPackageInstance(this, it))
    }
    return packages
  }

  public List<PackageInstance> lookup(String name) {
    shUser.exec("urpmq -i $name | sed -re \"s/(Size\\s+:\\s[0-9]+)\\s+/\\1\\n/\" ");
    return (shUser.getRet() == 0) ? RPMPackage.parseMetaOutput(null, shUser.out, this) : [];
  }

  public int install(PackageInstance pkg) {
    shRoot.exec("urpmi --verbose --force --auto ${pkg.name}");
    pkg.installMessages = shRoot.getOut().join('\n');
    return shRoot.getRet();
  }

  public int remove(PackageInstance pkg) {
    shRoot.exec("urpme --verbose --force --auto ${pkg.name}");
    return shRoot.getRet();
  }

  public boolean isInstalled(PackageInstance pkg) {
    shUser.exec("rpm -q ${pkg.name}")
    return (shUser.getRet() == 0)
  }
}
