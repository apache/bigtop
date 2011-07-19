/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.itest

import org.junit.Test

import org.junit.AfterClass
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

public class JUnitUtilsTest {
  @AfterClass
  static void tearDown() {
    def testReports = ['TEST-com.cloudera.itest.DummyTestError.xml', 'TEST-com.cloudera.itest.DummyTestFail.xml',
                       'TEST-com.cloudera.itest.DummyTestPass.xml', '/tmp/TEST-com.cloudera.itest.DummyTestPass.xml'];
    testReports.each {
      (new File(it)).delete()
    }
  }

  @Test
  void testPassingTest() {
    assertTrue('DummyPass test is reported as failing', 
               JUnitUtils.executeTests(DummyTestPass.class));
  }
  
  @Test
  void testFailingTest() {
    println('Disclaimer: This test expected to show a failure in an embeded testcase')
    assertFalse('DummyFail test is reported as passing',
               JUnitUtils.executeTests(DummyTestPass.class, DummyTestFail.class));
  }

  @Test
  void testErrorTest() {
    println('Disclaimer: This test expected to show an error in an embeded testcase')
    assertFalse('DummyFail test is reported as passing',
               JUnitUtils.executeTests(DummyTestError.class));
  }


  @Test
  void testCustomOutput() {
    System.setProperty('com.cloudera.itest.JUnitUtils.results.dir', '/tmp');
    JUnitUtils.executeTests(DummyTestPass.class);

    def f = new File('/tmp/TEST-com.cloudera.itest.DummyTestPass.xml')
    assertTrue('Failed to produce expected XML report', f.exists())
  }
}
