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

package org.apache.bigtop.itest.hamaexamples

import org.junit.AfterClass
import org.junit.BeforeClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.runner.RunWith

/**
 * Test Hama examples shipped with the distribution.
 */
class TestHamaExamples {

	static private Log LOG = LogFactory.getLog(TestHamaExamples.class);

  	public static final String HADOOP_HOME = System.getenv("HADOOP_HOME");
  	static {
    	assertNotNull("HADOOP_HOME is not set", HADOOP_HOME);
  	}
  	
  	static Shell sh = new Shell("/bin/bash -s");
  
  @Test
  void testPiExample() {
    sh.exec("hama org.apache.hama.examples.ExampleDriver pi");
    logError(sh);

    assertTrue("Example pi failed",  sh.getRet() == 0);
  }
  
  @Test
  void testCMBExample() {
    sh.exec("hama org.apache.hama.examples.ExampleDriver cmb");
    logError(sh);

    assertTrue("Example cmb failed",  sh.getRet() == 0);
  }
  
  private static void logError (final Shell sh) {
    if (sh.getRet()) {
      println ('Failed command: ' + sh.script);
      println ('\terror code: ' + sh.getRet());
      println ('\tstdout: ' + sh.getOut());
      println ('\tstderr: ' + sh.getErr());
    }
  }
}