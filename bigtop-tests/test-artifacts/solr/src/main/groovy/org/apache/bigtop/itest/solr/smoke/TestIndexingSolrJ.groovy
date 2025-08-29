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

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrInputDocument
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.IOException
import java.net.MalformedURLException
import java.util.ArrayList
import java.util.List
import java.util.Map
import org.junit.Assert

/**
 * Trivial indexing test using SolrJ
 */
class TestIndexingSolrJ extends SolrTestBase {

  HttpSolrClient _client

  @Before
  public void before2() {
    _client = new HttpSolrClient.Builder(_baseURL).build()
  }

  @After
  public void after2() {
    if (_client != null) {
      _client.close()
      _client = null
    }
  }

  // Just add a couple of documents then search on them.
  @Test
  public void testIndexing() throws IOException, SolrServerException {
    List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>()
    SolrInputDocument doc = new SolrInputDocument()
    doc.addField("id", "one")
    doc.addField("name", "Another document one")
    docs.add(doc)
    doc = new SolrInputDocument()
    doc.addField("id", "two")
    doc.addField("name", "Another document two")
    docs.add(doc)
    _client.add(docs)
    _client.commit()

    doQuery("*:*", "one", "two")
    // Now see if we can search them.

  }

  private void doQuery(String queryString, String... docIds) throws MalformedURLException, SolrServerException {

    SolrQuery query = new SolrQuery()
    query.setQuery(queryString)
    query.setRows(1000)
    QueryResponse qr = _client.query(query, SolrRequest.METHOD.POST)
    Object o = qr.getHeader().get("status")
    Assert.assertEquals(0, qr.getHeader().get("status"))

    SolrDocumentList sdl = qr.getResults()
    Assert.assertEquals(docIds.length, sdl.size())
    for (SolrDocument doc : sdl) {
      Assert.assertTrue(docIds.contains(doc.get("id")))
    }
  }
}
