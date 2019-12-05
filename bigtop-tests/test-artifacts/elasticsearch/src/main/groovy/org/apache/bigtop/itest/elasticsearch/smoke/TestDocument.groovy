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
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Utterly trivial test to see if the server is running
 */
class TestDocument extends ElasticsearchTestBase {
  static jsonSlurper = new JsonSlurper()

  @Test
  void testCRUD() {
     // create a record
     def data = jsonSlurper.parseText('{"title" : "Winter","artist" : "Giuliano Carmignola","album" : "Vivaldi : Le Quattro Stagioni","year" : 1994}')
     def response = elastic.put(
		path: '/music/classic/1',
		contentType: 'application/json',
		body: data,
		headers: [Accept: 'application/json'])
	assertEquals("created", response.data.result)
	println "Sample record is created"

	// now get it back
	elastic.get( path: '/music/classic/1' ) { resp, json ->
	    println "Read it back: "+json
	    assertEquals(200, resp.status)
	    assertEquals("Winter", json._source.title)
	}

	// now update it
	data = jsonSlurper.parseText('{"title" : "Winter","artist" : "Giuliano Carmignola","album" : "Vivaldi : Le Quattro Stagioni","year" : 1994, "Country":"Italy"}')
	response = elastic.post(
		path: '/music/classic/1',
		contentType: 'application/json',
		body: data,
		headers: [Accept: 'application/json'])
	assertEquals("updated", response.data.result)
	println "Sample record is updated"

	// read it back
	elastic.get( path: '/music/classic/1' ) { resp, json ->
	    println "Read it back: "+json
	    assertEquals(200, resp.status)
	    assertEquals("Italy", json._source.Country)
	}

	// delete it
	response = elastic.delete( path: '/music/classic/1' )
	assertEquals("deleted", response.data.result)
	println "Sample record is deleted"
  }
}
