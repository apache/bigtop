/**
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

package org.apache.bigtop.itest.hcatalogsmoke

import java.util.ArrayList
import java.util.List
import java.util.Date

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters
import org.apache.bigtop.itest.Contract
import org.apache.bigtop.itest.ParameterSetter
import org.apache.bigtop.itest.Property
import org.apache.bigtop.itest.shell.Shell

@RunWith(OrderedParameterized.class)
public class TestHcatalogBasic {

  public static Shell sh = new Shell("/bin/bash -sx")

  public TestHcatalogBasic() {
  }


  @BeforeClass
  public static void setUp() {
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("rm -f *.actual")
    sh.exec("hive -e \"DROP TABLE IF EXISTS hcat_basic\"")
    sh.exec("hadoop fs -rmr -skipTrash /user/hive/warehouse")
  }

  /**
   * Validate that the table created via hcat exists from Hive's world view
   */
  @Test
  public void testBasic() {
    sh.exec("""
    hcat -e "CREATE TABLE hcat_basic(key string, value string) \
    PARTITIONED BY (dt STRING) \
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','"
    """)
    assertTrue("Could not create table via hcat, return code: " + sh.ret, sh.ret == 0)

    sh.exec("""
    hive -e "DESCRIBE hcat_basic" > hive_hcat_basic_verify.actual
    diff -u hcat_basic_describe.expected hive_hcat_basic_verify.actual
    """)
    assertEquals("hive couldn't detect the table created via hcat, return code: " + sh.ret,
      0, sh.ret);

    sh.exec("""
    hcat -e "DESCRIBE hcat_basic" > hcat_hcat_basic_verify.actual
    diff -u hcat_basic_describe.expected hcat_hcat_basic_verify.actual
    """)
    assertEquals("hcat couldn't detect the table created via hcat, return code: " + sh.ret,
      0, sh.ret);

    // Add a partition via hive
    sh.exec("hive -e \"ALTER TABLE hcat_basic ADD PARTITION (dt='2013-01-01')\"")
    // Add another partition via hcat
    sh.exec("hcat -e \"ALTER TABLE hcat_basic ADD PARTITION (dt='2013-01-02')\"")

    sh.exec("""
    hive -e "SHOW PARTITIONS hcat_basic" > hive_hcat_basic_partitions.actual
    diff -u hcat_basic_partitions.expected hive_hcat_basic_partitions.actual
    """)
    assertEquals("hive couldn't detect all the partitions of the table, return code: " + sh.ret, 0, sh.ret)

    sh.exec("""
    hcat -e "SHOW PARTITIONS hcat_basic" > hcat_hcat_basic_partitions.actual
    diff -u hcat_basic_partitions.expected hcat_hcat_basic_partitions.actual
    """)
    assertEquals("hcat couldn't detect all the partitions of the table, return code: " + sh.ret, 0, sh.ret)

    // Load data into various partitions of the table
    sh.exec("""
    hive -e "LOAD DATA LOCAL INPATH 'data/data-2013-01-01.txt' OVERWRITE INTO TABLE hcat_basic PARTITION(dt='2013-01-01')"
    hive -e "LOAD DATA LOCAL INPATH 'data/data-2013-01-02.txt' OVERWRITE INTO TABLE hcat_basic PARTITION(dt='2013-01-02')"
    """)
    assertEquals("Error in loading data in table, return code: " + sh.ret, 0, sh.ret)

    // Count the number of records via hive
    sh.exec("""
    hive -e "SELECT COUNT(*) FROM hcat_basic" > hive_hcat_basic_count.actual
    diff -u hcat_basic_count.expected hive_hcat_basic_count.actual
    """)
    assertEquals("hive's count of records doesn't match expected count, return code: " + sh.ret, 0, sh.ret)

    // Test Pig's integration with HCatalog
    sh.exec("""
    pig -useHCatalog -e "\
    REGISTER /usr/lib/hcatalog/share/hcatalog/*.jar; \
    REGISTER /usr/lib/hive/lib/*.jar; \
    DATA= LOAD 'hcat_basic' USING org.apache.hcatalog.pig.HCatLoader(); \
    DATA_GROUPS= GROUP DATA ALL; \
    DATA_COUNT= FOREACH DATA_GROUPS GENERATE COUNT(DATA); \
    DUMP DATA_COUNT;" > pig_hcat_basic_count.actual
    diff hcat_basic_count.expected <(cat pig_hcat_basic_count.actual | sed -e 's/(//g' -e 's/)//g')
    """)
    assertEquals("pig's count of records doesn't match expected count, return code: " + sh.ret, 0, sh.ret)

    sh.exec("""
    hcat -e "DROP TABLE hcat_basic"
    """)
    assertEquals("hcat wasn't able to drop table hcat_basic, return code: " + sh.ret, 0, sh.ret)

  }

  @Parameters
  public static Map<String, Object[]> readTestCases() {
    Map<String, Object[]> result = new HashMap<String, Object[]>()
    result.put(new String(), new Object[0])
    return result;
  }
}
