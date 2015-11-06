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
import org.apache.bigtop.itest.JarContent
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.EssentialTests;
import org.apache.bigtop.itest.interfaces.NormalTests;

class TestOozieSmoke {
  private static final int WAIT_TIMEOUT = 90000;
  private static Shell sh = new Shell("/bin/bash -s");
  private static String tmp_dir = "oozie.TestOozieSmoke.${(new Date().getTime())}";
  private static final String USERDIR = System.getProperty("user.dir"); 
  private static String oozie_url;
  private static String resourcemanager;
  private static String namenode;

  @BeforeClass
  static void setUp() {
         // unpack resource
        JarContent.unpackJarContainer(TestOozieSmoke.class, "." , null);
    Configuration conf = new Configuration();
    conf.addResource('yarn-site.xml');

    oozie_url = System.getProperty("org.apache.bigtop.itest.oozie_url", "http://localhost:11000/oozie");
    resourcemanager = conf.get("yarn.resourcemanager.address")
    resourcemanager = System.getProperty("org.apache.bigtop.itest.resourcemanager", resourcemanager);
    namenode = conf.get('fs.defaultFS') ? conf.get('fs.defaultFS') : conf.get('fs.default.name')
    namenode = System.getProperty("org.apache.bigtop.itest.namenode", namenode);
    assertNotNull("resourcemanager hostname isn't set", resourcemanager)
    assertNotNull("namenode hostname isn't set", namenode)

    sh.exec("mkdir /tmp/${tmp_dir}",
            "cd /tmp/${tmp_dir}",
            "tar xzf ${USERDIR}/oozie-examples.level0.tar.gz",
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


    sh.exec("hdfs dfs -ls ${tmp_dir}/examples/apps/*/lib/*");
    sh.exec("oozie job -oozie ${oozie_url} -run -DjobTracker=${resourcemanager} -DnameNode=${namenode} " +
            "-DexamplesRoot=${tmp_dir}/examples -config /tmp/${tmp_dir}/examples/apps/${testname}/job.properties");
            logError(sh);  
    assertEquals("Oozie job submition ${testname} failed",
                 0, sh.ret);

    String jobId = sh.out[0].replaceAll(/job: /,"");
    int timeoutCount = 60;
    while (sh.exec("oozie job -oozie ${oozie_url} -info ${jobId}").out.join(' ') =~ /Status\s*:\s*RUNNING/) {
      timeoutCount--;
      if(timeoutCount <= 0) {
        println("job did not get completed in an hour")
        break;
      }
      sleep(WAIT_TIMEOUT);
    }
    assertTrue("Oozie job ${testname} returned ${sh.out.join(' ')} instead of SUCCEEDED",
               (sh.out.join(' ') =~ /Status\s*:\s*SUCCEEDED/).find());
  }

@Category ( EssentialTests.class )
  @Test(timeout=3000000L)
  public void testNoOp() {
    testOozieExamplesCommon("no-op");
  }

@Category ( EssentialTests.class )
  @Test(timeout=3000000L)
  public void testJavaMain() {
    testOozieExamplesCommon("java-main");
  }

@Category ( EssentialTests.class )
  @Test(timeout=3000000L)
  public void testMapReduce() {
    testOozieExamplesCommon("map-reduce");
  }

@Category ( EssentialTests.class )
  @Test(timeout=3000000L)
  public void testCustomMain() {
    testOozieExamplesCommon("custom-main");
  }

@Category ( NormalTests.class )
  @Test(timeout=3000000L)
  public void testHadoopEl() {
    testOozieExamplesCommon("hadoop-el");
  }

@Category ( EssentialTests.class )
  @Test(timeout=3000000L)
  public void testStreaming() {
    testOozieExamplesCommon("streaming");
  }

@Category ( NormalTests.class )
  @Test(timeout=6000000L)
  public void testPig() {
    testOozieExamplesCommon("pig");
  }

@Category ( NormalTests.class )
  @Test(timeout=3000000L)
  public void testHive() {
    testOozieExamplesCommon("hive");
  }

@Category ( NormalTests.class )
  @Test(timeout=3000000L)
  public void testSubwf() {
    testOozieExamplesCommon("subwf");
  }

@Category ( NormalTests.class )
  @Test(timeout=3000000L)
  public void testSsh() {
    // testOozieExamplesCommon("ssh");
  }

@Category ( NormalTests.class )
  @Test(timeout=3000000L)
  public void testDemo() {
    // testOozieExamplesCommon("demo");
  }
}
