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

class TestSimple extends SolrTestBase {
  static public final String _updatePathXML = "/update?commit=true&stream.body="

  @Test
  public void testSearch() {
    // Index a couple of documents, move to beforeClass?
    // NOTE: JSON update handler isn't enabled in this distro, should it be?
    //def builder = new groovy.json.JsonBuilder()
    //builder([["id": "doc1", "name": URLEncoder.encode("first test document")],
    //        ["id": "doc2", "name": URLEncoder.encode("second test document")]])
    //doReq(_updatePathJSON + builder.toString())
    StringBuilder sb = new StringBuilder()
    sb.append("<add><doc><field name=\"id\">doc1</field><field name=\"name\">first test document").
      append("</field></doc><doc><field name=\"id\">doc2</field><field name=\"name\">second test document").
      append("</field></doc></add>")
    doReq(_updatePathXML + URLEncoder.encode(sb.toString()))
    testEquals(doReq("/select?q=*:*"), "response.numFound", "2")
    testEquals(doReq("/select?q=name:\"first+test+document\""), "response.numFound", "1")
    testEquals(doReq("/select?q=none"), "response.numFound", "0")
  }
}