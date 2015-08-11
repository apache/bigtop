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

package org.apache.bigtop.itest.phoenix

import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

class TestPhoenixQuerySerer {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")
  static final String PHOENIX_HOME = System.getenv("PHOENIX_HOME")
  static final String PSQL = PHOENIX_HOME + "/bin/psql.py"
  static final String SQLLINE_THIN=PHOENIX_HOME + "/bin/sqlline-thin.py" 
  
  @AfterClass
  public static void tearDown() {
    sh.exec("rm -rf /tmp/bigtop-phoenix-smoke*");
    sh.exec(SQLLINE_THIN + " http://localhost smoke-test-teardown.sql")
  }

  @BeforeClass
  static void setUp() {
    sh.exec(PSQL + " localhost STOCK_SYMBOL.sql STOCK_SYMBOL.csv")
  }

  @Test
  void test() {
    sh.exec(SQLLINE_THIN + " http://localhost smoke-test.sql >> /tmp/bigtop-phoenix-smoke-1")
    sh.exec("cat /tmp/bigtop-phoenix-smoke-1")
    assertTrue("Test failed. " + sh.getOut() + " " + sh.getErr(), sh.getOut().toString().contains("SALESFORCE") )
  }
}
