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

import groovy.io.FileType
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
  private Map arguments

  private static ENV = System.getenv()

  @Parameters(name="{0}")
  public static Collection<Object[]> allTests() {
    List<Object[]> specs = [];

    config.specs.tests.each { test ->
      specs.add([test.value.name, test.value.type, test.value.arguments] as Object[])
    }
    return specs
  }

  public TestSpecsRuntime (String testName, String type, Map arguments) {
    this.testName = testName
    this.type = type
    this.arguments = arguments
  }

  public static final String testsList = System.properties['test.resources.dir'] ?:
      "${System.properties['buildDir']}/resources/test"
  def final static config = new ConfigSlurper().parse(new URL("file:${getTestConfigName()}"))

  private static String getTestConfigName() {
    return "$testsList/testRuntimeSpecConf.groovy";
  }

  private Map getEnvMap(String command) {
    def envMap = [:]
    Shell sh = new Shell()
    def envvars = sh.exec(command).getOut()
    if (sh.getRet() == 0) {
      envvars.each {
        def match = it =~ /(?<variable>[^=]+)='(?<value>[^']+)'$/
        if ( match.matches() ) {
          envMap[match.group('variable')] = match.group('value')
        }
      }
    }
    return envMap
  }

  private String getEnv(String name, String cmd) {
    String value = ENV[name]
    if (value == null) {
       value = getEnvMap(cmd)[name]
    }
    return value
  }

  @Test
  public void testAll() {
    switch (type) {
      case 'shell':
        Shell sh = new Shell()
        def output = sh.exec(arguments['command']).getOut().join("\n")
        int actualResult = sh.getRet()
        int expectedResult = arguments['expectedResult'] ? arguments['expectedResult'] : 0 // use 0 as default success code
        Assert.assertTrue("${testName} fail: ${arguments['message']} - '${arguments['command']}' returned ${actualResult} instead of ${expectedResult}",
            actualResult == expectedResult)
        break

      case 'envdir':
        def var = arguments['variable']
        def isPathRelative = arguments['relative']
        def pathString = getEnv(var, arguments['envcmd'])
        Assert.assertTrue("${testName} fail: environment variable ${var} does not exist", pathString != null )

        if ( arguments['pattern'] ) {
            Assert.assertTrue("${testName} fail: $pathString doesn't contain expected pattern",
                pathString ==~ /${arguments['pattern']}/)
        }

        def pathFile = new File(pathString)
        if ( isPathRelative ) {
            Assert.assertFalse("${testName} fail: ${pathString} is not relative", pathFile.isAbsolute() )
        } else {
            Assert.assertTrue("${testName} fail: ${pathString} does not exist", pathFile.exists() )
            Assert.assertTrue("${testName} fail: ${pathString} is not directory", pathFile.isDirectory() )
        }
        break

      case 'dirstruct':
        def expectedFiles = []
        new File("${testsList}", "${arguments['referenceList']}").eachLine { line ->
           expectedFiles << line
        }
        def baseDirEnv = getEnv(arguments['baseDirEnv'], arguments['envcmd'])
        Assert.assertNotNull("${baseDirEnv} has to be set for the test to continue",
          baseDirEnv)
        def root = new File(baseDirEnv)
        def actualFiles = []
        if ( root.exists() ) {
          root.eachFileRecurse(FileType.ANY) { file ->
            def relPath = new File( root.toURI().relativize( file.toURI() ).toString() ).path
            actualFiles << relPath
          }
        }
        def missingFiles = (expectedFiles - actualFiles)
        Assert.assertTrue("${testName} fail: Directory structure for ${baseDirEnv} does not match reference. Missing files: ${missingFiles} ",
          missingFiles.size() == 0)
        break

      case 'hadoop_tools':
        Assert.assertNotNull("${testName} fail: HADOOP_TOOLS environment variable should be set", ENV["HADOOP_TOOLS"])
        Assert.assertTrue("${testName} fail: HADOOP_TOOLS should be set to the HADOOP_TOOLS_PATH environment variable.",
            ENV["HADOOP_TOOLS"]== ENV["HADOOP_TOOLS_PATH"])

        def toolsPath = new File(ENV["HADOOP_TOOLS"])
        Assert.assertTrue("${testName} fail: HADOOP_TOOLS must be an absolute path.", toolsPath.isAbsolute())

        Assert.assertNotNull("HADOOP_COMMON_HOME has to be set for the test to continue", ENV["HADOOP_COMMON_HOME"])
        Shell sh = new Shell()
        def classPath = sh.exec("${ENV["HADOOP_COMMON_HOME"]}/bin/hadoop classpath").getOut().join("\n")
        Assert.assertTrue("${testName} fail: Failed to retrieve hadoop's classpath", sh.getRet()==0)

        Assert.assertFalse("${testName} fail: The enire '${toolsPath}' path should not be included in the hadoop's classpath",
          classPath.split(File.pathSeparator).any {
            new File(it).getCanonicalPath() =~ /^${toolsPath}\/?\*/
          }
        )
        break
      default:
        break
    }
  }
}
