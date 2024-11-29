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
package org.apache.bigtop.itest.solr.smoke

import org.junit.Test

/**
 * This is actually a pretty useless test as it stands, included as a template for making more "interesting"
 * evaluations of the status of the Solr server.
 */
class TestStatistics extends SolrTestBase {
  @Test
  public void testCache() {
    Object res = doReq(_adminPath + "/mbeans?stats=true")
    ArrayList<Object> beans = res."solr-mbeans"
    for (int idx = 0; idx < beans.size(); idx++) {
      if (beans[idx] instanceof String && "CACHE".equals(beans[idx])) {
        // Next object is the stats data for caches.
        Object hits = beans[idx + 1].filterCache.stats.hits
        break
      }
    }
  }
}
