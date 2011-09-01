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

package org.apache.bigtop.itest.hadoopsmoke

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import static org.junit.Assert.assertEquals

// TODO: we have to stub it for 0.20.2 release, once we move to 0.21+ this can go
// import org.apache.hadoop.hdfs.DFSConfigKeys
class DFSConfigKeys {
  static public final FS_DEFAULT_NAME_KEY = "fs.default.name";
}

class TestHadoopSmoke {
  static Shell sh = new Shell("/bin/bash -s")

  static String hadoopHome   = System.getProperty('HADOOP_HOME', '/thisfileneverwillexist')
  static String testDir      = "test.hadoopsmoke." + (new Date().getTime())
  static String nn           = (new Configuration()).get(DFSConfigKeys.FS_DEFAULT_NAME_KEY)

  String cmd = "hadoop  jar ${hadoopHome}/contrib/streaming/hadoop-streaming*.jar" +
                 " -D mapred.map.tasks=1 -D mapred.reduce.tasks=1 -D mapred.job.name=Experiment "
  String cmd2 =" -input  ${testDir}/cachefile/input.txt -mapper map.sh -file map.sh -reducer cat" +
                 " -output ${testDir}/cachefile/out -verbose "
  String arg = "${nn}/user/${System.properties['user.name']}/${testDir}/cachefile/cachedir.jar#testlink "

  @BeforeClass
  static void  setUp() throws IOException {
    JarContent.unpackJarContainer(TestHadoopSmoke.class, '.' , null)

    sh.exec(
    "hadoop fs  -mkdir ${testDir}/cachefile",
    "hadoop dfs -put   cachedir.jar ${testDir}/cachefile",
    "hadoop dfs -put   input.txt ${testDir}/cachefile",
    )
    logError(sh)
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash ${testDir}")
  }

  @Test
  void testCacheArchive() {
    sh.exec("hadoop fs -rmr ${testDir}/cachefile/out",
             cmd + ' -cacheArchive ' + arg + cmd2)
    logError(sh)
    sh.exec("hadoop fs -cat ${testDir}/cachefile/out/part-00000")
    logError(sh)

    assertEquals("cache1\t\ncache2\t", sh.out.join('\n'))
  }

  @Test
  void testArchives() {
    sh.exec("hadoop fs -rmr ${testDir}/cachefile/out",
             cmd + ' -archives ' + arg + cmd2)
    logError(sh)
    sh.exec("hadoop fs -cat ${testDir}/cachefile/out/part-00000")
    logError(sh)

    assertEquals("cache1\t\ncache2\t", sh.out.join('\n'))
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
