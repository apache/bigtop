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
package org.apache.bigtop.itest.hadoop.hcfs

import org.apache.hadoop.conf.Configuration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import org.apache.bigtop.itest.shell.Shell
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * This is a "superset" of the hdfs/TestFuseDFS class.
 * In time, we can refactor or deprecate the TestFuseDFS class since
 * there might not be any particular need to test HDFS over FUSE specifically.
 * After all, FUSE is an interface, and it should be tested at that level, with
 * understanding that all distribtued file system implementations should
 * require the  same testing.
 *
 * These tests are complex (use lambdas and complex shell commands)
 * and thus somewhat overcommented for the first iteration,
 * we can clean comments up
 * over time.*/
public class TestFuseDFS {
  private static String username = System.properties["user.name"];
  private static Configuration conf;
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shRoot = new Shell("/bin/bash -s", "root");
  private static String mount_point = System.
    getProperty("fuse.dfs.mountpoint", "/tmp/hcfs-test");
  static private Log LOG = LogFactory.getLog(Shell.class)

  /**
   * If HCFS=GLUSTER,S3,... Then we dont do a mount option.
   * That allows this test to work on any file system, because its not
   * coupled to hadoop-fuse-dfs*/
  private static boolean isHDFS = "HDFS".
    equals(System.getProperty("HCFS_IMPLEMENTATION", "HDFS"));
  private static String userdir = "${mount_point}/user/${username}";
  private static String testdir = "${userdir}/TestFuseDFS-testDir";

  /**
   * Used to have a "test file" but now we leave that to individual tests.
   * Also see the testWrapper function to see how the base testDir is
   * created.*/
  @BeforeClass
  public static void setUp() {
    conf = new Configuration();
    String fs_default_name = conf.get("fs.defaultFS");
    String uri = fs_default_name.substring(1);
    shRoot.exec("umount ${mount_point}");
    shRoot.exec("mkdir -p ${mount_point}");

    if (isHDFS) {
      LOG.info("mounting ${uri} on ${mount_point}");
      String opts = "entry_timeout=0,attribute_timeout=0,rdbuffer=0";
      //maximum consistency options
      shRoot.exec("hadoop-fuse-dfs ${uri} ${mount_point} -o${opts}");
    }
    logError(shRoot);
    assertEquals("Failed: hadoop-fuse-dfs mount", 0, shRoot.getRet());
  }

  /**
   * Simple test wrapper: Send a command, and a closure to validate it.
   * See the main impl. for details. */
  public void testWrapper(String testCommand, Closure validatorFn) {
    testWrapper(null, testCommand, validatorFn);
  }

  /**
   * Test Wrapper takes care of several aspects of testing the FUSE mount.
   * 1) Does basic setup of a test dir from scratch.
   * 2) Runs shell "setupCommand" and asserts that if passes.
   * 3) Runs shell "testCommand".
   * 4) Sends results of (3) to validator, runs the validator.
   * 5) Removes the test dir so next test is pure, and no dependencies are
   * possible.
   *
   * Use this function to make it easy to write declarative FUSE tests
   * which mostly
   * focus on logic of the test.*/
  public void testWrapper(String setupCommand, String testCommand,
                          Closure validatorFn) {
    /**
     * Note that to setup we use FUSE ops, but in future
     * formally, better to use "hadoop fs".  The FUSE operations
     * are faster due to no JVM setup, so for the first iteration
     * we go with them.  But that makes this test somewhat dependant
     * on working FUSE mount to begin with.*/
    sh.exec("mkdir -p ${testdir}");
    assertEquals("Failed: mkdir basic setup !", 0, sh.getRet());

    /**
     * some tests will require a file system command to setup the test,
     * for example making sub directories.*/
    if (setupCommand != null) {
      sh.exec(setupCommand);
      LOG.info(setupCommand + " out : " + sh.getOut());
      logError(sh);
      assertEquals("Failed: SETUP function.", 0, sh.getRet());
    }
    /**
     * The main test is here.  */
    sh.exec(testCommand);
    /**
     * Validator lambda is called here.  Groovy is smart enough
     * it runs against the shell object to confirm that the right
     * out/err/etc..
     * occured.*/
    validatorFn(sh);

    /**
     * Completely clean up the testing sub directory, this gaurantees
     * that each unit test is self contained.*/
    LOG.info("Cleaning after " + testCommand);
    sh.exec("rm -rf ${testdir}");
  }

  @AfterClass
  public static void tearDown() {
    if (isHDFS) {
      shRoot.exec("umount ${mount_point}");
      logError(shRoot);
      assertEquals("Failed: FUSE-DFS mount not cleaned up", 0, shRoot.getRet());
    }
  }

  @Test
  public void testCd() {
    testWrapper(//The test: Change to a directory.
      "cd ${testdir} && pwd ",
      //The lambda: Validates via running pwd.
      {
        LOG.info("After cd, pwd=" + sh.getOut()[0]);
        assertEquals("Failed: testing contains '${testdir}' after change " +
          "dir", true,
          sh.getOut()[0].contains("${testdir}"));
        assertEquals("Failed: exit code is non-zero", 0, sh.getRet());
      }//validator
    );
  }

