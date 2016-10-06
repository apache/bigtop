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

package org.apache.bigtop.itest.spark

import org.apache.bigtop.itest.shell.Shell
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import org.junit.Test
import org.junit.BeforeClass
import static org.junit.Assert.assertEquals

import static org.apache.bigtop.itest.LogErrorsUtils.logError

import org.apache.spark.api.java.*
import org.apache.spark.api.java.function.Function

public class TestSparkSmoke implements Serializable {

  private static String SPARK_HOME = System.getenv("SPARK_HOME")
  private static String SPARK_MASTER = System.getenv("SPARK_MASTER")
  private static String USER = System.getProperty("user.name")
  private static String pwd = ""
  private static Configuration conf
  static Shell sh = new Shell("/bin/bash -s")
  def result = ["9.1: 3", "9.2: 3", "0.2: 3", "9.0: 3", "0.0: 3", "0.1: 3"]

  @BeforeClass
  static void setUp() {
    sh.exec("pwd")
    pwd = sh.out
    int lastIndex = pwd.length() - 1
    pwd = pwd.substring(1, lastIndex)
  }

  @Test
  void ShellTest() {
    String kmeans = "file://" + pwd + "/kmeans_data.txt"
    sh.exec("cd ${SPARK_HOME} && ./bin/spark-submit --class org.apache.spark.examples.JavaWordCount --master local lib/spark-examples.jar " + kmeans)
    logError(sh)
    assertEquals(result, sh.out)
  }

  @Test
  public void HDFSTest() {
    conf = new Configuration()
    String fs_default_name = conf.get("fs.defaultFS")
    FileSystem fs = FileSystem.get(conf)
    String pathname = "/user/${USER}/kmeans_data.txt"
    fs.copyFromLocalFile(new Path("kmeans_data.txt"), new Path(pathname))
    fs.close()

    String dfsname = fs_default_name + pathname
    sh.exec("cd ${SPARK_HOME} && ./bin/spark-submit --class org.apache.spark.examples.JavaWordCount --master ${SPARK_MASTER} lib/spark-examples.jar " + dfsname)
    logError(sh)
    assertEquals(result, sh.out)
  }

  @Test
  public void JobTest() {
    String logFile = "file://" + pwd + "/README.md";
    String[] jars = [System.getProperty("sparkJar"), org.apache.bigtop.itest.JarContent.getJarURL("groovy.lang.GroovyObject")];

    JavaSparkContext sc = new JavaSparkContext("local", "Simple Job",
      SPARK_HOME, jars);

    JavaRDD<String> logData = sc.textFile(logFile).cache();

    long num_Spark = logData.filter(new Function<String, Boolean>() {
      public Boolean call(String s) { return s.contains("Spark"); }
    }).count();

    long num_e = logData.filter(new Function<String, Boolean>() {
      public Boolean call(String s) { return s.contains("e"); }
    }).count();

    assertEquals("Lines containing 'spark' should be 14", 14, num_Spark);
    assertEquals("Lines containing 'e' should be 43", 43, num_e);
  }

}
