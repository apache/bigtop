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

import junit.framework.Assert
import org.junit.Test

class TestBuildUtils {

  def final PREFIX = "BIGTOP_UTILS"
  def input = [
      'BIGTOP_VERSION=0.9.0-3',
      'BIGTOP_UTILS_NAME=bigtop-utils',
      'BIGTOP_UTILS__RELNOTES_NAME=Bigtop-utils',
      'BIGTOP_UTILS_PKG_NAME=bigtop-utils',
      'BIGTOP_UTILS_BASE_VERSION=$(subst -,.,$(BIGTOP_VERSION))',
      'BIGTOP_UTILS_PKG_VERSION=$(BIGTOP_UTILS_BASE_VERSION)',
      'BIGTOP_UTILS_RELEASE_VERSION=1',
      'HADOOP_SITE=$(APACHE_MIRROR)/$(BIGTOP_UTILS_RELEASE_VERSION)/hadoop-2.0.6-alpha-src.tar.gz'
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

    Assert.assertEquals("0.9.0.3", map.get("BIGTOP_UTILS_BASE_VERSION"))
    Assert.assertEquals("0.9.0.3", map.get("BIGTOP_UTILS_PKG_VERSION"))
    Assert.assertEquals("http://apache.osuosl.org/1/hadoop-2.0.6-alpha-src.tar.gz", map.get("HADOOP_SITE"))
  }
}