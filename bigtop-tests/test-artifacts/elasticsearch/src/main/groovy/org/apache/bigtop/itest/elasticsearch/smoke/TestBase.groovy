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

package org.apache.bigtop.itest.elasticsearch

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.BeforeClass
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import groovyx.net.http.RESTClient

import org.junit.runner.RunWith

class ElasticsearchTestBase {
  static RESTClient elastic

  static String prop(String key) {
     def value = System.getenv(key)
     assertNotNull(value)
     return value
  }

  @BeforeClass
  static void setUp() {
    elastic = new RESTClient("${prop('ELASTICSEARCH_URL')}:9200/")
    elastic.parser.'text/plain' = elastic.parser.'application/json'
  }
}
