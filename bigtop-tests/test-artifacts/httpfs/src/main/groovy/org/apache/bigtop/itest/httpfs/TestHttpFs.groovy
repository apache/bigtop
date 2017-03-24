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
  private static final String DOASUSER = 'hdfs';
  private static final String HTTPFS_PROXY = System.getenv('HTTPFS_PROXY');
  static {
    assertNotNull("HTTPFS_PROXY has to be set to run this test",
      HTTPFS_PROXY);
  }

  private static final String HTTPFS_PREFIX = "http://$HTTPFS_PROXY/webhdfs/v1";
  private static final String HTTPFS_SUCCESS = "{\"boolean\":true}";
  private static final String HTTP_OK = "HTTP/1.1 200 OK";
  private static final String HTTP_CREATE = "HTTP/1.1 201 Created";
  private static final String HTTP_TMP_REDIR = "HTTP/1.1 307 TEMPORARY_REDIRECT";

  public static final String HTTPFS_SOURCE = "bigtop-tests/test-artifacts/httpfs/src/main/resources/"
  def httpfs_source = System.getenv("BIGTOP_HOME") + "/" + HTTPFS_SOURCE;
  def DATA_DIR = httpfs_source + "/" + "text-files";

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
      if (value.length() && expected.startsWith(value)) {
        exists = true;
      }
    }
    assertTrue(expected + " NOT found!", exists == true);
  }

  public void assertValueContains(List<String> values, String expected) {
    boolean exists = false;
    for (String value : values) {
      if (value.length() && value.contains(expected)) {
        exists = true;
      }
    }
    assertTrue(expected + " NOT found!", exists == true);
  }

  private void createDir(String dirname, String doasUser='') {
    def doasStr = ''
    if(doasUser.length()) {
        doasStr = "doas=$doasUser&"
    }
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$dirname?user.name=$USERNAME&${doasStr}op=MKDIRS'");
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  private void renameDir(String dirname, String doasUser='') {
    createDir(dirname, doasUser);
    def doasStr = ''
    if(doasUser.length()) {
        doasStr = "doas=$doasUser&"
    }
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&${doasStr}op=RENAME&destination=$testHttpFsFolderRenamed'");
    assertTrue("curl command to rename a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  private void deleteDir(String dirname, String doasUser='') {
    createDir(dirname, doasUser);
    def doasStr = ''
    if(doasUser.length()) {
        doasStr = "doas=$doasUser&"
    }
    sh.exec("curl -i -X DELETE '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&${doasStr}op=DELETE'");
    assertTrue("curl command to delete a dir failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTPFS_SUCCESS);
  }

  private void statusDir(String dirname, String doasUser='') {
    createDir(dirname, doasUser);
    def doasStr = ''
    if(doasUser.length()) {
        doasStr = "doas=$doasUser&"
    }
    sh.exec("curl -i '$HTTPFS_PREFIX$testHttpFsFolder?user.name=$USERNAME&${doasStr}op=GETFILESTATUS'");
    assertTrue("curl command to create a dir failed", sh.getRet() == 0);
    assertValueContains(sh.getOut(), "DIRECTORY");
    assertValueExists(sh.getOut(), HTTP_OK);
  }

  private void createFile(String filename, String doasUser='') {
    String filenameContent = 'Hello World!';
    def doasStr = ''
    if(doasUser.length()) {
        doasStr = "doas=$doasUser&"
    }

    createDir(testHttpFsFolder, doasUser);
    sh.exec("curl -i -X PUT '$HTTPFS_PREFIX$testHttpFsFolder/$filename?user.name=$USERNAME&${doasStr}op=CREATE'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    String datanodeLocation = null;
    sh.getOut().each {
      if (it.startsWith("Location:")) {
        datanodeLocation = it.split(' ')[1];
        return true;
      }
    }
    LOG.debug("Datanode location: $datanodeLocation");
    assertValueExists(sh.getOut(), HTTP_TMP_REDIR);
    assertNotNull("Datanode location not in response", datanodeLocation);
    sh.exec("curl -i -T $DATA_DIR/$filename '$datanodeLocation' --header 'Content-Type:application/octet-stream'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTP_CREATE);
    sh.exec("curl -i -L '$HTTPFS_PREFIX$testHttpFsFolder/$filename?user.name=$USERNAME&op=OPEN'");
    assertTrue("curl command to create a file failed", sh.getRet() == 0);
    assertValueExists(sh.getOut(), HTTP_OK);
    assertValueExists(sh.getOut(), filenameContent);
  }

  @Test
  public void testCreateDir() {
    createDir(testHttpFsFolder)
  }

  @Test
  public void testCreateDirAsUser() {
    createDir(testHttpFsFolder, DOASUSER)
  }

  @Test
  public void testRenameDir() {
    renameDir(testHttpFsFolder);
  }

  @Test
  public void testRenameDirAsUser() {
    renameDir(testHttpFsFolder, DOASUSER);
  }

  @Test
  public void testDeleteDir() {
    deleteDir(testHttpFsFolder);
  }

  @Test
  public void testDeleteDirAsUser() {
    deleteDir(testHttpFsFolder, DOASUSER);
  }

  @Test
  public void testStatusDir() {
    statusDir(testHttpFsFolder);
  }

  @Test
  public void testStatusDirAsUser() {
    statusDir(testHttpFsFolder, DOASUSER);
  }

  @Test
  public void testCreateFile() {
    String filename = "helloworld.txt";
    createFile(filename)
  }

  @Test
  public void testCreateFileAsUser() {
    String filename = "helloworld.txt";
    createFile(filename, DOASUSER)
  }
}

