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

import org.apache.hadoop.conf.Configuration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import org.apache.bigtop.itest.shell.Shell
import org.junit.runners.MethodSorters
import org.junit.FixMethodOrder
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.NormalTests;

@Category ( NormalTests.class )
@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class TestFuseDFS {
  private static String username = System.properties["user.name"];
  private static Configuration conf;
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shRoot = new Shell("/bin/bash -s");
 private static String mount_point = "/tmp/hdfs";
//  private static String mount_point = System.getProperty("fuse.dfs.mountpoint", "/tmp/hdfs-test");
  private static String userdir = "${mount_point}/user/${username}";
  private static String testdir = "${userdir}/TestFuseDFS-testDir";
  private static String testfile = "${testdir}/TestFuseDFS-testFile";

  @BeforeClass
  public static void setUp() {
    conf = new Configuration();
//    String fs_default_name = conf.get("fs.defaultFS");
//    String uri = fs_default_name.substring(1);
    shRoot.exec("umount ${mount_point}");
    shRoot.exec("mkdir -p ${mount_point}");
  //  shRoot.exec("hadoop-fuse-dfs ${uri} ${mount_point}");
    logError(shRoot);
    assertEquals("hadoop-fuse-dfs failed", 0, shRoot.getRet());
  }

  @AfterClass
  public static void tearDown() {
/*

    shRoot.exec("umount ${mount_point}");
    logError(shRoot);
    assertEquals("FUSE-DFS mount not cleaned up", 0, shRoot.getRet());
*/
  }
  @Test
  public void t1_testCd() {
    System.out.println("Test cd");
    sh.exec("cd ${mount_point}");
    logError(sh);
    assertEquals("cd failed", 0, sh.getRet());
  }

  @Test
  public void t2_testLs() {
    System.out.println("Test ls");
    sh.exec("ls ${mount_point}");
    logError(sh);
    assertEquals("ls failed", 0, sh.getRet());
  }

  @Test
  public void t3_testMkDir() {
    System.out.println("Test mkdir");
    sh.exec("mkdir -p ${testdir}");
    logError(sh);
    assertEquals("mkdir failed", 0, sh.getRet());
  }

  @Test
  public void t4_testTouch() {
    System.out.println("Test touch");
    sh.exec("touch ${testfile}");
    logError(sh);
    assertEquals("touch failed", 0, sh.getRet());
  }

  @Test
  public void t5_cat() {
    System.out.println("Test cat");
    sh.exec("cat ${testfile}");
    logError(sh);
    assertEquals("cat failed", 0, sh.getRet());
  }

  @Test
  public void t6_testCp() {
    System.out.println("Test cp");
    sh.exec("cp ${testfile} ${testfile}2");
    logError(sh);
    assertEquals("cp failed", 0, sh.getRet());
    sh.exec("cp -r ${testdir} ${testdir}2" );
    logError(sh);
    assertEquals("cp -r failed", 0, sh.getRet());
  }

  @Test
  public void t7_testMv() {
    System.out.println("Test mv");
    sh.exec("mv ${testdir} ${testdir}3");
    logError(sh);
    assertEquals("mv failed", 0, sh.getRet());
  }

  @Test
  public void t8_testRm() {
    System.out.println("Test rm -r");
    sh.exec("rm -r ${testdir}*");
    logError(sh);
    assertEquals("rm -r failed", 0, sh.getRet());
  }

}
