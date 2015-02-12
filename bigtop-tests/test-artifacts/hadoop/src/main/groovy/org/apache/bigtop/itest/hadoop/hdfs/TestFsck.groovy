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

import org.junit.Test
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import static org.apache.bigtop.itest.LogErrorsUtils.logError

/**
 * Tests the HDFS fsck command.
 */
public class TestFsck {
  static Shell shHDFS = new Shell("/bin/bash", "hdfs")
  String[] fsckCmds = [
    "hdfs fsck /",
    "hdfs fsck -move /",
    "hdfs fsck -delete /",
    "hdfs fsck / -files",
    "hdfs fsck -openforwrite /",
    "hdfs fsck -list-corruptfileblocks /",
    "hdfs fsck -blocks /",
    "hdfs fsck -locations /",
    "hdfs fsck -racks /"
  ]

  @Test
  public void testFsckBasic() {
    for (cmd in fsckCmds) {
      shHDFS.exec(cmd)
      logError(shHDFS)
      assertTrue(shHDFS.getRet() == 0)
    }
  }

}
