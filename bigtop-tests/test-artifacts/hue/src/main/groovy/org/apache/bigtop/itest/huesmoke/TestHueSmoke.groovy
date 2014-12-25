/*
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
package org.apache.bigtop.itest.hue.smoke

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell
import org.junit.Test
import static org.apache.bigtop.itest.LogErrorsUtils.logError


public class TestHueSmoke {
  final static String hueServer = System.getProperty("org.apache.bigtop.itest.hue_url", "http://localhost:8888");
  final static String loginURL = "${hueServer}/accounts/login/";
  final static String checkURL = "${hueServer}/debug/check_config";
  final static String hueUsername= System.getProperty("org.apache.bigtop.itest.hue_username");
  final static String huePassword= System.getProperty("org.apache.bigtop.itest.hue_password");
  final static String creds = "username=${hueUsername}&password=${huePassword}";
  final static Map checkApps = [ "about": "<title>Hue        - Quick Start </title>",
                                 "filebrowser/view" : "<title>Hue      - File Browser   </title>",
                                 "help"             : "<title>Hue      - Help       - Hue Help </title>",
                                 // FIXME: HUE-10 "jobbrowser"       : "<title>Er",
                                 "jobsub"           : "<title>Hue      - Job Designer   </title>",
                                 "useradmin"        : "<title>Hue      - User Admin       - Hue Users </title>",
                                 "beeswax"          : "<title>Hue      - Beeswax (Hive UI)       - Query </title>",
                                 "oozie"            : "<title>Hue      - Oozie Editor/Dashboard       - Workflows Dashboard </title>" ];

  Shell sh = new Shell();

  @Test
  void testHueCheckConfig() {
    String sessionId;
    String errormsg;
    String errormesg2;
    String outtest;
    List<String> failedApps = [];

    // first call creates admin/admin username/keypair
    sh.exec("curl -m 60 --data '${creds}' ${loginURL}");

    sh.exec("curl -m 60 -i --data '${creds}' ${loginURL} | sed -e 's#Set-Cookie: *##' -e 's#;.*\$##' | grep '^sessionid'");
    sessionId = sh.getOut().join('');
  System.out.println("${sessionId}"); 
  System.out.println("${checkURL}"); 
    sh.exec("curl -m 60 -b '${sessionId}' ${checkURL}");
    logError(sh);
    errormsg = sh.getOut();
     errormesg2 = sh.getOut().grep( ~/.*All ok. Configuration check passed.*/ ).size();
    System.out.println("${errormsg}");
    System.out.println("${errormesg2}");
    assertTrue("Global configuration check failed",
               sh.getOut().grep( ~/.*All ok. Configuration check passed.*/ ).size() == 0);
    checkApps.each { app, expected ->
      sh.exec("curl -m 60 -b '${sessionId}' ${hueServer}/${app}/");
       //outtest = sh.getOut().join(' ');
       // System.out.println("This is the expected string"+"${outtest}");
        
	System.out.println("Expected value: "+expected);
      if (sh.getOut().join(' ').indexOf(expected) == -1) {
        failedApps.add(app);
      }
    }
    assertEquals("Application(s) ${failedApps} failed to respond",
                 failedApps.size(), 0);
  }
}
