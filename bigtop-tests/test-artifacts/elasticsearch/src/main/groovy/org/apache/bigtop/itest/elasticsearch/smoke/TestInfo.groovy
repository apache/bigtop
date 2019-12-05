/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.elasticsearch

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Utterly trivial test to see if the server is running
 */
class TestInfo extends ElasticsearchTestBase {
  @Test
  void testStackInfo() {
     elastic.get( path: '/' ) { resp, json ->
       println json
	   assertEquals("bigtop-es", json.cluster_name)
       assertEquals("You Know, for Search", json.tagline)
     }
  }

  @Test
  void testClusterInfo() {
     elastic.get( path: '/_cluster/state' ) { resp, json ->
       println json.nodes.size()+" nodes in cluster"
       assertTrue(json.nodes.size()>0)
     }
  }
}
