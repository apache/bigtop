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

package org.apache.bigtop.itest.shell

/**
 * This class provides various constants describing an odd collection
 * of facts about and OS that we're running on. It was inspired by
 * Puppet's Facter and perhaps should be renamed sometime in the future ;-)
 */

class OS {
  public static boolean isLinux;
  public static String linux_flavor = "vanilla";
  public static String linux_codename = "plan9";
  public static String linux_release = "1.0";

  static {
    isLinux = (System.getProperty('os.name') =~ /(?i)linux/).matches();

    if (isLinux) {
      linux_flavor = "lsb_release -i -s".execute().text.trim();
      linux_codename = "lsb_release -c -s".execute().text.trim();
      linux_release = "lsb_release -r -s".execute().text.trim();
    }
  }
}
