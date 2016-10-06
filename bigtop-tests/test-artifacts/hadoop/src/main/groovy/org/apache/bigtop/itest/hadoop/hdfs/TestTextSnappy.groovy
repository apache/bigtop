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
package org.apache.bigtop.itest.hadoop.hdfs

import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestTextSnappy {
  static Shell sh = new Shell("/bin/bash -s")
  static String testDir = "testtextsnappy." + (new Date().getTime())
  static String testCacheDir = System.properties['test.resources.dir'] ?
    "${System.properties['test.resources.dir']}/": ""
  static String snappyFileName = "part-00001.snappy"
  static String snappyFile = "${testCacheDir}${snappyFileName}"

  @BeforeClass
  static void setUp() throws IOException {
    sh.exec(
      "hadoop fs  -mkdir ${testDir}",
      "hadoop fs -put ${snappyFile} ${testDir}/${snappyFileName}",
    )
    logError(sh)
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs -rm -r -skipTrash ${testDir}")
  }

  @Test
  void testTextSnappy() {
    String cmd = "hadoop fs -text ${testDir}/${snappyFileName}"
    System.out.println(cmd)
    sh.exec(cmd)
    String output = sh.getOut().join("\n")
    logError(sh)
    String expected = "1\trafferty\t31\n2\tjones\t33\n3\tsteinberg\t33"
    System.out.println("Expected output:\n${expected}")
    System.out.println("Actual output:\n${output}")
    assertEquals("Incorrect output", expected, output)
  }

}
