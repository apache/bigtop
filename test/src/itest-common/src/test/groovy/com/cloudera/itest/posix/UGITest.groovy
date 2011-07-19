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
import static org.junit.Assert.assertEquals

class UGITest {
  UGI ugi = new UGI();

  @Test
  void testUsers() {
    assertEquals("expect root uid to be 0",
                 "0", ugi.getUsers()["root"]["uid"]);
  }

  @Test
  void testGroups() {
    assertEquals("expect root gid to be 0",
                 "0", ugi.getGroups()["root"]["gid"]);
  }
}
