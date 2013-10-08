/*
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
package org.apache.bigtop.itest.phoenix.smoke

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell
import org.junit.Test

public class TestPhoenixSmoke {

  static Shell sh = new Shell('/bin/bash -s');

  static final String PHOENIX_HOME = System.getenv("PHOENIX_HOME");
  static {
    assertNotNull("PHOENIX_HOME has to be set to run this test", PHOENIX_HOME);
  }
  static String phoenixClientJar = PHOENIX_HOME + "/" +
    JarContent.getJarName(PHOENIX_HOME, "phoenix-.*client.jar");
  static String phoenixTestsJar = PHOENIX_HOME + "/" +
    JarContent.getJarName(PHOENIX_HOME, "phoenix-.*tests.jar");

  // Run a Phoenix end to end unit test using the hbase exec script.
  // This really simplifies the testing setup we would otherwise need
  // to accomplish with Maven and will be more amenable to change if
  // the Phoenix tests someday can be run as test drivers against a
  // running cluster as well.

  static void runTest(String testName) {
    sh.exec("HBASE_CLASSPATH=" + phoenixClientJar + ":" + phoenixTestsJar + 
      " hbase org.junit.runner.JUnitCore " + testName);
    assertTrue(testName + " failed", sh.getRet() == 0);
  }

  @Test
  public void testAlterTable() {
    runTest("com.salesforce.phoenix.end2end.AlterTableTest")
  }

  @Test
  public void testArithmeticQuery() {
    runTest("com.salesforce.phoenix.end2end.ArithmeticQueryTest")
  }

  @Test
  public void testAutoCommit() {
    runTest("com.salesforce.phoenix.end2end.AutoCommitTest")
  }

  @Test
  public void testBinaryRowKey() {
    runTest("com.salesforce.phoenix.end2end.BinaryRowKeyTest")
  }

  @Test
  public void testCoalesceFunction() {
    runTest("com.salesforce.phoenix.end2end.CoalesceFunctionTest")
  }

  @Test
  public void testCompareDecimalToLong() {
    runTest("com.salesforce.phoenix.end2end.CompareDecimalToLongTest")
  }

  @Test
  public void testCreateTable() {
    runTest("com.salesforce.phoenix.end2end.CreateTableTest")
  }

  @Test
  public void testCSVLoader() {
    runTest("com.salesforce.phoenix.end2end.CSVLoaderTest")
  }

  @Test
  public void testCustomEntityData() {
    runTest("com.salesforce.phoenix.end2end.CustomEntityDataTest")
  }

  @Test
  public void testDefaultParallelIteratorsRegionSplitter() {
    runTest("com.salesforce.phoenix.end2end.DefaultParallelIteratorsRegionSplitterTest")
  }

  @Test
  public void testDeleteRange() {
    runTest("com.salesforce.phoenix.end2end.DeleteRangeTest")
  }

  @Test
  public void testDescColumnSortOrder() {
    runTest("com.salesforce.phoenix.end2end.DescColumnSortOrderTest")
  }

  @Test
  public void testDistinctCount() {
    runTest("com.salesforce.phoenix.end2end.DistinctCountTest")
  }

  @Test
  public void testDynamicColumn() {
    runTest("com.salesforce.phoenix.end2end.DynamicColumnTest")
  }

  @Test
  public void testDynamicFamily() {
    runTest("com.salesforce.phoenix.end2end.DynamicFamilyTest")
  }

  @Test
  public void testDynamicUpsert() {
    runTest("com.salesforce.phoenix.end2end.DynamicUpsertTest")
  }

  @Test
  public void testExecuteStatements() {
    runTest("com.salesforce.phoenix.end2end.ExecuteStatementsTest")
  }

  @Test
  public void testExtendedQueryExec() {
    runTest("com.salesforce.phoenix.end2end.ExtendedQueryExecTest")
  }

  @Test
  public void testFunkyNames() {
    runTest("com.salesforce.phoenix.end2end.FunkyNamesTest")
  }

  @Test
  public void testGroupByCase() {
    runTest("com.salesforce.phoenix.end2end.GroupByCaseTest")
  }

  @Test
  public void testIsNull() {
    runTest("com.salesforce.phoenix.end2end.IsNullTest")
  }

  @Test
  public void testKeyOnly() {
    runTest("com.salesforce.phoenix.end2end.KeyOnlyTest")
  }

  @Test
  public void testMultiCfQueryExec() {
    runTest("com.salesforce.phoenix.end2end.MultiCfQueryExecTest")
  }

  @Test
  public void testNativeHBaseTypes() {
    runTest("com.salesforce.phoenix.end2end.NativeHBaseTypesTest")
  }

  @Test
  public void testPercentile() {
    runTest("com.salesforce.phoenix.end2end.PercentileTest")
  }

  @Test
  public void testProductMetrics() {
    runTest("com.salesforce.phoenix.end2end.ProductMetricsTest")
  }

  @Test
  public void testQueryDatabaseMetaData() {
    runTest("com.salesforce.phoenix.end2end.QueryDatabaseMetaDataTest")
  }

  @Test
  public void testQueryExec() {
    runTest("com.salesforce.phoenix.end2end.QueryExecTest")
  }

  @Test
  public void testQueryExecWithoutSCN() {
    runTest("com.salesforce.phoenix.end2end.QueryExecWithoutSCNTest")
  }

  @Test
  public void testQueryPlan() {
    runTest("com.salesforce.phoenix.end2end.QueryPlanTest")
  }

  @Test
  public void testReadIsolationLevel() {
    runTest("com.salesforce.phoenix.end2end.ReadIsolationLevelTest")
  }

  @Test
  public void testReverseFunction() {
    runTest("com.salesforce.phoenix.end2end.ReverseFunctionTest")
  }

  @Test
  public void testServerException() {
    runTest("com.salesforce.phoenix.end2end.ServerExceptionTest")
  }

  @Test
  public void testSkipRangeParallelIteratorRegionSplitter() {
    runTest("com.salesforce.phoenix.end2end.SkipRangeParallelIteratorRegionSplitterTest")
  }

  @Test
  public void testSkipScanQuery() {
    runTest("com.salesforce.phoenix.end2end.SkipScanQueryTest")
  }

  @Test
  public void testSpooledOrderBy() {
    runTest("com.salesforce.phoenix.end2end.SpooledOrderByTest")
  }

  @Test
  public void testStatementHints() {
    runTest("com.salesforce.phoenix.end2end.StatementHintsTest")
  }

  @Test
  public void testStddev() {
    runTest("com.salesforce.phoenix.end2end.StddevTest")
  }

  @Test
  public void testTopN() {
    runTest("com.salesforce.phoenix.end2end.TopNTest")
  }

  @Test
  public void testUpsertBigValues() {
    runTest("com.salesforce.phoenix.end2end.UpsertBigValuesTest")
  }

  @Test
  public void testUpsertSelect() {
    runTest("com.salesforce.phoenix.end2end.UpsertSelectTest")
  }

  @Test
  public void testUpsertValues() {
    runTest("com.salesforce.phoenix.end2end.UpsertValuesTest")
  }

  @Test
  public void testVariableLengthPK() {
    runTest("com.salesforce.phoenix.end2end.VariableLengthPKTest")
  }

  // INDEX

  @Test
  public void testIndex() {
    runTest("com.salesforce.phoenix.end2end.index.IndexTest")
  }

  @Test
  public void testIndexMetadata() {
    runTest("com.salesforce.phoenix.end2end.index.IndexMetadataTest")
  }

  // SALTED

  @Test
  public void testSaltedTable() {
    runTest("com.salesforce.phoenix.end2end.salted.SaltedTableTest")
  }

  @Test
  public void testSaltedTableUpsertSelect() {
    runTest("com.salesforce.phoenix.end2end.salted.SaltedTableUpsertSelectTest")
  }

  @Test
  public void testSaltedTableVarLengthRowKey() {
    runTest("com.salesforce.phoenix.end2end.salted.SaltedTableVarLengthRowKeyTest")
  }

}
