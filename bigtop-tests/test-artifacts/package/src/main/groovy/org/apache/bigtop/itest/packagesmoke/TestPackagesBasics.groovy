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

import org.junit.BeforeClass
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.Rule
import static org.junit.Assert.assertTrue
import static org.hamcrest.CoreMatchers.equalTo
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters
import org.junit.AfterClass

import org.apache.bigtop.itest.pmanager.PackageInstance
import org.apache.bigtop.itest.junit.OrderedParameterized
import org.apache.bigtop.itest.junit.OrderedParameterized.RunStage

import static org.apache.bigtop.itest.shell.OS.linux_flavor
import org.apache.bigtop.itest.shell.Shell

@RunWith(OrderedParameterized.class)
class TestPackagesBasics extends PackageTestCommon {
  private static PackageTestRepoMgr repo;
  // Ideally, we would want to have PackageInstance implementation be efficient in how it manages different
  // objects representing the same package. That would allow us to have PackageInstance per testcase and
  // not worry about managing it ourselves via the following static Map. For now, however, we rely on
  // constructors being synchronized and thus inserting just one copy of PackageInstance for each package we test
  private static Map<String, PackageInstance> pkgs = [:];
  private static String selectedTests = System.getProperty("bigtop.packages.test", ".");
  private static String skippedTests = System.getProperty("bigtop.packages.skip", "\$^");

  public Node golden;

  static {
    repo = new PackageTestRepoMgr();
    TestPackagesBasics.pm = repo.getPm();
  }

  @Rule
  public ErrorCollector errors = new ErrorCollector();

  @BeforeClass
  public static void setUp() {
    tryOrFail({ repo.addRepo() }, 2, "adding repository failed");
    tryOrFail({
      (repo.getPm().refresh() == 0)
    }, 1, "refreshing repository failed");
  }

  @AfterClass
  public static void tearDown() {
    repo.removeRepo();
  }


  private static void mergeTreeIntoForrest(NodeList forrest, Node tree) {
    for (it in forrest) {
      if (it instanceof Node && it.name() == tree.name()) {
        tree.value().groupBy({
          (it instanceof Node) ? it.name() : "-$it"
        }).each { k, v ->
          if (v.size() == 1 && v.get(0) instanceof Node) {
            mergeTreeIntoForrest(it.value(), v.get(0));
          } else if (v.size() != 1) {
            it.value().addAll(v);
          }
        }
        return;
      }
    }
    forrest.add(tree);
  }

  private static Node mergeTrees(Node n1, Node n2) {
    Node merge = new Node(null, "merge");
    merge.append(n1);
    mergeTreeIntoForrest(merge.value(), n2);
    return (merge.children().size() == 1) ? merge.children().get(0) :
      merge;
  }

  @Parameters
  public static Map<String, Object[]> generateTests() {
    String type = TestPackagesBasics.pm.getType();
    String arch = (new Shell()).exec("uname -m").getOut().get(0).replaceAll(/i.86/, "i386").replaceAll(/x86_64/, "amd64");
    String archTranslated = (type == "apt") ? "" : ((arch == "amd64") ? ".x86_64" : ".${arch}");
    def config = mergeTrees(new XmlParser().parse(TestPackagesBasics.class.getClassLoader().
      getResourceAsStream("package_data.xml")),
      new XmlParser().parse(TestPackagesBasics.class.getClassLoader().
        getResourceAsStream("${type}/package_data.xml")));

    Map<String, Object[]> res = [:];

    config.children().each {
      String name = it.name();

      if ((name =~ /\.(amd64|i386)$/).find()) {
        name = (name =~ "${arch}\$").find() ? name.replaceAll("\\.${arch}\$", archTranslated) : null;

        // TODO: this is a total hack
        if ((name =~ /\.i386$/).find() && linux_flavor == "RedHatEnterpriseServer") {
          name = null;
        }
      }

      if (name != null && (name =~ selectedTests).find() && !(name =~ skippedTests).find()) {
        def strm = TestPackagesBasics.class.getClassLoader().getResourceAsStream("${type}/${name}.xml");
        if (strm) {
          Node manifest = mergeTrees(it, new XmlParser().parse(strm));
          res[name] = ([name, manifest] as Object[]);
        } else {
          res[name] = ([name, it] as Object[]);
        }
      }
    };

    return res;
  }

