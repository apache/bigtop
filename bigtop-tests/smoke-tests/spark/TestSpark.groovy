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

package org.apache.bigtop.itest.spark

import org.junit.BeforeClass
import org.junit.AfterClass

import java.util.jar.JarFile
import java.util.zip.ZipInputStream

import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestSpark {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")
  static final String BIGTOP_HOME = System.getenv("BIGTOP_HOME")
  static final String BIGTOP_K8S_NS = "bigtop";

  @BeforeClass
  static void setUp() {
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("kubectl delete sparkapplication " + "-n " + BIGTOP_K8S_NS + " spark-pi")
    logError(sh)
  }

  @Test
  void testSparkPi() {
    sh.exec("kubectl apply -f " + BIGTOP_HOME + "/spark/examples/spark-pi.yaml");
    logError(sh);
    assertTrue("Failed ...", sh.getRet() == 0);

    // sleep 20s
    sleep(20000);

    sh.exec("kubectl logs " + "-n " + BIGTOP_K8S_NS + " spark-pi-driver");
    logError(sh);
    String out = sh.getOut().toString();
    LOG.info("Output of Spark application driver:\n" + out);
    assertTrue(out.contains("Pi is roughly")); 
  }
}

