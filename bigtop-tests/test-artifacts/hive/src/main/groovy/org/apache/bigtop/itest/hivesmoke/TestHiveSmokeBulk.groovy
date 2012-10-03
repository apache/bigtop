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
import org.junit.Before
import org.junit.BeforeClass
import org.apache.bigtop.itest.shell.Shell
import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

@RunWith(OrderedParameterized.class)
public class TestHiveSmokeBulk {
  private static String test_include =
    System.getProperty("org.apache.bigtop.itest.hivesmoke.TestHiveSmokeBulk.test_include");
  private static String test_exclude =
    System.getProperty("org.apache.bigtop.itest.hivesmoke.TestHiveSmokeBulk.test_exclude");
  static Shell sh = new Shell("/bin/bash -s");
  static HiveBulkScriptExecutor scripts = new HiveBulkScriptExecutor("scripts/ql");

  private String test;

  public TestHiveSmokeBulk(String t) {
    test = t;
  }

  @Before
  public void cleanUp() {
    def hive_script = "";
    Shell shHive = new Shell("hive");
    ["analyze_srcpart","authorization_part","columntable",
     "dest1","hbase_part","hbase_pushdown","merge_dynamic_part",
     "mp","myinput1","nzhang_part14","src_multi1","src_multi2",
     "srcbucket_mapjoin","srcpart_merge_dp","stats_src","t1",
     "triples","u_data","union_out", "T1", "T2", "T3", "smb_input1",
     "smb_input2", "srcbucket_mapjoin_part", "bucketmapjoin_hash_result_1",
     "bucketmapjoin_hash_result_2", "bucketmapjoin_tmp_result",
     "srcbucket_mapjoin_part_2"].each { 
     hive_script <<= "drop table ${it};\n"; 
    }
    shHive.exec("${hive_script} quit; \n"); 
  }

  @BeforeClass
  public static void setUp() {
    sh.exec("hive -f ./seed.hql");
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
    scripts.runScript(test);
  }
}
