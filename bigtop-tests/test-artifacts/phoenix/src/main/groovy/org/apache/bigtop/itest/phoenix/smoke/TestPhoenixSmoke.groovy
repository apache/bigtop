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
  static String phoenixCoreJar = PHOENIX_HOME + "/lib/" +
    JarContent.getJarName(PHOENIX_HOME + "/lib/", "phoenix-core.*.jar");
  static String phoenixCoreTestsJar = PHOENIX_HOME + "/lib/" +
    JarContent.getJarName(PHOENIX_HOME + "/lib/", "phoenix-core.*tests.jar");

  // Run a Phoenix end to end unit test using the hbase exec script.
  // This really simplifies the testing setup we would otherwise need
  // to accomplish with Maven and will be more amenable to change if
  // the Phoenix tests someday can be run as test drivers against a
  // running cluster as well.

  static void runTest(String testName) {
    sh.exec("HBASE_CLASSPATH=" + phoenixClientJar + ":" + phoenixCoreJar + ":" + phoenixCoreTestsJar +
      " hbase org.junit.runner.JUnitCore " + testName);
    assertTrue(testName + " failed", sh.getRet() == 0);
  }

  @Test
  public void testAlterTable() {
    runTest("org.apache.phoenix.end2end.AlterTableIT")
  }

  @Test
  public void testArithmeticQuery() {
    runTest("org.apache.phoenix.end2end.ArithmeticQueryIT")
  }

  @Test
  public void testArray() {
    runTest("org.apache.phoenix.end2end.ArrayIT")
  }

  @Test
  public void testAutoCommit() {
    runTest("org.apache.phoenix.end2end.AutoCommitIT")
  }

  @Test
  public void testBinaryRowKey() {
    runTest("org.apache.phoenix.end2end.BinaryRowKeyIT")
  }

  @Test
  public void testCoalesceFunction() {
    runTest("org.apache.phoenix.end2end.CoalesceFunctionIT")
  }

  @Test
  public void testColumnProjectionOptimization() {
    runTest("org.apache.phoenix.end2end.ColumnProjectionOptimizationIT")
  }

  @Test
  public void testCompareDecimalToLong() {
    runTest("org.apache.phoenix.end2end.CompareDecimalToLongIT")
  }

  @Test
  public void testCreateTable() {
    runTest("org.apache.phoenix.end2end.CreateTableIT")
  }

  @Test
  public void testCSVCommonsLoader() {
    runTest("org.apache.phoenix.end2end.CSVCommonsLoaderIT")
  }

  @Test
  public void testCustomEntityData() {
    runTest("org.apache.phoenix.end2end.CustomEntityDataIT")
  }

  @Test
  public void testDefaultParallelIteratorsRegionSplitter() {
    runTest("org.apache.phoenix.end2end.DefaultParallelIteratorsRegionSplitterIT")
  }

  @Test
  public void testDelete() {
    runTest("org.apache.phoenix.end2end.DeleteIT")
  }

  @Test
  public void testDistinctCount() {
    runTest("org.apache.phoenix.end2end.DistinctCountIT")
  }

  @Test
  public void testDynamicColumn() {
    runTest("org.apache.phoenix.end2end.DynamicColumnIT")
  }

  @Test
  public void testDynamicFamily() {
    runTest("org.apache.phoenix.end2end.DynamicFamilyIT")
  }

  @Test
  public void testDynamicUpsert() {
    runTest("org.apache.phoenix.end2end.DynamicUpsertIT")
  }

  @Test
  public void testExecuteStatements() {
    runTest("org.apache.phoenix.end2end.ExecuteStatementsIT")
  }

  @Test
  public void testExtendedQueryExec() {
    runTest("org.apache.phoenix.end2end.ExtendedQueryExecIT")
  }

  @Test
  public void testFunkyNames() {
    runTest("org.apache.phoenix.end2end.FunkyNamesIT")
  }

  @Test
  public void testGroupByCase() {
    runTest("org.apache.phoenix.end2end.GroupByCaseIT")
  }

  @Test
  public void testHashJoin() {
    runTest("org.apache.phoenix.end2end.HashJoinIT")
  }

  @Test
  public void testInMemoryOrderBy() {
    runTest("org.apache.phoenix.end2end.InMemoryOrderByIT")
  }

  @Test
  public void testIsNull() {
    runTest("org.apache.phoenix.end2end.IsNullIT")
  }

  @Test
  public void testKeyOnly() {
    runTest("org.apache.phoenix.end2end.KeyOnlyIT")
  }

  @Test
  public void testMD5Function() {
    runTest("org.apache.phoenix.end2end.MD5FunctionIT")
  }

  @Test
  public void testMultiCfQueryExec() {
    runTest("org.apache.phoenix.end2end.MultiCfQueryExecIT")
  }

  @Test
  public void testNativeHBaseTypes() {
    runTest("org.apache.phoenix.end2end.NativeHBaseTypesIT")
  }

  @Test
  public void testOrderBy() {
    runTest("org.apache.phoenix.end2end.OrderByIT")
  }

  @Test
  public void testPercentile() {
    runTest("org.apache.phoenix.end2end.PercentileIT")
  }

  @Test
  public void testProductMetrics() {
    runTest("org.apache.phoenix.end2end.ProductMetricsIT")
  }

  @Test
  public void testQueryDatabaseMetaData() {
    runTest("org.apache.phoenix.end2end.QueryDatabaseMetaDataIT")
  }

  @Test
  public void testQueryExecWithoutSCN() {
    runTest("org.apache.phoenix.end2end.QueryExecWithoutSCNIT")
  }

  @Test
  public void testQuery() {
    runTest("org.apache.phoenix.end2end.QueryIT")
  }

  @Test
  public void testQueryPlan() {
    runTest("org.apache.phoenix.end2end.QueryPlanIT")
  }

  @Test
  public void testReadIsolationLevel() {
    runTest("org.apache.phoenix.end2end.ReadIsolationLevelIT")
  }

  @Test
  public void testReverseFunction() {
    runTest("org.apache.phoenix.end2end.ReverseFunctionIT")
  }

  @Test
  public void testRoundFloorCeilFunctionsEnd2End() {
    runTest("org.apache.phoenix.end2end.RoundFloorCeilFunctionsEnd2EndIT")
  }

  @Test
  public void testRowValueConstructor() {
    runTest("org.apache.phoenix.end2end.RowValueConstructorIT")
  }

  @Test
  public void testSaltedView() {
    runTest("org.apache.phoenix.end2end.SaltedViewIT")
  }

  @Test
  public void testSequence() {
    runTest("org.apache.phoenix.end2end.SequenceIT")
  }

  @Test
  public void testServerException() {
    runTest("org.apache.phoenix.end2end.ServerExceptionIT")
  }

  @Test
  public void testSkipRangeParallelIteratorRegionSplitter() {
    runTest("org.apache.phoenix.end2end.SkipRangeParallelIteratorRegionSplitterIT")
  }

  @Test
  public void testSkipScanQuery() {
    runTest("org.apache.phoenix.end2end.SkipScanQueryIT")
  }

  @Test
  public void testSortOrderF() {
    runTest("org.apache.phoenix.end2end.SortOrderFIT")
  }

  @Test
  public void testSpillableGroupBy() {
    runTest("org.apache.phoenix.end2end.SpillableGroupByIT")
  }

  @Test
  public void testSpooledOrderBy() {
    runTest("org.apache.phoenix.end2end.SpooledOrderByIT")
  }

  @Test
  public void testStatementHints() {
    runTest("org.apache.phoenix.end2end.StatementHintsIT")
  }

  @Test
  public void testStatsManager() {
    runTest("org.apache.phoenix.end2end.StatsManagerIT")
  }

  @Test
  public void testStddev() {
    runTest("org.apache.phoenix.end2end.StddevIT")
  }

  @Test
  public void testTenantSpecificTablesDDL() {
    runTest("org.apache.phoenix.end2end.TenantSpecificTablesDDLIT")
  }

  @Test
  public void testTenantSpecificTablesDML() {
    runTest("org.apache.phoenix.end2end.TenantSpecificTablesDMLIT")
  }

  @Test
  public void testTenantSpecificViewIndex() {
    runTest("org.apache.phoenix.end2end.TenantSpecificViewIndexIT")
  }

  @Test
  public void testTenantSpecificViewIndexSalted() {
    runTest("org.apache.phoenix.end2end.TenantSpecificViewIndexSaltedIT")
  }

  @Test
  public void testToCharFunction() {
    runTest("org.apache.phoenix.end2end.ToCharFunctionIT")
  }

  @Test
  public void testToNumberFunction() {
    runTest("org.apache.phoenix.end2end.ToNumberFunctionIT")
  }

  @Test
  public void testTopN() {
    runTest("org.apache.phoenix.end2end.TopNIT")
  }

  @Test
  public void testTruncateFunction() {
    runTest("org.apache.phoenix.end2end.TruncateFunctionIT")
  }

  @Test
  public void testUpsertBigValues() {
    runTest("org.apache.phoenix.end2end.UpsertBigValuesIT")
  }

  @Test
  public void testUpsertSelectAutoCommit() {
    runTest("org.apache.phoenix.end2end.UpsertSelectAutoCommitIT")
  }

  @Test
  public void testUpsertSelect() {
    runTest("org.apache.phoenix.end2end.UpsertSelectIT")
  }

  @Test
  public void testUpsertValues() {
    runTest("org.apache.phoenix.end2end.UpsertValuesIT")
  }

  @Test
  public void testVariableLengthPK() {
    runTest("org.apache.phoenix.end2end.VariableLengthPKIT")
  }

  @Test
  public void testView() {
    runTest("org.apache.phoenix.end2end.ViewIT")
  }

  // INDEX

  @Test
  public void testDropView() {
    runTest("org.apache.phoenix.end2end.index.DropViewIT")
  }

  @Test
  public void testImmutableIndex() {
    runTest("org.apache.phoenix.end2end.index.ImmutableIndexIT")
  }

  @Test
  public void testIndexMetadata() {
    runTest("org.apache.phoenix.end2end.index.IndexMetadataIT")
  }

  @Test
  public void testMutableIndexFailure() {
    runTest("org.apache.phoenix.end2end.index.MutableIndexFailureIT")
  }

  @Test
  public void testMutableIndex() {
    runTest("org.apache.phoenix.end2end.index.MutableIndexIT")
  }

  @Test
  public void testSaltedIndex() {
    runTest("org.apache.phoenix.end2end.index.SaltedIndexIT")
  }

  // SALTED

  @Test
  public void testSaltedTable() {
    runTest("org.apache.phoenix.end2end.salted.SaltedTableIT")
  }

  @Test
  public void testSaltedTableUpsertSelect() {
    runTest("org.apache.phoenix.end2end.salted.SaltedTableUpsertSelectIT")
  }

  @Test
  public void testSaltedTableVarLengthRowKey() {
    runTest("org.apache.phoenix.end2end.salted.SaltedTableVarLengthRowKeyIT")
  }

}
