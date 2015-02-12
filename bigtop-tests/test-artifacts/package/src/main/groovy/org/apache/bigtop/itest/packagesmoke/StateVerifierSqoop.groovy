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

class StateVerifierSqoop extends StateVerifier {
  final static String jobSpec = "-- import-all-tables --connect jdbc:mysql://example.com/db";
  final static String remoteOpt = "--meta-connect 'jdbc:hsqldb:hsql://localhost:16000/sqoop'";
  Shell sh = new Shell();

  public void createState() {
    sh.exec("sqoop job              --create localJob ${jobSpec}");
    sh.exec("sqoop job ${remoteOpt} --create storeJob ${jobSpec}");
  }

  public boolean verifyState() {
    boolean localFound = (sh.exec("sqoop job              --show localJob | grep -q '^Job: localJob'").getRet() == 0);
    boolean storeFound = (sh.exec("sqoop job ${remoteOpt} --show storeJob | grep -q '^Job: storeJob'").getRet() == 0);
    return (localFound && storeFound);
  }
}
