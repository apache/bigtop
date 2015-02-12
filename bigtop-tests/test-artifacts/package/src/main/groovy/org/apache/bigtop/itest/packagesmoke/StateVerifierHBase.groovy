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

class StateVerifierHBase extends StateVerifier {
  static Shell shHBase = new Shell('hbase shell');

  public static void createStaticState() {
    shHBase.exec("create 't1', 'f1'",
      "put 't1', 'r1', 'f1:q', 'val'",
      "flush 't1'",
      "quit\n");
  }

  public static boolean verifyStaticState() {
    shHBase.exec("scan 't1'",
      "quit\n");

    return (shHBase.getOut().join(' ') =~ /r1.*column=f1:q.*value=val/).find();
  }

  public void createState() {
    createStaticState()
  }

  public boolean verifyState() {
    return verifyStaticState()
  }
}
