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

package org.apache.bigtop.itest.ambari

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.BeforeClass
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import groovyx.net.http.RESTClient

import org.junit.runner.RunWith

class TestAmbariSmoke {
  static RESTClient ambari

  static String prop(String key) {
     def value = System.getenv(key)
     assertNotNull(value)
     return value
  }
  
  @BeforeClass
  static void setUp() {
    ambari = new RESTClient("${prop('AMBARI_URL')}/api/v1/")
    ambari.setHeaders(["X-Requested-By": "ambari", "Authorization": "Basic YWRtaW46YWRtaW4="])
    ambari.parser.'text/plain' = ambari.parser.'application/json'
  }

  @Test
  void testStackNameVersion() {
     ambari.get( path: 'stacks/ODPi' ) { resp, json ->
       println json
       assertEquals("ODPi", json.versions.Versions[0].stack_name)
       assertEquals("2.0", json.versions.Versions[0].stack_version)
     } 
  }

  @Test
  void testBlueprints() {
     ambari.get( path: 'blueprints' ) { resp, json ->
       println json 
       assertEquals(0, json.items.size)
     } 
  }

  @Test
  void testHosts() {
     def hosts
     ambari.get( path: 'hosts' ) { resp, json ->
       hosts = json.items.Hosts
     }
     hosts.each {
       ambari.get ( path: "hosts/${it.host_name}") { resp, json ->
           assertEquals("HEALTHY", json.Hosts.host_status)
       }
     }
  }
}
