/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop

import org.junit.Assert
import org.junit.Test

class TestBuildUtils {

  def final PREFIX = "BIGTOP_UTILS"
  def input = [
      'BIGTOP_VERSION=0.9.0-3',
      'BIGTOP_GROOVY=2.4-SNAPSHOT',
      'BIGTOP_UTILS_NAME=bigtop-utils',
      'BIGTOP_UTILS__RELNOTES_NAME=Bigtop-utils',
      'BIGTOP_UTILS_PKG_NAME=bigtop-utils',
      'BIGTOP_UTILS_BASE_VERSION=$(subst -,.,$(BIGTOP_VERSION))',
      'BIGTOP_GROOVY_BASE_VERSION=$(subst -SNAPSHOT,,$(BIGTOP_GROOVY))',
      'BIGTOP_GROOVY_BASE_VERSION=$(BIGTOP_GROOVY_BASE_VERSION)',
      'BIGTOP_UTILS_PKG_VERSION=$(BIGTOP_UTILS_BASE_VERSION)',
      'BIGTOP_UTILS_RELEASE_VERSION=1',
      'HADOOP_SITE=$(APACHE_MIRROR)/$(BIGTOP_UTILS_RELEASE_VERSION)/hadoop-2.0.6-alpha-src.tar.gz',
      'BIGTOP_BUILD_STAMP=1'
  ]
  Map map = [
      APACHE_MIRROR:  "http://apache.osuosl.org",
      APACHE_ARCHIVE: "http://archive.apache.org/dist",
  ]

  @Test
  void testEvaluateBOM () {
    BuildUtils buildUtils = new BuildUtils()
    def envs = []
    input.each { line ->
      envs = line?.split("=")
      map.put(envs[0], buildUtils.evaluateBOM(map, envs[1]))
    }

    Assert.assertEquals("2.4", map.get("BIGTOP_GROOVY_BASE_VERSION"))
    Assert.assertEquals("0.9.0.3", map.get("BIGTOP_UTILS_BASE_VERSION"))
    Assert.assertEquals("0.9.0.3", map.get("BIGTOP_UTILS_PKG_VERSION"))
    Assert.assertEquals("http://apache.osuosl.org/1/hadoop-2.0.6-alpha-src.tar.gz", map.get("HADOOP_SITE"))
  }

  @Test
  void testOverrideBOM () {
    System.setProperty("BIGTOP_UTILS_BASE_VERSION", "10.1.0")
    System.setProperty("BIGTOP_BUILD_STAMP", "12")
    System.setProperty("HADOOP_SITE", "http://www.apache.org")
    BuildUtils buildUtils = new BuildUtils()
    def envs = []
    input.each { line ->
      envs = line?.split("=")
      def value = buildUtils.evaluateBOM(map, envs[1])
      value = System.getProperty(envs[0]) ?: value
      map.put(envs[0], value)
    }

    Assert.assertEquals("10.1.0", map.get("BIGTOP_UTILS_BASE_VERSION"))
    Assert.assertEquals("12", map.get("BIGTOP_BUILD_STAMP"))
    Assert.assertEquals("http://www.apache.org", map.get("HADOOP_SITE"))
    System.clearProperty("HADOOP_SITE")
    System.clearProperty("BIGTOP_BUILD_STAMP")
    System.clearProperty("BIGTOP_UTILS_BASE_VERSION")
  }
}
