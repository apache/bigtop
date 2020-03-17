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
package org.apache.bigtop.itest.hadoop.kms;

import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;

public class TestKms {
  private static final String USERNAME = System.getProperty("user.name");
  private static final String ZONEDIR  = "/user/$USERNAME/zone1";
  private static final String KEYNAME  = "key1";
  private static final String LOCALDIR = "temp_kms";
  private static final String TXTFILE  = "test.txt";
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs")


  @BeforeClass
  public static void setUp() {
    sh.exec("mkdir -p $LOCALDIR");
    sh.exec("echo testdata >> $LOCALDIR/$TXTFILE");
    sh.exec("hdfs dfs -mkdir -p $ZONEDIR");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("rm -rf $LOCALDIR");
    shHDFS.exec("hdfs dfs -rm -r -skipTrash $ZONEDIR");
    sh.exec("hadoop key delete $KEYNAME -f");
  }

  @Test
  public void testEncryptionZone() {
    sh.exec("hadoop key create $KEYNAME");
    assertTrue("failed to create key $KEYNAME", sh.getRet() == 0);
    shHDFS.exec("hdfs crypto -createZone -keyName $KEYNAME -path $ZONEDIR");
    assertTrue("failed to create encryption zone", shHDFS.getRet() == 0);
    sh.exec("hdfs dfs -put $LOCALDIR/$TXTFILE $ZONEDIR/");
    assertTrue("failed to put file into encryption zone", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $ZONEDIR/$TXTFILE $LOCALDIR/$TXTFILE.1");
    assertTrue("failed to get file from encryption zone", sh.getRet() == 0);
  }
}
