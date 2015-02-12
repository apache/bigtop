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

import org.junit.rules.ErrorCollector
import org.apache.bigtop.itest.pmanager.PackageManager
import org.apache.bigtop.itest.pmanager.PackageInstance
import org.apache.bigtop.itest.posix.Service
import org.apache.bigtop.itest.posix.UGI
import org.apache.bigtop.itest.posix.Alternative
import junit.framework.AssertionFailedError
import static org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher

import static org.apache.bigtop.itest.packagesmoke.PackageTestMatchers.MapHasKeys.hasSameKeys
import org.apache.bigtop.itest.shell.Shell
import groovy.xml.MarkupBuilder

class PackageTestCommon {
  static public PackageManager pm;

  PackageInstance pkg;
  String name;

  ErrorCollector result;

  public void checkThat(String msg, Object value, Matcher<Object> matcher) {
    PackageTestErrorProxy.checkThat(result, msg, value, matcher);
  }

  public void checkThatService(String msg, Service svc, Matcher<Object> matcher) {
    if (PackageTestErrorProxy.checkEquals(svcStatusDecoder(svc.status()), matcher) == true) {
      return;
    } else {
      sleep(3001);
      if (PackageTestErrorProxy.checkEquals(svcStatusDecoder(svc.status()), matcher) == true) {
        return;
      } else {
        sleep(3001);
        PackageTestErrorProxy.checkThat(result, msg, svcStatusDecoder(svc.status()), matcher);
      }
    }
  }

  public void recordFailure(String message) {
    result.addError(new AssertionFailedError(message));
  }

  String formatDescription(String description, String summary) {
    return ((summary ?: "") + ' ' + description).replaceAll(/\s+/, ' ').replaceAll(/\s\.\s/, ' ').replaceAll(/\s\.$/, ' ').trim();
  }

  private void checkMetadataInternal(PackageInstance pkg, Map expected_metadata) {
    boolean noSummary = (pm.type == "apt");

    expected_metadata.each { key, expected ->
      String actual = (pkg.meta["$key"] ?: "").toString().replaceAll(/\n/, ' ');

      if (key == "summary") {
        if (noSummary) {
          expected = actual;
        } else {
          expected = formatDescription(expected, null);
          actual = formatDescription(actual, null);
        }
      }
      if (key == "description") {
        actual = formatDescription(actual, null);
        expected = formatDescription(expected, noSummary ? expected_metadata["summary"] : null);
      }

      checkThat("checking $key on package $name",
        actual, equalTo(expected));
    }
  }

  public void checkMetadata(Map expected_metadata) {
    checkMetadataInternal(pkg, expected_metadata);
  }

  public void checkRemoteMetadata(Map expected_metadata, boolean unique) {
    List<PackageInstance> pl = pm.lookup(name);

    if (pm.getType() == "zypper") {
      expected_metadata.remove("url");
    }

    if (unique && pl.size() != 1) {
      recordFailure("more than one package is available for name $name");
    } else if (pl.size() == 0) {
      recordFailure("can not find $name in the repository");
    } else {
      checkMetadataInternal(pl.get(0), expected_metadata);
    }
  }

  public void checkPulledDeps(Map expected_deps) {
    Map pkgDeps = [:];

    pkg.getDeps().each { k, v ->
      if (!(k =~ /\.so\.[0-9]/).find()) {
        pkgDeps[k] = v;
      }
    }

    checkThat("a set of dependencies of package $name is different from what was expected",
      pkgDeps, hasSameKeys(expected_deps));

    expected_deps.each { depName, version ->
      if (version == "/self") {
        PackageInstance dep = PackageInstance.getPackageInstance(pm, depName);
        dep.refresh();
        checkThat("checking that an expected dependecy $depName for the package $name has the same version",
          "${dep.getVersion()}-${dep.getRelease()}", equalTo("${pkg.getVersion()}-${pkg.getRelease()}"));
      }
      // checkThat("checking that and expected dependency $key for the package $name got pulled",
      //           dep.isInstalled(), equalTo(true));
    }
  }

  private String svcStatusDecoder(String status) {
    if ((status =~ /\.\.failed|not running/).find()) {
      return "stop";
    } else if ((status =~ /run|start/).find()) {
      return "start";
    } else {
      return "stop";
    }
  }

