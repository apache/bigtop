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
package org.apache.bigtop.itest.hadoop.hdfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.bigtop.itest.JarContent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestWebHDFS {

  private static Shell sh = new Shell("/bin/bash -s");
  private static final String USERNAME = System.getProperty("user.name");
  private static String WEBHDFS_URL = System.getenv('WEBHDFS_URL');
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String testDir = "/user/$USERNAME/webhdfs_$date";
  /**
   * To run the below tests please make sure the below:
   * 1. please set dfs.webhdfs.enabled to true.Else these tests will fail.
   * 2. Also make sure to run the tests using the user
   *    who has read/write permission to hdfs.
   * 3. Make sure to set WEBHDFS_URL environment varirable to the web hdfs url.
   *    which is designated to handle the httpfs requests. The sample url
   *    should look like: http://https_host:14000/webhdfs/v1
   */
  @BeforeClass
  public static void setUp() {
    // check that webhdfs url is properly set
    WEBHDFS_URL = (WEBHDFS_URL != null) ? WEBHDFS_URL : System.getProperty("WEBHDFS_URL");
    assertNotNull("set WEBHDFS_URL environment variable to correct web hdfs url",
                  WEBHDFS_URL);
    // prepare the test directories
    sh.exec("hdfs dfs -test -d $testDir");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r $testDir");
      assertTrue("Unable to clean directory?", sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -mkdir -p $testDir");
    assertTrue("Unable to create test directory?", sh.getRet() == 0);
  }

  @AfterClass
  public static void tearDown() {
    // deletion of test folder
    sh.exec("hadoop fs -test -e $testDir");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $testDir");
      assertTrue("Deletion of previous webhdfs folder from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("hadoop fs -test -e ${testDir}_test");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash ${testDir}_test");
      assertTrue("Deletion of previous webhdfs_test folder from HDFS failed",
          sh.getRet() == 0);
    }
  }

  /**
   * testMkdir() verifies whether the directory creation is happening properly
   * using option op=MKDIRS
   */
  @Test
  public void testMkdir() {
    println("testMkdir");
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir"+
            "?user.name=$USERNAME&op=MKDIRS\"");
    if (!lookForGivenString(sh.getOut(), "HTTP/1.1 200 OK")) {
      println("Failed to create directory");
      println("cmd: curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir"+
              "?user.name=$USERNAME&op=MKDIRS\"");
      println("output: " + sh.getOut());
      assertTrue("Unable to create directory using webhdfs", false);
    }

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}$testDir"+
            "?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("Unable to list directory using webhdfs",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK"));
  }

  /**
   * testCreateFile() verifies whether file is getting created properly using
   * option op=CREATE
   */
  @Test
  public void testCreateFile() {
    println("testCreateFile");
    sh.exec("echo testCreateFile > test_create.txt");
    // create a file using op=CREATE
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir/"+
            "webtest_create.txt?user.name=$USERNAME&op=CREATE\"");
    assertTrue("could not create file using webhdfs", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    String write_url;
    String OUTMSG = "HTTP/1.1 307 Temporary Redirect";
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.toLowerCase().contains(OUTMSG.toLowerCase())) {
        success_1 =true;
      }
      if (next_val.contains("Location: http://")) {
        write_url = next_val.replaceAll("Location: ","");
      }
    }

    if (!success_1) {
      println("Failed to create url");
      println("cmd: curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir/"+
              "test_create.txt?user.name=$USERNAME&op=CREATE\"");
      println("output: " + sh.getOut());
      assertTrue("webhdfs didn't create url for file creation on hdfs",false);
    }

    // now upload data from test_data/test_create.txt to the created file
    sh.exec("curl -u : --negotiate -i -X PUT -T test_create.txt " +
            "\"${write_url}\"");
    assertTrue("could not write to the file using webhdfs", sh.getRet() == 0);
    assertTrue("webhdfs did not write to the url on hdfs ",
                 lookForGivenString(sh.getOut(), "Created")==true);

    // now fetch the data from the file created above and verify
    sh.exec("hdfs dfs -get $testDir/webtest_create.txt test_create_get.txt");
    assertTrue("could not copy the file from hdfs to local", sh.getRet() == 0);
    sh.exec("diff test_create.txt test_create_get.txt");
    assertTrue("file written to hdfs and file copied from hdfs are different",
               sh.getRet() == 0);
    sh.exec("rm -f test_create.txt test_create_get.txt");
  }

  /**
   * testOpen() verifies whether op=OPEN is working properly
   */
  @Test
  public void testOpen() {
    println("testOpen");
    // create a temp file
    sh.exec("echo \"Hi\" > tempOpen");
    sh.exec("hdfs dfs -put tempOpen $testDir/tempOpen");
    assertTrue("Unable to upload file to hdfs?", sh.getRet() == 0);

    // using op=OPEN read the entire content of the file to test_1_2_read.txt
    sh.exec("curl -u : --negotiate -L \"${WEBHDFS_URL}$testDir/tempOpen?"+
            "user.name=$USERNAME&op=OPEN\" > test_1_2_read.txt");
    assertTrue("could not open a file using webhdfs", sh.getRet() == 0);

    sh.exec("diff tempOpen test_1_2_read.txt");
    assertTrue("file read from hdfs and file written to hdfs are different",
               sh.getRet() == 0);

    sh.exec("rm -f test_1_2_read.txt tempOpen");
    sh.exec("hdfs dfs -rm -r /user/${USERNAME}/tempOpen");
  }

  /**
   * testRename() verifies whether the rename operation is working properly
   * using op=RENAME
   */
  @Test
  public void testRename() {
    println("testRename");
    // create webtest_1.txt file
    createTempFile("${testDir}", "webtest_1.txt");
    // rename webtest_1.txt to webtest_2.txt
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir/"+
            "webtest_1.txt?user.name=$USERNAME&op=RENAME&destination="+
            "$testDir/webtest_2.txt\"");
    assertTrue("could not rename file using webhdfs", sh.getRet() == 0);

    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("webhdfs did not rename the file opening on hdfs",
               lookForGivenString(sh.getOut(),OUTMSG)==true);

    // check that webtest_1.txt is not present
    sh.exec("hdfs dfs -ls $testDir");
    assertTrue("file read from hdfs and file written to hdfs are different",
               sh.getRet() == 0);

    Boolean success_1 =false;
    Boolean success_2 =false;
    String file1 = "$testDir/webtest_2.txt";
    String file2 = "$testDir/webtest_1.txt";
    success_1 = lookForGivenString(sh.getOut(), file1);
    success_2 = lookForGivenString(sh.getOut(), file2);

    if (!(success_1== true && success_2== false)) {
      assertTrue("$testDir/webtest_1.txt did not get renamed on hdfs ", false);
    }

    // rename folder
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}$testDir?"+
            "user.name=$USERNAME&op=RENAME&destination=${testDir}_test\"");
    assertTrue("could not rename folder using webhdfs", sh.getRet() == 0);

    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("webhdfs did not rename the folder on hdfs",
               lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("hdfs dfs -ls $testDir");
    assertTrue("webhdfs folder still present on hdfs", sh.getRet() == 1);

    sh.exec("hdfs dfs -ls ${testDir}_test");
    assertTrue("folder did not get renamed", sh.getRet() == 0);

    OUTMSG = "${testDir}_test";
    assertTrue("webhdfs did not rename the folder on hdfs",
               lookForGivenString(sh.getOut(),OUTMSG)==true);
  }

  /**
   * testGetFileStatus() verifies the functionality of op=GETFILESTATUS
   */
  @Test
  public void testGetFileStatus() {
    println("testtGetFileStatus");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETFILESTATUS\"");
    assertTrue("could not get file status using webhdfs", sh.getRet() == 0);

    assertTrue("getfilestatus is not listing proper values",
              lookForGivenString(sh.getOut(),"\"permission\":\"644\"") &&
              lookForGivenString(sh.getOut(),"\"type\":\"FILE\""));

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test?user.name="+
            "$USERNAME&op=GETFILESTATUS\"");
    assertTrue("could not get file status using webhdfs", sh.getRet() == 0);

    assertTrue("getfilestatus is not liisting proper values",
               lookForGivenString(sh.getOut(),"\"owner\":\""+USERNAME+"\"") &&
               lookForGivenString(sh.getOut(),"\"permission\":\"755\"") &&
               lookForGivenString(sh.getOut(),"\"type\":\"DIRECTORY\""));
  }

  /**
   * testGetContentSummary() verifies the functionality of op=GETCONTENTSUMMARY
   */
  @Test
  public void testGetContentSummary() {
    println("testtGetContentSummary");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETCONTENTSUMMARY\"");
    assertTrue("could not get summary for a file using webhdfs",
               sh.getRet() == 0);

    assertTrue("getcontentsummary is not listing proper values for a file",
                lookForGivenString(sh.getOut(),"\"directoryCount\":0") &&
                lookForGivenString(sh.getOut(),"\"fileCount\":1") &&
                lookForGivenString(sh.getOut(),"\"length\":0"));

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=GETCONTENTSUMMARY\"");
    assertTrue("could not get summary for a folder using webhdfs",
               sh.getRet() == 0);

    assertTrue("getcontentsummary is not listing proper values for a folder",
                lookForGivenString(sh.getOut(),"\"directoryCount\":1") &&
                lookForGivenString(sh.getOut(),"\"fileCount\":1") &&
                lookForGivenString(sh.getOut(),"\"length\":0"));
  }

  /**
   * testGetFileChecksum() verifies the functionality of op=GETFILECHECKSUM
   */
  @Test
  public void testGetFileChecksum() {
    println("testtGetFileChecksum");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -L -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETFILECHECKSUM\"");
    assertTrue("could not get checksum for a file using webhdfs",
               sh.getRet() == 0);

    Boolean success_1 =false;
    success_1 = lookForGivenString(sh.getOut(),"\"FileChecksum\":") &&
                lookForGivenString(sh.getOut(),"\"length\":28");
    assertTrue("getchecksum failed for file", success_1);

    // now delete the created temp file
    sh.exec("hdfs dfs -rm -r ${testDir}_test/webtest_2.txt")
    assertTrue("Failed to clean test file?", sh.getRet() == 0);

    sh.exec("curl -L -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=GETFILECHECKSUM\"");
    assertTrue("could not get checksum for a folder using webhdfs",
               sh.getRet() == 0);

    assertTrue("getchecksum failed for folder?",
                lookForGivenString(sh.getOut(),
                "\"message\":\"Path is not a file") == true);
  }

  /**
   * testSetReplication() verifies the functionality of op=SETREPLICATION
   */
  @Test
  public void testSetReplication() {
    println("testSetReplication");

    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETREPLICATION&"+
            "replication=2\"");
    assertTrue("could not set replication for a file using webhdfs",
               sh.getRet() == 0);

    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not received",
               lookForGivenString(sh.getOut(), OUTMSG) == true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);

    String msg1 = "\"owner\":\""+USERNAME+"\"";
    String msg2 = "\"permission\":\"644\"";
    String msg3 = "\"replication\":2";
    boolean success_1 = lookForGivenString(sh.getOut(), msg1) &&
                        lookForGivenString(sh.getOut(), msg2) &&
                        lookForGivenString(sh.getOut(), msg3);
    assertTrue("replication factor not set properly",success_1);

    // folder
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=SETREPLICATION&replication=2\"");
    assertTrue("could not set replication for a folder using webhdfs",
               sh.getRet() == 0);

    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not received for directory",
               lookForGivenString(sh.getOut(), OUTMSG) == true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}{$testDir}_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);

    success_1 = lookForGivenString(sh.getOut(), msg1) &&
                lookForGivenString(sh.getOut(), msg2) &&
                lookForGivenString(sh.getOut(), msg3);
    assertTrue("replication factor not set proeprly for a directory",success_1);
  }

  /**
   * testSetPermissions() verifies the functionality of op=SETPERMISSION
   */
  @Test
  public void testSetPermissions() {
    println("testSetPermissions");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETPERMISSION&"+
            "permission=600\"");
    assertTrue("could not set permissions for a file using webhdfs",
               sh.getRet() == 0);

    assertTrue("expected HTTP status of 200 not received",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);

    assertTrue("permissions not set properly",
                lookForGivenString(sh.getOut(), "\"owner\":\""+USERNAME+"\"") &&
                lookForGivenString(sh.getOut(), "\"permission\":\"600\""));

    // test for folder
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=SETPERMISSION&replication=2\"");
    assertTrue("could not set permissions for a folder using webhdfs",
               sh.getRet() == 0);

    assertTrue("expected HTTP status of 200 not recieved  for directory",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);

    boolean success_1 = lookForGivenString(sh.getOut(),
                        "\"owner\":\""+USERNAME+"\"") &&
                        lookForGivenString(sh.getOut(),
                        "\"permission\":\"600\"");
    assertTrue("permissions not set properly for a directory", success_1);
  }

  /**
   * testSetTimes() verifies the functionality of op=SETTIMES
   */
  @Test
  public void testSetTimes() {
    println("testSetTimes");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");

    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETTIMES&"+
            "modificationtime=1319575753923&accesstime=1369575753923\"");
    assertTrue("could not set times for a file using webhdfs",
               sh.getRet() == 0);

    Boolean success_1 =false;
    assertTrue("expected HTTP status of 200 not received",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);

    assertTrue("times not set properly",
               lookForGivenString(sh.getOut(), "\"owner\":\""+USERNAME+"\"") &&
               lookForGivenString(sh.getOut(), "\"accessTime\":1369575753923") &&
               lookForGivenString(sh.getOut(), "\"modificationTime\":1319575753923"));

    // folder
    sh.exec("curl -u : --negotiate -i -X PUT \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=SETTIMES&modificationtime=1319575753923&"+
            "accesstime=1369575753923\"");
    assertTrue("could not set times for a folder using webhdfs", sh.getRet() == 0);

    assertTrue("expected HTTP status of 200 not received for directory",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("curl -u : --negotiate -i \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);

    assertTrue("times not set properly for a directory",
              lookForGivenString(sh.getOut(), "\"owner\":\""+USERNAME+"\"") &&
              lookForGivenString(sh.getOut(), "\"accessTime\":1369575753923") &&
              lookForGivenString(sh.getOut(), "\"modificationTime\":1319575753923"));
  }

  /**
   * testDelete() verifies the functionality of op=DELETE
   */
  @Test
  public void testDelete() {
    println("testDelete");
    /*
     * First upload a file to hdfs
     */
    createTempFile("${testDir}_test", "webtest_2.txt");
    sh.exec("curl -u : --negotiate -i -X DELETE \"${WEBHDFS_URL}${testDir}_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=DELETE\"");
    assertTrue("could not delete a file using webhdfs", sh.getRet() == 0);

    Boolean success_1 =false;
    assertTrue("expected HTTP status of 200 not received",
               lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("hdfs dfs -ls ${testDir}_test/webtest_2.txt");
    assertTrue("webtest_2.txt should not be present", sh.getRet() == 1);

    // folder
    sh.exec("curl -u : --negotiate -i -X DELETE \"${WEBHDFS_URL}${testDir}_test?"+
            "user.name=$USERNAME&op=DELETE\"");
    assertTrue("could not delete a directory using webhdfs", sh.getRet() == 0);

    assertTrue("expected HTTP status of 200 not recieved for directory",
                lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK"));

    sh.exec("hdfs dfs -ls ${testDir}_test");
    assertTrue("webhdfs_tests directory still present on hdfs",
               sh.getRet() == 1);
  }

  public void createTempFile(String parentDir, String fileName)
  {
    /*
     * create parent directory first
     */
    sh.exec("hdfs dfs -test -d " + parentDir);
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r " + parentDir);
    }
    sh.exec("hdfs dfs -mkdir -p " + parentDir)
    /*
     * First upload a file to hdfs
     */
    sh.exec("hdfs dfs -test -e $parentDir/$fileName")
    // if the file already present then delete it
    if (sh.getRet() == 0 ) {
      sh.exec("hdfs dfs -rm -r $parentDir/$fileName")
      assertTrue("Failed to clean test file $parentDir/$fileName?",
                 sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -touchz $parentDir/$fileName");
    assertTrue("Failed to create test file $parentDir/$fileName?",
               sh.getRet() == 0);
  }

  /**
   * lookForGivenString check the given string is present in the list data
   */
  private boolean lookForGivenString(List<String> data,
                                     String searchString) {
    boolean result = false;
    for( String output_String : data) {
      if(output_String.toLowerCase().contains(searchString.toLowerCase())) {
        result = true;
        break;
      }
    }
    return result;
  }
}
