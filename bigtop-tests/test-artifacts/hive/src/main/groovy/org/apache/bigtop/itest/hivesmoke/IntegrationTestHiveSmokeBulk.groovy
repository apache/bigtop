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
package org.apache.bigtop.itest.hivesmoke

import org.junit.Test
import org.junit.AfterClass
import org.junit.BeforeClass
import org.apache.bigtop.itest.shell.Shell
import static junit.framework.Assert.assertEquals
import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

@RunWith(OrderedParameterized.class)
public class IntegrationTestHiveSmokeBulk {
  private static String test_include =
    System.getProperty("org.apache.bigtop.itest.hivesmoke.IntegrationTestHiveSmokeBulk.test_include");
  private static String test_exclude =
    System.getProperty("org.apache.bigtop.itest.hivesmoke.IntegrationTestHiveSmokeBulk.test_exclude");
  private static String extra_jars =
    System.getProperty("org.apache.bigtop.itest.hivesmoke.IntegrationTestHiveSmokeBulk.extra_jars","");

  static Shell sh = new Shell("/bin/bash -s");
  static HiveBulkScriptExecutor scripts = new HiveBulkScriptExecutor("scripts/integration");

  private String test;

  public IntegrationTestHiveSmokeBulk(String t) {
    test = t;
  }

  @BeforeClass
  public static void setUp() {
    def hbase_script = "";
    def hive_script = "";
    Shell shHbase = new Shell("hbase shell");

    sh.exec("hive -f ./seed.hql");
    assertEquals("Can not initialize seed databases",
                 0, sh.ret);

    ['PARTITION_STAT_TBL', 'countries', 'hbase_pushdown', 'stats_src', 'hbase_part',
     'hbase_table_0', 'hbase_table_3', 'hbase_table_4', 'hbase_table_6', 'hbase_table_7',
     'hbase_table_8', 'states', 'users', 'empty_hbase_table'].each { 
      hbase_script <<= "disable '${it}'\ndrop '${it}'\n";
      hive_script <<= "drop table ${it};\n";  
    }
    shHbase.exec("${hbase_script}\nquit\n\n");
    sh.exec("hive << __EOT__\n${hive_script}__EOT__");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash /user/hive/warehouse",
            "hadoop fs -rmr -skipTrash /tmp/count");
  }

  @Parameters
  public static Map<String, Object[]> readTestCases() {
    List<String> tests;
    if (test_include != null) {
      tests = scripts.getScripts().intersect(Arrays.asList(test_include.split(",")));
    } else if (test_exclude != null) {
      tests = scripts.getScripts() - Arrays.asList(test_exclude.split(","));
    } else {
      tests = scripts.getScripts();
    }
    Map res = [:];
    tests.each {
      res[it] = ([it] as String[]);
    };
    return res;
  }

  @Test
  public void testHiveBulk() {
    scripts.runScript(test, "${(extra_jars == '') ? '' : '--auxpath '}$extra_jars");
  }
}
