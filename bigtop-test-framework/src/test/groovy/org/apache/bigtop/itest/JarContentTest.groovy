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

package org.apache.bigtop.itest

import groovy.io.FileType
import java.util.zip.ZipInputStream
import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals

class JarContentTest {
  @Test
  void testJarContent() {
    def env = System.getenv();
    def list = JarContent.listContent(env['JAVA_HOME'] + '/lib/tools.jar');
    assertTrue("Jar content should be greater than 10", list.size() > 10);
  }

  @Test(expected = IOException.class)
  void testJarContentNeg() {
    def env = System.getenv();
    try {
      JarContent.listContent(env['JAVA_HOME'] + '/lib/nofilelikethat.jar').each {
        println it;
      }
      assert ("IOException should have been thrown");
    } catch (IOException e) {
      throw e;
    };
  }

  @Test
  void testUnpackJarContainer() {
    def destination = System.properties['buildDir'] ?: 'target/local.unpack.dir';
    JarContent.unpackJarContainer('java.lang.String', destination, 'visitor');
    // expect to find a number of sun/reflect/generics/visitor
    // under destination folder
    File dir = new File(destination);
    int count = 0
    dir.eachFileRecurse {
      if (it.name.endsWith(".class"))
        count++
    }
    assertTrue('Expect more than one file', count > 1);
    dir.deleteDir();
  }

  @Test
  void testUnzipSingleString() {
    def destination = System.properties['buildDir'] ?: 'target/local.unpack.dir'
    URL url = JarContent.getJarURL(String.class)
    ZipInputStream zis = new ZipInputStream(url.openConnection().getInputStream())
    zis.unzip(destination, 'visitor')
    File dir = new File(destination)
    int count = 0
    boolean result = true
    dir.eachFileRecurse(FileType.FILES) {
      if (it.name.endsWith(".class"))
        count++
      if (!it.path.contains("visitor"))
        result = false
    }
    assertTrue('Expect more than one file', count > 1);
    assertTrue('Expect that all paths contain the specified string', result);
    dir.deleteDir()
  }

  @Test
  void testUnzipMultipleStrings() {
    def destination = System.properties['buildDir'] ?: 'target/local.unpack.dir'
    URL url = JarContent.getJarURL(String.class)
    ZipInputStream zis = new ZipInputStream(url.openConnection().getInputStream())
    zis.unzip(destination, ['visitor', 'tree'])
    File dir = new File(destination)
    int count = 0
    boolean result = true
    dir.eachFileRecurse(FileType.FILES) {
      if (it.name.endsWith(".class"))
        count++
      if (!it.path.contains("visitor") && !it.path.contains("tree"))
        result = false
    }
    assertTrue('Expect more than one file', count > 1);
    assertTrue('Expect that all paths contain at least one of the specified string', result);
    dir.deleteDir()
  }

  @Test
  void testUnzipSingleRegex() {
    def destination = System.properties['buildDir'] ?: 'target/local.unpack.dir'
    URL url = JarContent.getJarURL(String.class)
    ZipInputStream zis = new ZipInputStream(url.openConnection().getInputStream())
    // This will unzip sun/security/x509/GeneralSubtree.class and sun/security/x509/GeneralSubtrees.class
    // but not sun/reflect/generics/tree/*
    zis.unzip(destination, ~/[^\/]tree[^\/]/)
    File dir = new File(destination)
    int count = 0
    boolean posChkResult = true
    boolean negChkResult = true
    dir.eachFileRecurse(FileType.FILES) {
      if (it.name.endsWith(".class"))
        count++
      if (!it.path.contains("tree"))
        posChkResult = false
      if (it.path.contains("/tree/"))
        negChkResult = false
    }
    assertTrue('Expect more than one file', count > 1);
    assertTrue('Expect that all paths contain the string "tree"', posChkResult);
    assertTrue('Expect that all paths do not contain the string "/tree/"', negChkResult);
    dir.deleteDir()
  }

  @Test
  void testUnzipMultipleRegexes() {
    def destination = System.properties['buildDir'] ?: 'target/local.unpack.dir'
    URL url = JarContent.getJarURL(String.class)
    ZipInputStream zis = new ZipInputStream(url.openConnection().getInputStream())
    zis.unzip(destination, [~/[^\/]visitor[^\/]/, ~/[^\/]tree[^\/]/])
    File dir = new File(destination)
    int count = 0
    boolean posChkResult = true
    boolean negChkResult = true
    dir.eachFileRecurse(FileType.FILES) {
      if (it.name.endsWith(".class"))
        count++
      if (!it.path.contains("visitor") && !it.path.contains("tree"))
        posChkResult = false
      if (it.path.contains("/visitor/") || it.path.contains("/tree/"))
        negChkResult = false
    }
    assertTrue('Expect more than one file', count > 1);
    assertTrue('Expect that all paths contain either "visitor" or "tree"', posChkResult);
    assertTrue('Expect that all paths do not contain both "/visitor/" and "/tree/"', negChkResult);
    dir.deleteDir()
  }

  @Test
  void testGetJarName() {
    assertEquals("Should've find tools.jar file",
      'tools.jar',
      JarContent.getJarName(System.getenv()['JAVA_HOME'] + '/lib/', 't.*.jar'));
    assertEquals("Should not have found tools.jar file", null,
      JarContent.getJarName(System.getenv()['JAVA_HOME'] + '/lib/', 'nosuch-file.*.jar'));
  }

  // ClassNotException is expected to be thrown in case of non-existing class
  @Test(expected = ClassNotFoundException.class)
  void testUnpackJarContainerNeg() {
    def destination = 'target/local.unpack.dir';
    JarContent.unpackJarContainer('com.lang.NoString', destination, 'visitor');
  }
  // IOException is expected in case of a class not loaded from a jar
  @Test(expected = IOException.class)
  void testUnpackJarContainerNoJar() {
    def destination = 'target/local.unpack.dir';
    JarContent.unpackJarContainer(JarContentTest, destination, 'visitor');
  }
}
