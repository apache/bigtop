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
package org.apache.bigtop.itest.packagesmoke

import org.apache.bigtop.itest.shell.Shell
import java.security.MessageDigest

class StateVerifierHDFS extends StateVerifier {
  static Shell sh = new Shell();

  /*
  MessageDigest MD5 = MessageDigest.getInstance("MD5");
  byte[] digest;
  String hdfsName;

  public void createState() {
    File dataFile =  File.createTempFile("StateVerifierHDFS", ".bin");
    String name = dataFile.getAbsolutePath();
    dataFile.deleteOnExit();

    sh.exec("dd if=/dev/urandom of=$name bs=4096 count=1024",
            "hadoop fs -put $name $name");
    MD5.update(dataFile.readBytes());
    digest = MD5.digest();
    hdfsName = name;
  }

  public boolean verifyState() {
    File dataFile =  File.createTempFile("StateVerifierHDFS", ".bin");
    String name = dataFile.getAbsolutePath();
    dataFile.deleteOnExit();

    dataFile.delete();
    sh.exec("hadoop fs -get $hdfsName $name");
    try {
      MD5.update(dataFile.readBytes());
    } catch (Throwable x) {
      return false;
    }
    return (Arrays.equals(digest, MD5.digest()));
  } */

  public static void createStaticState() {
    sh.exec("hadoop fs -put <(echo StateHDFSVErifier) /StateHDFSVErifier");
  }

  public static boolean verifyStaticState() {
    return sh.exec("hadoop fs -cat /StateHDFSVErifier").getOut().join('') == "StateHDFSVErifier";
  }

  public void createState() {
    createStaticState()
  }

  public boolean verifyState() {
    return verifyStaticState()
  }
}
