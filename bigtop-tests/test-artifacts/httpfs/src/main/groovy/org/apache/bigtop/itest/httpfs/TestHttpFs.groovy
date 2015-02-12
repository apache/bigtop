/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hdfs

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class TestHttpFs {
  private static Log LOG = LogFactory.getLog(TestHttpFs.class)

  private static final String USERNAME = System.getProperty("user.name");
  private static final String HTTPFS_PROXY = System.getenv('HTTPFS_PROXY');
  static {
    assertNotNull("HTTPFS_PROXY has to be set to run this test",
      HTTPFS_PROXY);
  }

  private static final String HTTPFS_PREFIX = "http://$HTTPFS_PROXY/webhdfs/v1";
  private static final String HTTPFS_SUCCESS = "{\"boolean\":true}";

  private static final String DATA_DIR = System.getProperty("data.dir", "text-files");

  private static String testHttpFsFolder = "/tmp/httpfssmoke-" + (new Date().getTime());
  private static String testHttpFsFolderRenamed = "$testHttpFsFolder-renamed";

  private static Shell sh = new Shell("/bin/bash");
  // it will used to cleanup directories, as they are created with via curl with user.name=$USERNAME
  private static Shell shUSERNAME = new Shell("/bin/bash", USERNAME);

  @BeforeClass
  public static void setUp() {
  }

  @AfterClass
  public static void tearDown() {
    // clean up of existing folders using USERNAME of user who created them via curl
    shUSERNAME.exec("hadoop fs -test -e $testHttpFsFolder");
    if (shUSERNAME.getRet() == 0) {
      shUSERNAME.exec("hadoop fs -rmr -skipTrash $testHttpFsFolder");
      assertTrue("Deletion of previous testHttpFsFolder from HDFS failed",
        shUSERNAME.getRet() == 0);
    }
    shUSERNAME.exec("hadoop fs -test -e $testHttpFsFolderRenamed");
    if (shUSERNAME.getRet() == 0) {
      shUSERNAME.exec("hadoop fs -rmr -skipTrash $testHttpFsFolderRenamed");
      assertTrue("Deletion of previous testHttpFsFolderRenamed from HDFS failed",
        shUSERNAME.getRet() == 0);
    }
  }

  public void assertValueExists(List<String> values, String expected) {
    boolean exists = false;
    for (String value : values) {
      if (expected.startsWith(value)) {
        exists = true;
      }
    }
    assertTrue(expected + " NOT found!", exists == true);
  }

  private void createDir(String dirname) {
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$dirname?user.name=$USERNAME&op=MKDIRS'");
  }

  @Test
  public void testCreateDir() {
    createDir(testHttpFsFolder)
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  @Test
  public void testRenameDir() {
    createDir(testHttpFsFolder);
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&op=RENAME&destination=$testHttpFsFolderRenamed'");
    assertTrue("curl command to rename a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  @Test
  public void testDeleteDir() {
    createDir(testHttpFsFolder);
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    sh.exec("curl -i -X DELETE '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&op=DELETE'");
    assertTrue("curl command to delete a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  @Test
  public void testStatusDir() {
    createDir(testHttpFsFolder);
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    sh.exec("curl -i '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&op=GETFILESTATUS'");
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    assertValueExists(sh.getOut(), "DIRECTORY");
  }

  @Test
  public void testCreateFile() {
    String filename = "helloworld.txt";
    String filenameContent = 'Hello World!';

    createDir(testHttpFsFolder);
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$testHttpFsFolder/$filename?user.name=$USERNAME&op=CREATE'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    String datanodeLocation = null;
    sh.getOut().each {
      if (it.startsWith("Location:")) {
        datanodeLocation = it.split(' ')[1];
        return true;
      }
    }
    LOG.debug("Datanode location: $datanodeLocation");
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    sh.exec("curl -i -T $DATA_DIR/$filename '$datanodeLocation' --header 'Content-Type:application/octet-stream'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    sh.exec("curl -i -L '$HTTPFS_PREFIX$testHttpFsFolder/$filename?user.name=$USERNAME&op=OPEN'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
    assertValueExists(sh.getOut(), filenameContent);
  }
}

