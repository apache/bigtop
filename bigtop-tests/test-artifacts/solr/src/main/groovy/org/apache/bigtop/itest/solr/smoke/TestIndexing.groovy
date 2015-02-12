/*
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
package org.apache.bigtop.itest.solr.smoke

import org.junit.BeforeClass
import org.junit.Test

class TestIndexing extends SolrTestBase {
  @BeforeClass
  static void before() {
    // Index a couple of documents
    def builder = new groovy.json.JsonBuilder()
    builder([["id": "doc1", "name": URLEncoder.encode("first test document")],
      ["id": "doc2", "name": URLEncoder.encode("second test document")]])
    doReq(_updatePathJSON + builder.toString() + "&commit=true")
  }

  @Test
  public void testSearch() {
    testEquals(doReq("/select?q=*:*"), "response.numFound", "2")
    testEquals(doReq("/select?q=name:first"), "response.numFound", "1")
    testEquals(doReq("/select?q=name:document"), "response.numFound", "2")
    testEquals(doReq("/select?q=none"), "response.numFound", "0")
  }
}
