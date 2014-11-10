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

package org.apache.bigtop.itest.tachyon

import org.junit.Before
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull
import org.junit.Test
import org.apache.bigtop.itest.JarContent

class TestTachyonSmoke {

  def tachyonHome = prop('TACHYON_HOME');
  def tachyonMasterAddress = prop('TACHYON_MASTER_ADDRESS');
  def tachyonTestDir = prop('TACHYON_TEST_DIR', '/bigtop');
  def hadoopHome = prop('HADOOP_HOME');

  def hadoop = "${hadoopHome}/bin/hadoop"
  def tachyon = "${tachyonHome}/bin/tachyon"

   Shell sh = new Shell("/bin/bash -s");

   String prop(String key) {
      def value = System.getenv(key)
      assertNotNull(value)
      return value
   }

   String prop(String key, String defaultValue) {
      def value = System.getenv(key)
      if (value == null) {
        return defaultValue
      }
      return value
   }

  /**
   * Runs the Tachyon runTests command that runs the Tachyon examples against the cluster.
   * This command takes care of cleanup at the start of each test, so this framework
   * does not have to worry about cleanup.
   */
  @Test
  void runTests() {
    sh.exec("$tachyon runTests");
    assertTrue("runTests failed. " + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }

  @Test
  void hadoopCat() {
    sh.exec("""
      set -x
      set -e

      export LIB_JARS=\$(find ${tachyonHome} -name "tachyon-client-*-jar-with-dependencies.jar" | sort | head -n1)
      export HADOOP_CLASSPATH=\${LIB_JARS}

      $tachyon tfs rm $tachyonTestDir/hadoopLs/datafile
      $tachyon tfs copyFromLocal datafile $tachyonTestDir/hadoopLs/datafile
      $hadoop fs -cat $tachyonMasterAddress/$tachyonTestDir/hadoopLs/datafile
    """)
    assertTrue("Unable to list from hadoop. " + sh.getOut().join('\n') + " " + sh.getErr().join('\n'), sh.getRet() == 0);
  }
}
