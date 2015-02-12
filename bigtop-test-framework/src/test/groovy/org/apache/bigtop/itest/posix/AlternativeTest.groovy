/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.posix

import org.apache.bigtop.itest.shell.OS
import org.junit.Test
import static org.junit.Assert.assertTrue

class AlternativeTest {

  @Test
  void testGetAllAlternatives() {
    // Some code in Alternatives.groovy doesn't work on redhat-based OS, so skip it
    if (OS.linux_flavor ==~ /(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/) {
      return;
    };

    Map groups = Alternative.getAlternatives();
    assertTrue("not a single alternative group found. weird.",
      groups.size() > 0);
    assertTrue("there is no alternative for editor. weird.",
      groups["editor"] != null);
    assertTrue("in the editor alternative there are no actuall alternatives",
      groups["editor"].getAlts().size() > 0);
  }
}
