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

package org.apache.bigtop.itest.minio

import org.junit.BeforeClass
import org.junit.AfterClass
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestKafkaSmoke {
  static private Log LOG = LogFactory.getLog(Object.class)
  
  static Shell sh = new Shell("/bin/bash -s");

  static String TEST_BUCKET = "testbucket123";
  static String NS = "bigtop";

  @BeforeClass
  static void minioSetUp() {
  }

  @AfterClass
  public static void deleteMinioTestBucket() {
    sh.exec("kubectl exec -n " + NS + " minio-client mc rb bigtop-minio/" + TEST_BUCKET);
  }

  @Test
  public void testServerInfo() {
    sh.exec("kubectl exec -n " + NS + " minio-client mc admin info server bigtop-minio");
    String out = sh.getOut().toString();
    LOG.info(out);
    assertTrue(
      out.contains("Uptime") ||
      out.contains("Version") ||
      out.contains("Storage")
    );
  }

  @Test
  public void testCreateTopics() {
    sh.exec("kubectl exec -n " + NS + " minio-client mc mb bigtop-minio/" + TEST_BUCKET);
    String out = sh.getOut().toString();
    assertTrue(out.contains("Bucket created successfully"));
    
    sh.exec("kubectl exec -n " + NS + " minio-client mc ls bigtop-minio/");
    String outLs = sh.getOut().toString();
    assertTrue(outLs.contains(TEST_BUCKET));
  }
}
