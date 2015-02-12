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

package org.apache.bigtop.itest.posix

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
