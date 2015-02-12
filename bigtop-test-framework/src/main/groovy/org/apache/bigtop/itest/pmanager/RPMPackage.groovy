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
import org.apache.bigtop.itest.shell.Shell

class RPMPackage extends ManagedPackage {
  Shell shRoot = new Shell("/bin/bash -s", "root");
  Shell shUser = new Shell();

  /**
   * A helper method that parses a stream of metadata describing one or more packages.
   *
   * NOTE: the design of this is a bit ugly, we need to rethink how it is done.
   *
   * @param givenPkg a package to which the beginning of the metadata stream applies
   * @param text stream of metadata as a list of Strings
   * @param pm a package manager that this package belong to
   * @return list of EXTRA packages that were detected (and created!) during metadata parsing
   */
  public static List parseMetaOutput(PackageInstance givenPkg, List<String> text, PackageManager pm) {
    def packages = new ArrayList<PackageInstance>();
    PackageInstance pkg = givenPkg;
    String curMetaKey = "";
    text.each {
      // theoretically RPM can generate multiline output for any field, but we are only allowing description & summary
      if (curMetaKey == "description" || ((it =~ /^\s+: /).find() && curMetaKey == "summary")) {
        pkg.meta[curMetaKey] <<= "\n${it.replaceAll(/^\s+:/, '')}";
      } else {
        def m = (it =~ /(\S+)\s*:\s*(.*)/);
        if (m.size()) {
          String[] matcher = m[0];
          if ("Name" == matcher[1] && !givenPkg) {
            pkg = PackageInstance.getPackageInstance(pm, matcher[2]);
            packages.add(pkg);
          } else if (pkg) {
            curMetaKey = matcher[1].toLowerCase();
            pkg.meta[curMetaKey] = matcher[2].trim();
          }
        }
      }
    }

    (packages.size() == 0 ? [givenPkg] : packages).each {
      it.version = it.meta["version"] ?: it.version;
      it.release = it.meta["release"] ?: it.release;
      it.arch = it.meta["arch"] ?: it.arch;
    };
    return packages;
  }

  @Override
  public void refresh() {
    // maintainer is missing from RPM ?
    String q = """
Name: %{NAME}
Arch: %{ARCH}
Version: %{VERSION}
Release: %{RELEASE}
Summary: %{SUMMARY}
URL: %{URL}
License: %{LICENSE}
Vendor: %{VENDOR}
Group: %{GROUP}
Depends: [%{REQUIRES}\t]
Breaks: [%{CONFLICTS}\t]
Replaces: [%{OBSOLETES}\t]
Provides: [%{PROVIDES}\t]
Distribution: %{DISTRIBUTION}
OS: %{OS}
Source: %{SOURCERPM}
Description: %{DESCRIPTION}
""";
    parseMetaOutput(this, shUser.exec("rpm -q --qf '$q' $name").out, mgr);
  }

  public Map<String, Service> getServices() {
    Map res = [:];
    String transform;

    switch (mgr.getType()) {
      case "zypper":
        transform = "sed -ne '/^.etc.rc.d./s#^.etc.rc.d.##p'"
        break
      case "yum":
        transform = "sed -ne '/^.usr.sbin./s#^.usr.sbin.##p'"
        break
      default:
        transform = "sed -ne '/^.etc.rc.d.init.d./s#^.etc.rc.d.init.d.##p'"
        break
    }

    shUser.exec("rpm -ql $name | $transform").out.collect {
      res[it] = new Service(it);
    }
    return res;
  }

  List<String> getFiles() {
    shUser.exec("rpm -ql $name | grep -v '^(contains no files)\$'");
    return shUser.out.collect({ "$it" });
  }

  List<String> getConfigs() {
    shUser.exec("rpm -qc $name | grep -v '^(contains no files)\$'");
    return shUser.out.collect({ "$it" });
  }

  List<String> getDocs() {
    shUser.exec("rpm -qd $name | grep -v '^(contains no files)\$'");
    return shUser.out.collect({ "$it" });
  }

  Map<String, String> getDeps() {
    Map<String, String> res = [:];
    shUser.exec("rpm -qR $name").getOut().each {
      def matcher = (it =~ /(\S+)( [><=]+ \S+)*/);
      if (!(it =~ /\(.*\)/).find() && matcher.size() == 1) {
        res[matcher[0][1]] = (matcher[0][2] ?: "").replaceAll(/( |\(|\))/, '');
      }
    }
    return res;
  }
}
