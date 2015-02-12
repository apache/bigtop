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

package org.apache.bigtop.itest.posix

import org.apache.bigtop.itest.shell.Shell

class Alternative {
  String name;
  String best;
  String value;
  String status;

  Map alts;

  Shell shUser = new Shell();
  Shell shRoot = new Shell("/bin/bash -s", "root");

  /*
   * We need to figure out whether update-alternatives supports --query since
   * it is a much better way to parse metadata.
   */
  static private String listCmd;
  static {
    int i = (new Shell()).exec("""update-alternatives --help 2>/dev/null | grep -q -- "--query" """).getRet();
    listCmd = (i == 0) ? "query" : "display";
  }

  private void parse_query(List<String> metadata) {
    Map curAlt = [:];
    metadata.each {
      def m = (it =~ /([^:]+):(.*)/);
      if (m.size()) {
        String val = (m[0][2]).trim();
        switch (m[0][1]) {
          case "Best": best = val;
            break;
          case "Value": value = val;
            break;
          case "Status": status = val;
            break;
          case "Priority": curAlt["priority"] = val;
            break;
          case "Slaves": curAlt["slaves"] = [:];
            break;
          case "Alternative": alts[val] = [:];
            curAlt = alts[val];
            break;
        }
      } else if ((it =~ /^ /).find()) {
        curAlt["slaves"][it.trim().replaceAll(/ .*$/, "")] = it.replaceAll(/ \S+ /, "");
      }
    }
  }

  /*
   * Here's what we have to parse on lesser platforms that don't support --query
   * # /usr/sbin/update-alternatives --display ksh
   *   ksh - status is auto.
   *   link currently points to /bin/ksh93
   *   /bin/ksh93 - priority 50
   *   slave ksh-usrbin: /bin/ksh93
   *   slave ksh-man: /usr/share/man/man1/ksh93.1.gz
   *   Current `best' version is /bin/ksh93.
   * #
   */

  private void parse_display(List<String> metadata) {
    Map curAlt = [:];
    String val;
    metadata.each {
      switch (it) {
        case ~/^Current.*version is.*/:
          best = it.replaceAll(/^Current.*version is\s+/, "").
            replaceAll(/\s*\.$/, "");
          break;
        case ~/.*link currently points to.*/:
          value = it.replaceAll(/^.*link currently points to\s+/, "");
          break;
        case ~/.* status is .*/:
          status = it.replaceAll(/^.* status is\s+/, "").
            replaceAll(/\s*\.$/, "");
          break;
        case ~/^ slave .*/:
          val = it.replaceAll(/^ slave /, "").replaceAll(/:.*$/, "");
          curAlt["slaves"][val] = it.replaceAll(/^.*: /, "").trim();
          break;
        case ~/.*priority.*/:
          val = it.replaceAll(/ - priority .*$/, "");
          alts[val] = [:];
          curAlt = alts[val];
          curAlt["priority"] = it.replaceAll(/^.* - priority /, "").trim();
          curAlt["slaves"] = [:];
          break;
      }
    }
  }

  /**
   * Refreshes the state of in-memeory representation of a Linux alternative (see update-alternatives(8)
   * for details on how they work). The following bits of information for each alternative are
   * cached: name (name of the alternative), best (current selection of a best alternative target),
   * value (where does this alternative currently points), status (automatic or manual).
   *
   * The alternatives themselves (including slave ones) are kept in a map alts, where each key is a
   * target and values are maps holding information for slaves.
   */
  public void refresh() {
    this."parse_$listCmd"(shUser.exec("update-alternatives --$listCmd $name").out);
  }

  public Alternative(String n) {
    name = n;
    alts = [:];
    refresh();
  }

  /**
   * Adding another target to the alternative. An alternative needs NOT exist in order
   * for target to be added.
   *
   * @param link an alternative link that is a well-known file location for this alternative
   * @param path a target path (one of the choices for link to actually point to)
   * @param priority a numeric priority for this alternative
   * @param slaves a list os slave links (possibly empty)
   * @return UNIX error code of an update-alternatives --install command
   */
  public int add(String link, String path, String priority, List<Map> slaves) {
    String script = "update-alternatives --install $link $name $path $priority ";
    slaves.each {
      script <<= "--slave ${it.link} ${it.name} ${it.path} ";
    }
    shRoot.exec(script);
    refresh();
    return shRoot.getRet();
  }

  /**
   * Removing one of the choices (a path name to point to) from this alternative.
   *
   * @param path a target path (one of the choices for alternative's link to actually point to)
   * @return UNIX error code of an update-alternatives --remove command
   */
  public int remove(String path) {
    shRoot.exec("update-alternatives --remove $name $path");
    refresh();
    return shRoot.getRet();
  }

  /**
   * Removing ALL of the choices (path names to point to) from this alternative.
   *
   * @return UNIX error code of an update-alternatives --remove-all command
   */
  public int removeAll() {
    shRoot.exec("update-alternatives --remove-all $name");
    refresh();
    return shRoot.getRet();
  }

  /**
   * Switch the link for this alternative to to one of the path names registered under it.
   *
   * @param path a target path (one of the choices for alternative's link to actually point to)
   * @return UNIX error code of an update-alternatives --set command
   */
  public int set(String path) {
    shRoot.exec("update-alternatives --set $name $path");
    refresh();
    return shRoot.getRet();
  }

  /**
   * A factory method producing all of the alternatives registered on this system.
   *
   * @return All the alternatives registered on this system
   */
  public static Map<String, Alternative> getAlternatives() {
    def alts = [:];
    (new Shell()).exec("update-alternatives --get-selections").out.clone().each {
      def m = (it =~ /(\S+)\s+(\S+)\s+(\S+)/);
      if (m.size()) {
        String name = m[0][1];
        alts[name] = new Alternative(name);
      }
    }
    return alts;
  }
}
