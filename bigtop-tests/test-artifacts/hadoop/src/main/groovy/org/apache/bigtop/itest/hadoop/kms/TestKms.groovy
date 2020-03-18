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
  private static final String USER_NAME = System.getProperty("user.name");
  private static final String ZONE_DIR  = "/user/$USER_NAME/zone1";
  private static final String KEY_NAME  = "key1";
  private static final String LOCAL_DIR = "temp_kms";
  private static final String TXT_FILE  = "test.txt";
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs")


  @BeforeClass
  public static void setUp() {
    sh.exec("mkdir -p $LOCAL_DIR");
    sh.exec("echo testdata >> $LOCAL_DIR/$TXT_FILE");
    sh.exec("hdfs dfs -mkdir -p $ZONE_DIR");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("rm -rf ./$LOCAL_DIR");
    shHDFS.exec("hdfs dfs -rm -r -skipTrash $ZONE_DIR");
    sh.exec("hadoop key delete $KEY_NAME -f");
  }

  @Test
  public void testEncryptionZone() {
    sh.exec("hadoop key create $KEY_NAME");
    assertTrue("failed to create key $KEY_NAME", sh.getRet() == 0);
    shHDFS.exec("hdfs crypto -createZone -keyName $KEY_NAME -path $ZONE_DIR");
    assertTrue("failed to create encryption zone", shHDFS.getRet() == 0);
    sh.exec("hdfs dfs -put $LOCAL_DIR/$TXT_FILE $ZONE_DIR/");
    assertTrue("failed to put file into encryption zone", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $ZONE_DIR/$TXT_FILE $LOCAL_DIR/$TXT_FILE.1");
    assertTrue("failed to get file from encryption zone", sh.getRet() == 0);
  }
}
