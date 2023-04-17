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
package org.apache.bigtop.itest.ranger

import groovy.json.JsonSlurper
import org.junit.Test
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class TestRangerSmoke {

  static String prop(String key) {
     def value = System.getenv(key)
     assertNotNull(value)
     return value
  }

  def url = "${prop('RANGER_URL')}/service/"
  def header = ['Authorization':
    "Basic ${new String(Base64.getEncoder().encode('admin:Admin01234'.getBytes()))}"]

  @Test
  void testServiceDefs() {
    def json = new URL(url + 'public/v2/api/servicedef').getText(requestProperties: header)
    def serviceDefs = new JsonSlurper().parseText(json)
    assertTrue(0 < serviceDefs.size())

    // Check if some major service definitions were registered through setup
    def serviceNames = serviceDefs.stream().map{ it.name }.collect()
    assertTrue(['hdfs', 'hbase', 'hive'].every { serviceNames.contains(it) })
  }
}
