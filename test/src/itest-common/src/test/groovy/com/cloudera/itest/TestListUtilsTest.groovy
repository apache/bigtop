/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.itest

import org.junit.Test
import static org.junit.Assert.assertTrue

public class TestListUtilsTest {
  @Test
  void testListUtils() {
    def prefix = 'tmp';
    def fileName = 'dir/under/which/file/created';
    File expectedFile = new File("${prefix}/${fileName}.touched");

    TestListUtils.touchTestFiles(prefix, "${fileName}.class");
    assertTrue("${fileName}.touched is missing", expectedFile.exists());
    expectedFile.delete();

    TestListUtils.touchTestFiles(prefix, "${fileName}.xml");
    assertTrue("only .class files are expected to be created",
               expectedFile.getParentFile().listFiles().size() == 0);

    File p = new File(prefix);
    p.deleteDir();
  }
}
