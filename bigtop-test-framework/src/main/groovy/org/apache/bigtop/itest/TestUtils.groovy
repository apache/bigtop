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

package org.apache.bigtop.itest

import org.junit.Assume

import static org.junit.Assert.assertTrue

import org.apache.bigtop.itest.shell.Shell

public class TestUtils {
  private static Shell sh = new Shell("/bin/bash -s");

  /** helper method to unpack test input files or input folder into HDFS.
   * if both inputDir and inputFiles not null,
   * create the Test resources folder under /user/<USER_NAME>/ in HDFS
   * and copy individual files to resource folder;
   * if inputDir is not null, but inputFiles is null,
   * move the inputDir folder under /user/<USER_NAME>/ in HDFS.
   * If outputDir is not null,
   * create output folder under /user/<USER_NAME>/ in HDFS.
   * @param ref
   * @param test_input
   * @param inputFiles
   * @param test_output
   */
  public static void unpackTestResources(Class ref, String inputDir, String[] inputFiles, String outputDir) {
    // Unpack resource
    JarContent.unpackJarContainer(ref, '.', null);

    // create input dir in HDFS
    if (inputDir != null) {
      sh.exec("hadoop fs -test -e ${inputDir}");
      if (sh.getRet() == 0) {
        sh.exec("hadoop fs -rmr -skipTrash ${inputDir}");
        assertTrue("Deletion of previous $inputDir from the DFS failed",
          sh.getRet() == 0);
      }
      if (inputFiles != null) {
        sh.exec("hadoop fs -mkdir -p ${inputDir}");
        assertTrue("Could not create input directory to the DFS", sh.getRet() == 0);
        // copy additional files into HDFS input folder
        inputFiles.each {
          sh.exec("hadoop fs -put ${it} ${inputDir}");
          assertTrue("Could not copy input files into input folder in the DFS", sh.getRet() == 0);
        }
      } else {
        // copy the entire resource folder into HDFS
        sh.exec("hadoop fs -put ${inputDir} ${inputDir}");
        assertTrue("Could not copy input directory to the DFS", sh.getRet() == 0);
      }
    }

    // create output dir in HDFS
    if (outputDir != null) {
      sh.exec("hadoop fs -test -e ${outputDir}");
      if (sh.getRet() == 0) {
        sh.exec("hadoop fs -rmr -skipTrash ${outputDir}");
        assertTrue("Deletion of previous examples output from the DFS failed",
          sh.getRet() == 0);
      }
      sh.exec("hadoop fs -mkdir -p ${outputDir}");
      assertTrue("Could not create output directory in DFS", sh.getRet() == 0);
    }
  }

  /**
   * Method makes a quick check to validate if password-less sudo is configured
   * @return <code>false</code> if password-less sudo is disabled;
   * <code>true</code> otherwise
   */
  public static boolean noPassSudo () {
    return new Shell().exec("sudo -n true").ret == 0
  }
}
