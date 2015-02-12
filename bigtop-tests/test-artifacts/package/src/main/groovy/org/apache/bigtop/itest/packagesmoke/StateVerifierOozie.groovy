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
import java.security.MessageDigest
import org.junit.Test

class StateVerifierOozie extends StateVerifier {
  static Shell sh = new Shell();
  final static String workflow = """<workflow-app xmlns="uri:oozie:workflow:0.1" name="no-op-wf">
                                       <start to="end"/>
                                       <end name="end"/>
                                     </workflow-app>""";

  public static void createStaticState() {
    verifyStaticState();
  }

  public static boolean verifyStaticState() {
    String jobID;
    sh.exec("hadoop fs -put <(echo '$workflow') /oozie.xml");
    sh.exec("oozie job -oozie http://localhost:11000/oozie -run -Doozie.wf.application.path=hdfs://localhost/oozie.xml");
    jobID = sh.getOut().get(0).replaceAll(/job: /, '');

    sleep(5001);

    return (sh.exec("oozie job -oozie http://localhost:11000/oozie -info $jobID | grep -q 'Status *: SUCCEEDED'")
      .getRet() == 0);
  }

  void createState() {
    createStaticState()
  }
}