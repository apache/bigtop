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

import org.apache.bigtop.itest.shell.Shell
import org.junit.Test

class StateVerifierHue extends StateVerifier {
  final static String hueServer = "http://localhost:8088"
  final static String loginURL = "${hueServer}/accounts/login/";
  final static String checkURL = "${hueServer}/debug/check_config";
  final static String creds = "username=admin&password=admin";
  final static List<String> checkApps = ["about", "beeswax", "filebrowser", "help", "jobbrowser", "jobsub", "useradmin"];

  Shell sh = new Shell();

  public boolean config() {
    Shell shRoot = new Shell("/bin/bash", "root");
    return 0 == shRoot.exec("sed -ie 's#^secret_key=#secret_key=1234567890#' /etc/hue/hue.ini").getRet();
  }

  public void createState() {
    // first call creates admin/admin username/keypair
    sh.exec("curl -m 60 --data '${creds}' ${loginURL}");
  }

  public boolean verifyState() {
    String sessionId;
    boolean res;

    sh.exec("curl -m 60 -i --data '${creds}' ${loginURL} | sed -e 's#Set-Cookie: *##' -e 's#;.*\$##' | grep '^sessionid'");
    sessionId = sh.getOut().join('');

    sh.exec("curl -m 60 -b '${sessionId}' ${checkURL}");
    res = (sh.getOut().grep(~/.*All ok. Configuration check passed.*/).size() != 0)
    checkApps.each {
      sh.exec("curl -m 60 -b '${sessionId}' ${hueServer}/${it}/");
      res = res && (sh.getOut().grep(~/.*Page Not Found.*/).size() == 0);
    }
    return res;
  }
}