  void checkService(Service svc, Map svc_metadata) {
    String name = svc.getName();
    Map runlevels = [:];

    configService(svc, svc_metadata);

    // TODO: this should really be taken care of by the matcher
    svc.getRunLevels().each {
      runlevels[it] = it;
    }

    checkThat("wrong list of runlevels for service $name",
      runlevels, hasSameKeys(svc_metadata.runlevel));

    checkThatService("wrong state of service $name after installation",
      svc, equalTo(svc_metadata.oninstall));

    svc.stop();
    sleep(3001);
    checkThatService("service $name is expected to be stopped",
      svc, equalTo("stop"));

    if (svc_metadata.configured == "true") {
      checkThat("can not start service $name",
        svc.start(), equalTo(0));
      sleep(3001);
      checkThatService("service $name is expected to be started",
        svc, equalTo("start"));

      checkThat("can not restart service $name",
        svc.restart(), equalTo(0));
      sleep(3001);
      checkThatService("service $name is expected to be re-started",
        svc, equalTo("start"));

      checkThat("can not stop service $name",
        svc.stop(), equalTo(0));
      sleep(3001);
      checkThatService("service $name is expected to be stopped",
        svc, equalTo("stop"));
    }

    // Stopping 2nd time (making sure that a stopped service is
    // not freaked out by an extra stop)
    checkThat("can not stop an already stopped service $name",
      svc.stop(), equalTo(0));
    checkThatService("wrong status after stopping service $name for the second time",
      svc, equalTo("stop"));
  }

  public void checkServices(Map expectedServices) {
    Map svcs = pm.getServices(pkg);

    checkThat("wrong list of services in a package $name",
      expectedServices, hasSameKeys(svcs));

    expectedServices.each { key, value ->
      if (svcs[key] != null) {
        checkService(svcs[key], value);
      }
    }
  }

  private void configService(Service svc, Map svc_metadata) {
    Shell shRoot = new Shell("/bin/bash", "root");
    if (svc_metadata.config != null) {
      def config = svc_metadata.config;
      def configfile = svc_metadata.config.configfile;

      def configcontent = "";
      def property = new TreeMap(config.property);
      property.keySet().eachWithIndex() {
        v, j ->
          configcontent = configcontent + "<property>";
          property.get(v).eachWithIndex() {
            obj, i ->
              if (obj.toString().contains("name")) {
                configcontent = configcontent + "<name>" + obj.toString()[5..-1] + "</name>";
              } else {
                configcontent = configcontent + "<value>" + obj.toString()[6..-1] + "</value>";
              }
          };
          configcontent = configcontent + "</property>";
      };

      shRoot.exec("""sed -e '/\\/configuration/i \\ $configcontent' $configfile > temp.xml
                       mv temp.xml $configfile""");
    }
    if (svc_metadata.init != null) {
      svc.init();
    }
  }

  public void checkUsers(Map expectedUsers) {
    UGI ugi = new UGI();

    expectedUsers.each { key, value ->
      Map user = ugi.getUsers()[key];
      if (user != null) {
        checkThat("checking user $key home directory",
          user.home, equalTo(value.home));
        checkThat("checking user $key description",
          user.descr.replaceAll(/,*$/, ""), equalTo(value.descr));
        checkThat("checking user $key shell",
          user.shell, equalTo(value.shell));
      } else {
        recordFailure("package $name is epected to provide user $key");
      }
    }
  }

  public void checkGroups(Map expectedGroups) {
    UGI ugi = new UGI();

    expectedGroups.each { key, value ->
      Map group = ugi.getGroups()[key];
      if (group != null) {
        (value.user instanceof List ? value.user : [value.user]).each {
          checkThat("group $key is expected to contain user $it",
            group.users.contains(it), equalTo(true));
        }
      } else {
        recordFailure("package $name is epected to provide group $key");
      }
    }
  }

