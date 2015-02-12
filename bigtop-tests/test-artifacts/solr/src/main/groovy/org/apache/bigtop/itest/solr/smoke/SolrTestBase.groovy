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
package org.apache.bigtop.itest.solr.smoke;

import groovy.json.JsonSlurper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class SolrTestBase {
  static private Log LOG = LogFactory.getLog(SolrTestBase.class);
  static public final String _updatePathJSON = "/update/json?stream.body="
  static public final String _adminPath = "/admin"

  static public final String _baseURL = System.getProperty("org.apache.bigtop.itest.solr_url", "http://localhost:8983/");

  @BeforeClass
  static void before() {
    // report bad citizens who left docs in the index. Shouldn't be possible, right?
    // May be too aggressive a test.
    // It's under discussion whether this should be on a class or test basis since multiple tests in the same
    // may be order dependent.

    // The pattern should be to index everything in the local BeforeClass perhaps?
    if (doReq("/select?q=*:*").response.numFound != 0) {
      LOG.warn("There's a bad citizen in the tests");
    }

    deleteAllDocs() // guard against bad citizens
  }

  @AfterClass
  static void after() {
    deleteAllDocs() // be a good citizen
  }

  private static void deleteAllDocs() {
    // Insure that the index is empty
    doReq("/update?stream.body=<delete><query>*:*</query></delete>")
    doReq("/update?stream.body=<commit/>")
    // Best check to insure we're empty!
    testEquals(doReq("/select?q=*:*"), "response.numFound", "0")
  }

  static Object doReq(String url) {
    String fullUrl = _baseURL + url + ((url.indexOf("?") >= 0) ? "&" : "?") + "wt=json"
    URLConnection conn = new URL(fullUrl).openConnection()
    BufferedReader res = new BufferedReader(new InputStreamReader(
      conn.getInputStream()))
    String inputLine;
    StringBuilder sb = new StringBuilder()
    while ((inputLine = res.readLine()) != null) {
      sb.append(inputLine)
    }
    res.close();
    return new JsonSlurper().parseText(sb.toString())
  }

  static void testEquals(Object json, String xpath, String value) {
    Object me = json
    xpath.split("\\.").each { part ->
      if (me) {
        me = me[part]
      }
    }
    assert (value.equals(me.toString()))
  }

}
