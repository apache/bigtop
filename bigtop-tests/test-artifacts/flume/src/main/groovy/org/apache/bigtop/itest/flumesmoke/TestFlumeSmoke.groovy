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

package org.apache.bigtop.itest.flumesmoke

import org.junit.Test
import org.junit.Ignore
import org.apache.bigtop.itest.shell.Shell
import org.junit.AfterClass
import org.junit.BeforeClass
import static junit.framework.Assert.assertEquals
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent

// TODO: we have to stub it for 0.20.2 release, once we move to 0.21+ this can go
// import org.apache.hadoop.hdfs.DFSConfigKeys
class DFSConfigKeys {
  static public final FS_DEFAULT_NAME_KEY = "fs.default.name";
}

class TestFlumeSmoke {
  private static String tmp = "TestFlumeSmoke-${(new Date().getTime())}";
  private static String nn = (new Configuration()).get(DFSConfigKeys.FS_DEFAULT_NAME_KEY);
  private static String hdfs_sink_dir = "${nn}/user/${System.properties['user.name']}/$tmp";

  private static Shell sh = new Shell('/bin/bash -s');

  @BeforeClass
  static void setUp() {
    JarContent.unpackJarContainer(TestFlumeSmoke.class, '.', null);
  }

  @AfterClass
  static void tearDown() {
    sh.exec("hadoop fs -rmr $hdfs_sink_dir");
  }

  private void compressionCommonTest(String id, String decompress, String glob) {
    String node_config = "node:text(\"events.txt\")|collectorSink(\"${hdfs_sink_dir}\",\"data\");";

    sh.exec("export FLUME_CONF_DIR=./${id}",
      "flume node_nowatch -s -1 -n node -c '${node_config}'");
    assertEquals("Flume failed to accept events",
      0, sh.ret);

    sh.exec("hadoop fs -cat ${hdfs_sink_dir}/${glob} | ${decompress} | wc -l");
    assertEquals("Wrong # of lines in output found at ${hdfs_sink_dir}",
      "10000", sh.out[0]);
  }

  @Test(timeout = 300000L)
  public void testBzip2() {
    compressionCommonTest("FlumeSmokeBzip2", "bzip2 -d", "*.bz2");
  }

  @Ignore("BIGTOP-218")
  @Test(timeout = 300000L)
  public void testDeflate() {
    compressionCommonTest("FlumeSmokeDeflate", "perl -MCompress::Zlib -e 'undef \$/; print uncompress(<>)'", "*.deflate");
  }

  @Test(timeout = 300000L)
  public void testGzip() {
    compressionCommonTest("FlumeSmokeGzip", "gzip -d", "*.gz");
  }
}