  public void checkAlternatives(Map expectedAlternatives) {
    expectedAlternatives.each { key, value ->
      Alternative alt = new Alternative(key);
      if (alt.getAlts().size() > 0) {
        checkThat("alternative link ${value.link} doesn't exist or does not point to /etc/alternatives",
          (new Shell()).exec("readlink ${value.link}").getOut().get(0),
          equalTo("/etc/alternatives/$key".toString()));

        checkThat("alternative $key has incorrect status",
          alt.getStatus(), equalTo(value.status));
        checkThat("alternative $key points to an unexpected target",
          alt.getValue(), equalTo(value.value));

        def altMap = [:];
        ((value.alt instanceof List) ? value.alt : [value.alt]).each {
          altMap[it] = it;
        }

        checkThat("alternative $key has incorrect set of targets",
          alt.getAlts(), hasSameKeys(altMap));
      } else {
        recordFailure("package $name is expected to provide alternative $key");
      }
    }
  }

  List sliceUp(List l, int chunks) {
    List res = [];
    int i = 0;
    while (i + chunks < l.size()) {
      res.add(l.subList(i, i + chunks));
      i += chunks;
    }
    res.add(l.subList(i, l.size()));
    return res;
  }

  Map getLsMetadata(List files) {
    Map lsFiles = [:];

    sliceUp(files, 500).each { files_chunk ->
      (new Shell()).exec("ls -ld '${files_chunk.join('\' \'')}'").out.each {
        String fileName = it.replaceAll('^[^/]*/', "/");
        def matcher = (it =~ /\S+/);

        Map meta = [:];
        if ((fileName =~ /->/).find()) {
          meta.target = fileName.replaceAll(/^.*-> /, '');
          fileName = fileName.replaceAll(/ ->.*$/, '');
        }
        meta.perm = matcher[0].replace('.', '');
        meta.user = matcher[2];
        meta.group = matcher[3];
        lsFiles[fileName] = meta;
      }
    }
    return lsFiles;
  }

