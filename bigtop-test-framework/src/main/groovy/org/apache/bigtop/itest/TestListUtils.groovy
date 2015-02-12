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
    def prefix = "";
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
