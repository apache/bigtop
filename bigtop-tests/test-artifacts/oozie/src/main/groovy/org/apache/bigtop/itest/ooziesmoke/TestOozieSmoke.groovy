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
package org.apache.bigtop.itest.ooziesmoke

import org.junit.Test

import static junit.framework.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import static junit.framework.Assert.assertEquals
import org.apache.hadoop.conf.Configuration

class TestOozieSmoke {
  private static final int WAIT_TIMEOUT = 60000;
  private static Shell sh = new Shell("/bin/bash -s");
  private static String tmp_dir = "oozie.${(new Date().getTime())}";

  private static String oozie_url;
  private static String resourcemanager;
  private static String namenode;
  private static String oozie_tar_home;

  @BeforeClass
  static void setUp() {
    Configuration conf = new Configuration();
    conf.addResource('yarn-site.xml');

    oozie_url = System.getProperty("org.apache.bigtop.itest.oozie_url", "http://localhost:11000/oozie");
    resourcemanager = conf.get("yarn.resourcemanager.address")
    resourcemanager = System.getProperty("org.apache.bigtop.itest.resourcemanager", resourcemanager);
    namenode = conf.get('fs.defaultFS') ? conf.get('fs.defaultFS') : conf.get('fs.default.name')
    namenode = System.getProperty("org.apache.bigtop.itest.namenode", namenode);
    assertNotNull("resourcemanager hostname isn't set", resourcemanager)
    assertNotNull("namenode hostname isn't set", namenode)

    oozie_tar_home = System.getProperty("org.apache.bigtop.itest.oozie_tar_home",
      (new File("/usr/share/doc/packages/oozie/")).exists() ?
        "/usr/share/doc/packages/oozie/" :
        "/usr/lib/oozie");

    sh.exec("mkdir /tmp/${tmp_dir}",
      "cd /tmp/${tmp_dir}",
      "tar xzf ${oozie_tar_home}/oozie-examples.tar.gz",
      "hadoop fs -mkdir ${tmp_dir}",
      "hadoop fs -put examples ${tmp_dir}");
    assertEquals("Failed to put examples onto HDFS",
      0, sh.ret);
  }

  @AfterClass
  static void tearDown() {
    sh.exec("rm -rf /tmp/${tmp_dir}",
      "hadoop fs -rmr ${tmp_dir}");
  }

  void testOozieExamplesCommon(String testname) {
    sh.exec("oozie job -oozie ${oozie_url} -run -DresourceManager=${resourcemanager} -DnameNode=${namenode} " +
      "-DexamplesRoot=${tmp_dir}/examples -config /tmp/${tmp_dir}/examples/apps/${testname}/job.properties");
    assertEquals("Oozie job submition ${testname} failed",
      0, sh.ret);

    String jobId = sh.out[0].replaceAll(/job: /, "");
    while (sh.exec("oozie job -oozie ${oozie_url} -info ${jobId}").out.join(' ') =~ /Status\s*:\s*RUNNING/) {
      sleep(WAIT_TIMEOUT);
    }
    assertTrue("Oozie job ${testname} returned ${sh.out.join(' ')} instead of SUCCEEDED",
      (sh.out.join(' ') =~ /Status\s*:\s*SUCCEEDED/).find());
  }

  @Test(timeout = 300000L)
  public void testNoOp() {
    testOozieExamplesCommon("no-op");
  }

  @Test(timeout = 300000L)
  public void testJavaMain() {
    testOozieExamplesCommon("java-main");
  }

  @Test(timeout = 300000L)
  public void testMapReduce() {
    testOozieExamplesCommon("map-reduce");
  }
/*
  @Test(timeout = 300000L)
  public void testCustomMain() {
    testOozieExamplesCommon("custom-main");
  }
*/
  @Test(timeout = 300000L)
  public void testHadoopEl() {
    testOozieExamplesCommon("hadoop-el");
  }

  @Test(timeout = 300000L)
  public void testStreaming() {
    testOozieExamplesCommon("streaming");
  }
/*
  @Test(timeout = 300000L)
  public void testPig() {
    testOozieExamplesCommon("pig");
  }
*/
  @Test(timeout = 300000L)
  public void testHive() {
    testOozieExamplesCommon("hive2");
  }

  @Test(timeout = 300000L)
  public void testSubwf() {
    testOozieExamplesCommon("subwf");
  }

  @Test(timeout = 300000L)
  public void testSsh() {
    // testOozieExamplesCommon("ssh");
  }

  @Test(timeout = 300000L)
  public void testDemo() {
    // testOozieExamplesCommon("demo");
  }
}