  // TODO: we might need to check that the files are actually owned by the installing package
  // although this does sound like testing package manager itself
  void updateOwners(Map files) {
    Shell sh = new Shell();
    List fileList = files.keySet().toList();

    if (pm.type == "apt") {
      int curFile = 0;
      sliceUp(fileList, 500).each { fileList_chunk ->
        sh.exec("dpkg -S '${fileList_chunk.join('\' \'')}'").out.each {
          String n = it.replaceAll(/^.*: \//, "/");
          while (fileList[curFile] != n) {
            files[fileList[curFile]].owners = 0;
            curFile++;
          }
          files[n].owners = it.replaceAll(/: \/.*$/, "").split(',').size();
          curFile++;
        }
      }
    } else {
      int curFile = -1;
      sliceUp(fileList, 500).each { fileList_chunk ->
        sh.exec("rpm -qf /bin/cat '${fileList_chunk.join('\' /bin/cat \'')}'").out.each {
          if ((it =~ /^coreutils/).find()) {
            curFile++;
            files[fileList[curFile]].owners = 0;
          } else if (!(it =~ /not owned by any package/).find()) {
            files[fileList[curFile]].owners++;
          }
        }
      }
    }
  }

  String formatFileName(String file) {
    return file.replace("${pkg.getVersion()}", 'BIGTOP-PACKAGE-VERSION');
  }

  String restoreFileName(String file) {
    return file.replace('BIGTOP-PACKAGE-VERSION', "${pkg.getVersion()}");
  }

  public void checkFiles(Map config, Map doc, Map file) {
    String fName;
    Map files = [:];
    Map docs = [:];
    Map configs = [:];

    pkg.getFiles().each { fName = formatFileName(it); files[fName] = fName; }
    pkg.getConfigs().each {
      fName = formatFileName(it); configs[fName] = fName; files.remove(fName);
    }
    pkg.getDocs().each {
      fName = formatFileName(it); docs[fName] = fName; files.remove(fName);
    }

    if (pm.type == "apt" && doc != null) {
      file.putAll(doc);
    } else {
      checkThat("list of documentation files of package $name is different from what was expected",
        docs, hasSameKeys(doc));
    }
    checkThat("list of config files of package $name is different from what was expected",
      configs, hasSameKeys(config));
    checkThat("list of regular files of package $name is different from what was expected",
      files, hasSameKeys(file));

    // TODO: we should probably iterate over a different set of files to include loose files as well
    List fileList = [];
    file.each { fileList.add(restoreFileName(it.key)); }
    doc.each { fileList.add(restoreFileName(it.key)); }
    config.each { fileList.add(restoreFileName(it.key)); }
    fileList.sort();

    Map fileMeta = getLsMetadata(fileList);
    if (fileMeta.size() != 0) {
      updateOwners(fileMeta); // this is an expensive (and fragile operation)
    }

    List problemFiles = [];
    Map goldenFileMeta = [:];
    goldenFileMeta.putAll(file ?: [:]);
    goldenFileMeta.putAll(config ?: [:]);
    goldenFileMeta.putAll(doc ?: [:]);

    fileList.each {
      Map meta = fileMeta[it];
      Map goldenMeta = goldenFileMeta[formatFileName(it)];

      if (goldenMeta.owners != "-1") {
        // TODO: we shouldn't really skip anything even for multi-owned dirs
        if (meta == null ||
          !meta.perm.equals(goldenMeta.perm) ||
          !meta.user.equals(goldenMeta.user) ||
          !meta.group.equals(goldenMeta.group) ||
          (goldenMeta.target != null && !goldenMeta.target.equals(meta.target)) ||
          (Integer.parseInt(goldenMeta.owners) == 1 && !meta.owners.toString().equals(goldenMeta.owners))) {
          problemFiles.add(it);
        }
      }
    }
    checkThat("file metadata difference detected on the following files",
      problemFiles, equalTo([]));

    // a bit of debug output
    def newManifest = new MarkupBuilder(new FileWriter("${pkg.name}.xml"));

    newManifest."${pkg.name}" {
      content {
        fileMeta = getLsMetadata(pkg.getFiles());

        pkg.getFiles().each {
          fName = formatFileName(it);
          Map meta = fileMeta[it] ?: [:];
          String node = configs[fName] ? "config" : (docs[fName] ? "doc " : "file");
          int owners = meta.owners ?: -1;

          if (meta.target) {
            "$node"(name: fName, owners: owners, perm: meta.perm, user: meta.user, group: meta.group, target: meta.target);
          } else {
            "$node"(name: fName, owners: owners, perm: meta.perm, user: meta.user, group: meta.group);
          }
        }
      }
    }
  }

  public void checkComplimentary32bitInstall() {
    // RedHat (5.X) systems are THE ONLY place where we can try
    // installing amd64 and i386 packages at the same time. On top of
    // that we have a pretty weird policy on which pairs are supposed
    // to go together (short answer is -- not all of them).
    Map complimentaryPackages = [
      "hadoop-0.20-sbin.x86_64": "hadoop-0.20-sbin.i386",
      "hadoop-0.20-pipes.x86_64": "hadoop-0.20-pipes.i386",
      "hadoop-0.20-native.x86_64": "hadoop-0.20-native.i386",
      "hadoop-0.20-libhdfs.x86_64": "hadoop-0.20-libhdfs.i386",
      "hadoop-0.20-debuginfo.x86_64": "hadoop-0.20-debuginfo.i386",
    ];

    if (complimentaryPackages[name] != null) {
      PackageInstance pkg386 = PackageInstance.getPackageInstance(pm, complimentaryPackages[name]);

      checkThat("complimentary native package ${pkg386.getName()} failed to be installed",
        pkg386.install(), equalTo(0));
      checkThat("complimentary native package ${pkg386.getName()} failed to be removed",
        pkg386.remove(), equalTo(0));
    }
  }

  public void checkPackageFilesGotRemoved(Map files) {
    List allFiles = [];
    (files.file ?: [:]).each {
      if (it.value.owners == "1") {
        allFiles.add(it.key)
      }
    };
    (files.doc ?: [:]).each {
      if (it.value.owners == "1") {
        allFiles.add(it.key)
      }
    };
    (files.config ?: [:]).each {
      if (it.value.owners == "1") {
        allFiles.add(it.key)
      }
    };

    allFiles.each {
      checkThat("file $it still present after package is being removed",
        (new File(it)).exists(), equalTo(false));
    }
  }

  public void checkDaemonStart() {
    // We need to start service for one more time to make sure that package
    // removal would succeed even when services are still running
    pkg.getServices().each { name, svc ->
      checkThat("can not start service $name",
        svc.start(), equalTo(0));
    }
  }

  static public boolean isUpgrade() {
    return System.getProperty("bigtop.prev.repo.version", "") != "";
  }
}
