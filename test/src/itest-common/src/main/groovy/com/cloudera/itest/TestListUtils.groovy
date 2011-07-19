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

public class TestListUtils {

  /**
   * Touching files to force Surefire plugin to pick them up
   * @param pathName
   * @throws IOException
   */
  static final def FS = System.getProperty('file.separator', '/');

  static void touchTestFiles(String dirPrefix, String pathName) throws IOException {
    if (!pathName.endsWith('.class')) {
      return;
    }

    List<String> pathArray = pathName.split(FS).toList();
    def prefix =  "";
    if (dirPrefix != null)
      prefix = dirPrefix;

    String fileName =
      pathArray.remove(pathArray.size() - 1).replaceAll('.class', '.touched')
    String dirName = prefix + FS + pathArray.join(FS)

    File dir = new File(dirName);
    dir.mkdirs();
    File file = new File(dirName, fileName);
    file.createNewFile();

    assert file.exists();
  }
}