  public TestPackagesBasics(String pkgName, Node pkgGolden) {
    result = errors;
    name = pkgName;
    golden = pkgGolden;
    // hopefully the following line will go away soon, once PackageInstance becomes more sophisticated
    synchronized (pkgs) {
      pkgs[name] = pkgs[name] ?: PackageInstance.getPackageInstance(pm, name);
    }
    pkg = pkgs[name];
  }

  @RunStage(level = -3)
  @Test
  synchronized void testRemoteMetadata() {
    if (!isUpgrade()) {
      if (pkg.isInstalled()) {
        checkThat("package $name is already installed and could not be removed",
          pkg.remove(), equalTo(0));
      }

      checkRemoteMetadata(getMap(golden.metadata), false);
    }
  }

  @RunStage(level = -2)
  @Test
  synchronized void testPackageInstall() {
    // WARNING: sometimes packages do not install because the server is busy
    for (int i = 3; pkg.install() && i > 0; i--) {
      recordFailure("can not install package $name will retry $i times");
    }

    // TODO: we need to come up with a way to abort any further execution to avoid spurious failures

    checkThat("package $name is expected to be installed",
      pm.isInstalled(pkg), equalTo(true));

    pkg.refresh();
  }

  @Test
  void testRepoFile() {
    // TODO: not sure what level of textual comparison of repo files do we actually need to implement
  }

  @Test
  void testPackageMetadata() {
    checkMetadata(getMap(golden.metadata));
  }

  @Test
  void testUsers() {
    checkUsers(getMap(golden.users));
  }

  @Test
  void testGroups() {
    checkGroups(getMap(golden.groups));
  }

  @Test
  void testAlternatives() {
    checkAlternatives(getMap(golden.alternatives));
  }

  static void tryOrFail(Closure cl, int retries, String fail) {
    while (!cl.call()) {
      retries--;
      assertTrue(fail, retries > 0);
      sleep(3001);
    }
  }

  Map getMap(NodeList nodeList) {
    switch (nodeList.size()) {
      case 0: return [:];
      case 1: return getMapN(nodeList.get(0));
      default: return null;  // poor man's XML validation
    }
  }

  Map getMapN(Node node) {
    String packagerType = pm.getType();
    Map res = [:];
    node.attributes()
    node.children().each {
      String key = it.name().toString();
      if (key == "tag" && it.attributes()["name"] != null) {
        // <tag name="foo"/> -> <foo/>
        key = it.attributes()["name"];
      }
      def value = null;
      if (it.children().size() == 0) {  // empty tags <foo/>
        Map attr = it.attributes();
        value = (attr.size() > 0) ? attr : key;
      } else if (it.children().size() == 1 && it.children().get(0) instanceof java.lang.String) {
        // text tags <foo>bar</foo>
        value = it.text();
      } else if (["apt", "yum", "zypper"].contains(key)) {
        // poor man's XML filtering
        res.putAll((packagerType == key) ? getMapN(it) : [:]);
      } else {
        value = getMapN(it);
      }

      // this is a little bit more tricky than it has to be :-(
      if (value != null) {
        // turn tags with a property name into a tag of its own <tag name="foo"> -> <tag><foo name="foo"</foo></tag>
        if (value instanceof Map && value.name != null) {
          Map tmpMap = [:];
          tmpMap.put(value.name, value);
          value = tmpMap;
        }
        if (res[key] == null) {
          res[key] = value;
        } else {
          if (res[key] instanceof Map && value instanceof Map) {
            res[key].putAll(value);
          } else {
            if (!(res[key] instanceof List)) {
              res[key] = [res[key]];
            }
            res[key].add(value);
          }
        }
      }
    }

    return res;
  }

  public void checkRemoval() {
    checkThat("package $name failed to be removed",
      pkg.remove(), equalTo(0));
    checkThat("package $name is NOT expected to remain installed after removal",
      pm.isInstalled(pkg), equalTo(false));

    checkPackageFilesGotRemoved(getMap(golden.content));
  }

}
