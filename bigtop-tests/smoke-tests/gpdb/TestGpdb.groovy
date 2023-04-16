/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.BeforeClass
import org.junit.AfterClass

import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertFalse
import org.junit.Test
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestGpdb {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")

  @BeforeClass
  static void setUp() {
    // noop for now.
  }

  @AfterClass
  public static void tearDown() {
    // noop for now
  }

  @Test
  void testGpdb() {
    // Basic test to verify that the server is running, and is in a
    // state that we expect.
    LOG.info('Test GPDB column query');
    sh.exec("runuser -l gpadmin /home/gpadmin/test-master-db.sh");
    logError(sh);
    int age = sh.getOut()[2].trim().toInteger()
    assertFalse(age == 0);
    LOG.info('Test GPDB column query finished');
    LOG.info('age is '+age);
  }
}