  @Test
  public void testLs() {
    testWrapper(
      "touch ${testdir}/non-trivial-fn",
      "ls -altrh ${testdir}", //Test command : ls the dir.
      {
        //assert that FUSE mount calculates total line (ls -altrh)
        assertTrue("Failed: Confiring that total is shown in ls",
          sh.getOut()[0].contains("total"));
        //now, we expect the user name to be in the test
        // directory, since
        //user is the one who created the test directory.
        assertTrue("Failed: Confirming that the non-trivial-fn is shown in " +
          "ls " +
          "" +
          "" + sh
          .getOut(),
          sh.getOut().toString().contains("non-trivial-fn"));
        assertEquals("Failed: exit code is non-zero", 0, sh.getRet());
      }//validator
    );
  }

  @Test
  public void testMkDir() {
    testWrapper("mkdir ${testdir}/dir1 && cd ${testdir}/dir1 && pwd",
      {
        LOG.info(sh.getOut());
        //assert that FUSE mount calculates total line (ls -altrh)
        assertTrue("Failed: Confirm that dir1 is the new working dir. ",
          sh.getOut().toString().contains("${testdir}/dir1"));
        assertEquals("Failed: mkdir under ${testdir} non-zero return code",
          0,
          sh.getRet());
      } //validator
    );
  }

  @Test
  public void testTouch() {
    testWrapper("touch ${testdir}/file1 && ls ${testdir}",
      {
        LOG.info(sh.getOut());
        //assert that FUSE mount calculates total line (ls -altrh)
        assertTrue("Failed: Confirm that file1 is created/listed ",
          sh.getOut()[0].contains("file1"));
        assertEquals("Failed: touch ${testdir}/file1 + ls return code " +
          "non-zero", 0,
          sh.getRet());
      }//validator
    );
  }

  /**
   * TODO , make multiple files and cat them all.  Since files will be
   * distributed to nodes, this is a better distributed test of the FUSE cat
   * operation.*/
  @Test
  public void testCat() {
    //copy this file in and cat it.
    File f = new File("/tmp/FUSETEST_bigtop");
    f.write("hi_bigtop\nhi_bigtop\n");

    testWrapper("/bin/cp -rf /tmp/FUSETEST_bigtop ${testdir}/cf2",
      /**
       * Required sleep:  IS HDFS FUSE Strictly consistent?
       * Reveals HDFS-6072.*/
      "sleep 2 && cat ${testdir}/cf2",
      {
        //contents of output stream should be "-hello bigtop-"
        LOG.info("cat output = " + sh.getOut() + " " + sh.getErr() + " " +
          sh.getRet());
        def (out, err, ret) = [sh.getOut(), sh.getErr(), sh.getRet()];
        //assert that FUSE mount calculates total line (ls -altrh)
        assertTrue(
          "Failed: cat didnt contain " + out,
          out.contains("hi_bigtop"));
        assertEquals("Failed: return code non-zero", 0, ret);
      }//validator
    );
  }

  /**
   * Test that cp supports multiple files recursive copy,
   * also test that the copied file contents are identical using diff,
   * which returns non-zero if contents are non-identical.
   * */
  @Test
  public void testCp() {
    /**
     * Setup: we make a target dir to test cp'ing with some files
     * to copy in.
     * We also test that file copy is identical.
     * TODO: Determine if the length of this string effect consistency?
     * Small "contents" string might be another way to expose HDFS-6072.
     * */
    final String contents = "ABCDEFGHIJKLMNOPZUIRPIEOF";
    final String setup = "mkdir ${testdir}/targetdir &&" +
      "echo ${contents} > ${testdir}/cp1 && " +
      "echo ${contents} > ${testdir}/cp2 && " +
      "/bin/cp -rf ${testdir}/cp* ${testdir}/targetdir/";
    testWrapper(
      setup,//Large setup function so we externalize it above.
      {
        def files = ["cp1", "cp2"];

        assertEquals("Failed: ret code non-zero", 0, sh.getRet());
        sh.exec("ls -altrh ${testdir}/targetdir/");
        //assert that copy results in the new files
        //at least in the directory namespace...
        assertEquals("Failed: ls of target dir ret code non-zero", 0,
          sh.getRet());
        files.each() {
          assertTrue("Failed: to find ${it} in target directory",
            sh.getOut().toString().contains(it));
        }
        //Assert that the copy resulted in identical files
        //Note that due to eventual consistency, etc, this is
        //an important test for typical fuse behaviour and workload
        files.each() {
          sh.exec("diff " + "${testdir}/${it} "
            + "${testdir}/targetdir/${it}");
          assertTrue("Failed: Detected a difference between ${it} in " +
            "${testdir} vs " + "the ${testdir}/targetdir diff=" + sh.out.join("\n"),
            sh.getRet().equals(0));
        }
      }//validator
    );
  }

  @Test
  public void testMv() {
    //test that move recursively moves stuff
    testWrapper("mkdir -p ${testdir}/subdir1 && touch " +
      "${testdir}/subdir1/innerfile",
      "mv ${testdir}/subdir1 ${testdir}/subdir2",
      {
        assertEquals("Failed: cp exit code != 0", 0, sh.getRet());
        sh.exec("ls -altrh ${testdir}/subdir2/");
        //assert that the inner file exists
        assertTrue(sh.getOut().toString().contains("innerfile"));
        //assert that original file is gone
        sh.exec("ls -altrh ${testdir}");
        assertTrue(!sh.getOut().toString().contains("subdir1"));
      }//validator
    );
  }

  //TODO Test recursive removals
  @Test
  public void testRm() {
    testWrapper("touch ${testdir}/file-removed",
      "rm ${testdir}/file-removed",
      {
        assertEquals("Failed: rm ret code non-zero", 0, sh.getRet());
        sh.exec("ls ${testdir}");
        assertTrue(!sh.getOut().toString().contains("file-removed"));
      }//validator
    );
  }
}
