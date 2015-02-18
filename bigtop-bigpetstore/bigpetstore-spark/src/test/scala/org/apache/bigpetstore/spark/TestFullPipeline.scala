package org.apache.bigpetstore.spark

import org.apache.bigtop.bigpetstore.spark.analytics.PetStoreStatistics
import org.apache.bigtop.bigpetstore.spark.datamodel.{Statistics, IOUtils}
import org.apache.bigtop.bigpetstore.spark.etl.ETLParameters
import org.apache.bigtop.bigpetstore.spark.etl.SparkETL
import org.apache.bigtop.bigpetstore.spark.etl.{ETLParameters, SparkETL}
import org.apache.bigtop.bigpetstore.spark.generator.SparkDriver
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.junit.JUnitRunner

import Array._

import java.io.File
import java.nio.file.Files

import org.apache.spark.{SparkContext, SparkConf}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


// hack for running tests with Gradle
@RunWith(classOf[JUnitRunner])
class TestFullPipeline extends FunSuite with BeforeAndAfterAll {

  val conf = new SparkConf().setAppName("BPS Data Generator Test Suite").setMaster("local[2]")
  val sc = new SparkContext(conf)

  override def afterAll() {
    sc.stop()
  }

  test("Full integration test.") {

    // First generate the data.
    val tmpDir:File = Files.createTempDirectory("sparkDriverSuiteGeneratedData2").toFile()

    //stores, customers, days, randomSeed
    val parameters:Array[String] = Array(tmpDir.toString(), "10", "1000", "365.0","123456789")
    SparkDriver.parseArgs(parameters)

    val transactionRDD = SparkDriver.generateData(sc)
    SparkDriver.writeData(transactionRDD)

    //Now ETL the data
    val etlDir:File = Files.createTempDirectory("BPSTest_ETL2").toFile()
    System.out.println(etlDir.getAbsolutePath + "== "+etlDir.list())

    val (locations,stores,customers,products,transactions) = SparkETL.run(sc, new ETLParameters(tmpDir.getAbsolutePath,etlDir.getAbsolutePath))

    // assert(locations==400L) TODO : This seems to vary (325,400,)
    assert(stores==10L)
    assert(customers==1000L)
    assert(products==55L)
    //assert(transactions==45349L)

    //Now do the analytics.
    val analyticsJson = new File(tmpDir,"analytics.json")

    PetStoreStatistics.run(etlDir.getAbsolutePath,
      analyticsJson.getAbsolutePath, sc)

    val stats:Statistics = IOUtils.readLocalAsStatistics(analyticsJson)

    /**
     * Assert some very generic features.  We will refine this later once
     * consistency is implemented.
     * See https://github.com/rnowling/bigpetstore-data-generator/issues/38
     */
    assert(stats.totalTransactions === transactions)
    assert(stats.productDetails.length === products)
    assert(stats.transactionsByMonth.length === 12)

    sc.stop()
  }
}
