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
package org.apache.bigtop.itest.phoenix.smoke

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.bigtop.itest.shell.Shell
import org.junit.Test

public class TestPhoenixSmoke {

  static Shell sh = new Shell('/bin/bash -s');

  static final String JAVA_HOME = System.getenv("JAVA_HOME");
  static final String PHOENIX_HOME = System.getenv("PHOENIX_HOME");
  static final String HBASE_HOME = System.getenv("HBASE_HOME");
  
  static {
    assertNotNull("JAVA_HOME has to be set to run this test", JAVA_HOME);
    assertNotNull("PHOENIX_HOME has to be set to run this test", PHOENIX_HOME);
    assertNotNull("HBASE_HOME has to be set to run this test", HBASE_HOME);
  }
  static String phoenixEnd2EndTestCommand = JAVA_HOME + "/bin/java -cp " +
    HBASE_HOME + "/conf/*" + ":" + PHOENIX_HOME + "/lib/*" + ":" +
    HBASE_HOME + "/lib/*" + " org.apache.phoenix.end2end.End2EndTestDriver";

  @Test
  public void testPhoenixEnd2End() {
    sh.exec(phoenixEnd2EndTestCommand);
    assertTrue("end2end test failed", sh.getRet() == 0);
  }

}
