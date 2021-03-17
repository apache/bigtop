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

package org.apache.bigtop.itest.hadoop.mapreduce

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hdfs.DFSConfigKeys
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestHadoopSmoke {
  static Shell sh = new Shell("/bin/bash -s")

  static String hadoopHome = System.getProperty('HADOOP_HOME', '/usr/lib/hadoop')
  static String streamingHome = System.getenv('STREAMING_HOME')
  static String hadoopMapReduceHome = System.getProperty('HADOOP_MAPRED_HOME', '/usr/lib/hadoop-mapreduce')

  static final String STREAMING_HOME =
    (streamingHome == null) ? hadoopMapReduceHome : streamingHome;
  static String streaming_jar =
    JarContent.getJarName(STREAMING_HOME, 'hadoop.*streaming.*.jar');
  static {
    assertNotNull("Can't find hadoop-streaming.jar", streaming_jar);
  }
  static final String STREAMING_JAR = STREAMING_HOME + "/" + streaming_jar;
  static String testDir = "test.hadoopsmoke." + (new Date().getTime())
  static String nn = (new Configuration()).get(DFSConfigKeys.FS_DEFAULT_NAME_KEY)

  String cmd = "hadoop jar ${STREAMING_JAR}" +
    " -D mapred.map.tasks=1 -D mapred.reduce.tasks=1 -D mapred.job.name=Experiment"
  String cmd2 = " -input ${testDir}/cachefile/input.txt -mapper map.sh -file map.sh -reducer cat" +
    " -output ${testDir}/cachefile/out -verbose"
  String arg = "${nn}/user/${System.properties['user.name']}/${testDir}/cachefile/cachedir.jar#testlink"

  @BeforeClass
  static void setUp() throws IOException {
    String[] inputFiles = ["cachedir.jar", "input.txt"];
    try {
      TestUtils.unpackTestResources(TestHadoopSmoke.class, "${testDir}/cachefile", inputFiles, null);
    } catch(Throwable t) {
      logError("Couldn't unpack resources, you better have resources on your classpath.");
    }
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash ${testDir}")
  }

  @Test (timeout = 0x810000l)
  void testCacheArchive() {
    sh.exec("hadoop fs -rmr ${testDir}/cachefile/out",
      cmd + ' -cacheArchive ' + arg + cmd2)
    logError(sh)
    sh.exec("hadoop fs -cat ${testDir}/cachefile/out/part-00000")
    logError(sh)

    assertEquals("cache1\t\ncache2\t", sh.out.join('\n'))
  }

  @Test (timeout = 0x810000l)
  void testArchives() {
    sh.exec("hadoop fs -rmr ${testDir}/cachefile/out",
      cmd + ' -archives ' + arg + cmd2)
    logError(sh)
    sh.exec("hadoop fs -cat ${testDir}/cachefile/out/part-00000")
    logError(sh)

    assertEquals("cache1\t\ncache2\t", sh.out.join('\n'))
  }

}
