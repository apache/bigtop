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
package org.odpi.specs.runtime

import org.junit.Assert
import org.apache.bigtop.itest.shell.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Check all expected environment
 * Tests are constructed dynamically, using external DSL to define
 * - test name
 * - test type
 * - command to execute the test
 * - expected pattern of the output
 */
@RunWith(Parameterized.class)
public class TestSpecsRuntime {
  private String testName
  private String type
  private String command
  private String pattern

  @Parameters
  public static Collection<Object[]> allTests() {
    List<Object[]> specs = [];

    config.specs.tests.each { test ->
      specs.add([test.value.name, test.value.type, test.value.command, test.value.pattern] as Object[])
    }
    return specs
  }

  public TestSpecsRuntime (String testName, String type, String cmd, String ptrn) {
    this.testName = testName
    this.type = type
    this.command = cmd
    this.pattern = ptrn
  }

  public static final String testsList = System.properties['test.resources.dir'] ?:
      "${System.properties['buildDir']}/resources/test"
  def final static config = new ConfigSlurper().parse(new URL("file:${getTestConfigName()}"))

  private static String getTestConfigName() {
    return "$testsList/testRuntimeSpecConf.groovy";
  }

  @Test
  public void testAll() {
    switch (type) {
      case 'shell':
        Shell sh = new Shell()
        def output = sh.exec(command).getOut().join("\n")
        Assert.assertTrue("${testName} fail: $output doesn't contain expected pattern",
            output ==~ /${pattern}/)
        break
      default:
        break
    }
  }
}
