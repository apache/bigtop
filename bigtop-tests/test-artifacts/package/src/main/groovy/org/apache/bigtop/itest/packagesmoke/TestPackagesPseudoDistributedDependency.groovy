/**
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
package org.apache.bigtop.itest.packagesmoke

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runner.RunWith
import org.apache.bigtop.itest.junit.OrderedParameterized.RunStage
import org.junit.Test

@RunWith(OrderedParameterized.class)
class TestPackagesPseudoDistributedDependency extends TestPackagesBasics {
  public TestPackagesPseudoDistributedDependency(String pkgName, Node pkgGolden) {
    super(pkgName, pkgGolden);
  }

  @RunStage(level = -3)
  @Test
  synchronized void testRemoteMetadata() {
  }

  @RunStage(level = -1)
  @Test
  void testPackageUpgrade() {
    if (isUpgrade()) {
      checkThat("upgrade sequence on a package $name failed to be executed",
        BTUpgradeSequence.execute(name, System.getProperty("bigtop.prev.repo.version"), "0.5.0"), equalTo(0));
    }
  }

  @Test
  void testPulledDeps() {
    checkPulledDeps(getMap(golden.deps));
  }

  @Test
  void testPackageContent() {
  }

  @Test
  void testPackageServices() {
  }
}
