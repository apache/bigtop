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
package org.apache.bigtop.itest.hadoop.odpi 

import groovy.io.FileType
import org.junit.Assert
import org.apache.bigtop.itest.shell.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import java.util.regex.Matcher
import java.util.regex.Pattern

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
            if (!arguments['donotcheckexistance']) {
              Assert.assertTrue("${testName} fail: ${pathString} does not exist", pathFile.exists() )
              Assert.assertTrue("${testName} fail: ${pathString} is not directory", pathFile.isDirectory() )
            }
        }
        break

      case 'dirstruct':
        def expectedFiles = []
        new File("${testsList}", "${arguments['referenceList']}").eachLine { line ->
           expectedFiles << ~line
        }
        def baseDirEnv = getEnv(arguments['baseDirEnv'], arguments['envcmd'])
        Assert.assertNotNull("${baseDirEnv} has to be set for the test to continue",
          baseDirEnv)
        def root = new File(baseDirEnv)
        def actualFiles = []
        def missingFiles = []
        if ( ! root.exists() ) {
          Assert.assertFail("${testName} fail: ${baseDirEnv} does not exist!");
        }

        root.eachFileRecurse(FileType.ANY) { file ->
          def relPath = new File( root.toURI().relativize( file.toURI() ).toString() ).path
          actualFiles << relPath
        }

        expectedFiles.each { wantFile ->
          def ok = false
          for (def x : actualFiles) {
            if (actualFiles =~ wantFile) {
              ok = true
              break
            }
          }
          if (!ok) {
            missingFiles << wantFile
          }
        }

        Assert.assertTrue("${testName} fail: Directory structure for ${baseDirEnv} does not match reference. Missing files: ${missingFiles} ",
          missingFiles.size() == 0)
        break

      case 'dircontent':
        def expectedFiles = []
        new File("${testsList}", "${arguments['referenceList']}").eachLine { line ->
          expectedFiles << ~line
        }

        def baseDir = getEnv(arguments['baseDirEnv'], arguments['envcmd'])
        def subDir = arguments['subDir']
        if (!subDir && arguments['subDirEnv']) {
          subDir = getEnv(arguments['subDirEnv'], arguments['envcmd'])
        }

        def dir = null
        if (subDir) {
          dir = new File(baseDir, subDir)
        } else {
          dir = new File(baseDir)
        }
        Assert.assertNotNull("Directory has to be set for the test to continue", dir)

        def actualFiles = []
        if (dir.exists()) {
          dir.eachFile FileType.FILES, { file ->
            def relPath = new File( dir.toURI().relativize( file.toURI() ).toString() ).path
            actualFiles << relPath
          }
        }

        def missingList = []
        for (def wantFile : expectedFiles) {
          def ok = false
          for (def haveFile : actualFiles) {
            if (haveFile =~ wantFile) {
              ok = true
              break
            }
          }
          if (! ok) {
            missingList << wantFile
          }
        }

        def extraList = []
        for (def haveFile : actualFiles) {
          def ok = false
          for (def wantFile : expectedFiles) {
            if (haveFile =~ wantFile) {
              ok = true
              break
            }
          }
          if (! ok) {
            extraList << haveFile
          }
        }

        def commonFiles = actualFiles.intersect(expectedFiles)
        Assert.assertTrue("${testName} fail: Directory content for ${dir.path} does not match reference. Missing files: ${missingList}. Extra files: ${extraList}",
           missingList.size() == 0 && extraList.size() == 0)
        break
      case 'hadoop_tools':
        def toolsPathStr = getEnv("HADOOP_TOOLS_PATH", "hadoop envvars")
        Assert.assertNotNull("${testName} fail: HADOOP_TOOLS_PATH environment variable should be set", toolsPathStr)

        def toolsPath = new File(toolsPathStr)
        Assert.assertTrue("${testName} fail: HADOOP_TOOLS_PATH must be an absolute path.", toolsPath.isAbsolute())

        Shell sh = new Shell()
        def classPath = sh.exec("hadoop classpath").getOut().join("\n")
        Assert.assertTrue("${testName} fail: Failed to retrieve hadoop's classpath", sh.getRet()==0)

        Assert.assertFalse("${testName} fail: The enire '${toolsPath}' path should not be included in the hadoop's classpath",
          classPath.split(File.pathSeparator).any {
            new File(it).getCanonicalPath() =~ /^${toolsPath}\/?\*/
          }
        )
        break
      case 'hadoop_users':
        Shell sh = new Shell()
        def confDir = getEnv(arguments['confDir'], arguments['envcmd'])

        def dfsUser = sh.exec("sed -n '/dfs.cluster.administrator/ {n;p}' " + confDir + "/hdfs-site.xml | grep -oPm1 '(?<=<value>)[^<]+'").out
        if (sh.getRet() == 0) {
          System.out.println("Found DFS user in hadoop conf hdfs-site.xml: " + dfsUser)
        }
        def dfsGroup = sh.exec("sed -n '/dfs.permissions.superusergroup/ {n;p}' " + confDir + "/hdfs-site.xml | grep -oPm1 '(?<=<value>)[^<]+'").out
        if (sh.getRet() == 0) {
          System.out.println("Found DFS group in hadoop conf hdfs-site.xml: " + dfsGroup)
        }

        def yarnUser = sh.exec("sed -n '/yarn.admin.acl/ {n;p}' " + confDir + "/yarn-site.xml | grep -oPm1 '(?<=<value>)[^<]+'").out
        if (sh.getRet() == 0) {
          System.out.println("Found Yarn user in hadoop conf yarn-site.xml: " + yarnUser)
        }
        def yarnGroup = sh.exec("sed -n '/yarn.nodemanager.linux-container-executor.group/ {n;p}' " + confDir + "/yarn-site.xml | grep -oPm1 '(?<=<value>)[^<]+'").out
        if (sh.getRet() == 0) {
          System.out.println("Found Yarn group in hadoop conf yarn-site.xml: " + yarnGroup)
        }

        def mapredUser = sh.exec("sed -n '/mapreduce.cluster.administrators/ {n;p}' " + confDir + "/mapred-site.xml | grep -oPm1 '(?<=<value>)[^<]+'").out
        if (sh.getRet() == 0) {
          System.out.println("Found MapReduce user in hadoop conf mapred-site.xml: " + mapredUser)
        }

        break
      case 'api_examination':
        def basedir = getEnv(arguments['baseDirEnv'], arguments['envcmd'])
        def libdir = getEnv(arguments['libDir'], arguments['envcmd'])

        def dir = new File(basedir + "/" + libdir)
        Assert.assertTrue("Expected " + dir.getPath() + " to be a directory", dir.isDirectory())
        def pattern = Pattern.compile(arguments['jar'] + "-[0-9]+.*\\.jar")
        def String[] jars = dir.list(new FilenameFilter() {
          @Override
          boolean accept(File d, String name) {
            Matcher matcher = pattern.matcher(name)
            return (matcher.matches() && !name.contains("test"))
          }
        })
        Assert.assertEquals("Expected only one jar, but got " + jars.join(", "), 1, jars.length)
        def jar = dir.getAbsolutePath() + "/" + jars[0]

        def examinerJar = System.properties['bigtop.test.hive.hcat.job.jar']
        def resourceFile = System.properties['test.resources.dir']+ "/" + arguments['resourceFile']
        Shell sh = new Shell()
        def results = sh.exec("hadoop jar " + examinerJar + " org.apache.bigtop.itest.hadoop.api.ApiExaminer -c " + resourceFile + " -j " + jar).getErr()
        int rc = sh.getRet()
        Assert.assertEquals("Expected command to succeed, but got return code " + rc, 0, rc)
        if (results.size() > 0) {
          System.out.println("Received report for jar " + arguments['jar'] + results.join("\n"))
        }
        break;


      default:
        break
    }
  }
}
