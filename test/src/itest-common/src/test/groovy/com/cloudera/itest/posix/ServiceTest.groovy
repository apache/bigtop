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
package com.cloudera.itest.posix

import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals

class ServiceTest {
  private final String name = "rc.local";
  Service svc = new Service(name);

  @Test
  void testStatus() {
    println 'Status ' + svc.status()
    assertTrue("Expected a not-null and non-empty string as an ssh service status", svc.status() != null && svc.status() != "")
    assertEquals("wrong service name", name, svc.getName());
  }

  @Test
  void testRunLevels() {
    List<String> l = svc.getRunLevels();
    assertTrue("Expected a non-zero size list of registered run levels for ssh service",
               0 != l.size());
  }
}
