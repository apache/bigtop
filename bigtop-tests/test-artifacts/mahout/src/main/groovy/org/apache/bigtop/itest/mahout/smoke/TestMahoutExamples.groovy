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
package org.apache.bigtop.itest.mahout.smoke;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;

/**
 * Test Mahout examples shipped with the distribution.
 */
public class TestMahoutExamples {
  public static final String TEMP_DIR = "/tmp/mahout.${(new Date().getTime())}";
  public static final String WORK_DIR = TEMP_DIR;
  private static Shell sh = new Shell("/bin/bash -s");
  public static String download_dir = System.getProperty("mahout.examples.resources.download.path") ?: "/tmp" ;

  @BeforeClass
  public static void setUp() {
    // download resources
    sh.exec(
    "if [ ! -f ${download_dir}/20news-bydate.tar.gz ]; then " +
      "curl http://people.csail.mit.edu/jrennie/20Newsgroups/20news-bydate.tar.gz -o ${download_dir}/20news-bydate.tar.gz; " +
    "fi");
    sh.exec(
    "if [ ! -f ${download_dir}/reuters21578.tar.gz ]; then " +
      "curl http://kdd.ics.uci.edu/databases/reuters21578/reuters21578.tar.gz -o ${download_dir}/reuters21578.tar.gz; " +
    "fi");
    sh.exec(
    "if [ ! -f ${download_dir}/synthetic_control.data ]; then " +
      "curl http://archive.ics.uci.edu/ml/databases/synthetic_control/synthetic_control.data -o ${download_dir}/synthetic_control.data; " +
    "fi");
    sh.exec(
    "if [ ! -f ${download_dir}/ml-1m.zip ]; then " +
      "curl http://www.grouplens.org/system/files/ml-1m.zip -o ${download_dir}/ml-1m.zip; " +
    "fi");
    // uncompress archives
    // 20news-bydate.tar.gz
    // reuters21578.tar.gz
    // ml-1m.zip
    sh.exec("mkdir ${TEMP_DIR}",
            "cd ${TEMP_DIR}",
            "mkdir 20news-bydate",
            "cd 20news-bydate",
            "tar xzf ${download_dir}/20news-bydate.tar.gz",
            "cd ..",
            "mkdir 20news-all",
            "cp -R 20news-bydate/*/* 20news-all",
            "mkdir reuters-sgm",
            "cd reuters-sgm",
            "tar xzf ${download_dir}/reuters21578.tar.gz",
            "cd ..",
            "mkdir movielens",
            "cd movielens",
            "unzip ${download_dir}/ml-1m.zip");
    assertEquals("Failed to uncompress archives", 0, sh.getRet());
    sh.exec("hadoop fs -mkdir ${WORK_DIR}");
    assertEquals("Unable to create work dir in hdfs", 0, sh.getRet());
    rmr("temp");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("rm -rf ${TEMP_DIR}",
            "hadoop fs -rmr ${WORK_DIR}");
  }

  private static void rmr(String path) {
    sh.exec("hadoop fs -test -e $path");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $path");
      assertEquals("Deletion of $path from HDFS failed", 0, sh.getRet());
    }
  }

  @After
  public void killHangingProcess() {
    sh.exec("mapred job -list | grep 'Total jobs:0'");
    if (sh.getRet() == 0) {
      sh.exec("for jobid in `mapred job -list | grep 'RUNNING' |awk '{print \$1}'`;",
              "do mapred job -kill \${jobid};",
              "done");
    }
  }

  @Test(timeout=12000000L)
  public void factorizeMovieLensRatings() {
    // convert ratings
    sh.exec("cat ${TEMP_DIR}/movielens/ml-1m/ratings.dat |sed -e s/::/,/g| cut -d, -f1,2,3 > ${TEMP_DIR}/movielens/ratings.csv");
    assertEquals("Unexpected error from converting ratings", 0, sh.getRet());

    // put ratings in hdfs
    sh.exec("hadoop fs -mkdir ${WORK_DIR}/movielens",
            "hadoop fs -put ${TEMP_DIR}/movielens/ratings.csv ${WORK_DIR}/movielens/ratings.csv");
    assertEquals("Unable to put movielens/ratings.csv in hdfs", 0, sh.getRet());

    //create a 90% percent training set and a 10% probe set
    sh.exec("mahout splitDataset --input ${WORK_DIR}/movielens/ratings.csv --output ${WORK_DIR}/dataset " +
            "--trainingPercentage 0.9 --probePercentage 0.1 --tempDir ${WORK_DIR}/dataset/tmp");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    //run distributed ALS-WR to factorize the rating matrix based on the training set
    sh.exec("mahout parallelALS --input ${WORK_DIR}/dataset/trainingSet/ --output ${WORK_DIR}/als/out " +
            "--tempDir ${WORK_DIR}/als/tmp --numFeatures 20 --numIterations 10 --lambda 0.065");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    //compute predictions against the probe set, measure the error
    sh.exec("mahout evaluateFactorization --output ${WORK_DIR}/als/rmse --input ${WORK_DIR}/dataset/probeSet/ " +
            "--userFeatures ${WORK_DIR}/als/out/U/ --itemFeatures ${WORK_DIR}/als/out/M/ --tempDir ${WORK_DIR}/als/tmp");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    //compute recommendations
    sh.exec("mahout recommendfactorized --input ${WORK_DIR}/als/out/userRatings/ --output ${WORK_DIR}/recommendations " +
            "--userFeatures ${WORK_DIR}/als/out/U/ --itemFeatures ${WORK_DIR}/als/out/M/ " +
            "--numRecommendations 6 --maxRating 5");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    // check that error has been calculated
    sh.exec("hadoop fs -test -e ${WORK_DIR}/als/rmse/rmse.txt");
    assertEquals("${WORK_DIR}/als/rmse/rmse.txt does not exist", 0, sh.getRet());
    // print the error
    sh.exec("hadoop fs -cat ${WORK_DIR}/als/rmse/rmse.txt");
    assertEquals("Unexpected error from running hadoop", 0, sh.getRet());

    // check that recommendations has been calculated
    sh.exec("hadoop fs -test -e ${WORK_DIR}/recommendations/part-m-00000");
    assertEquals("${WORK_DIR}/recommendations/part-m-00000 does not exist", 0, sh.getRet());
  }

  // it's too much of a pain to use junit parameterized tests, so do it
  // the simple way
  private void _clusterSyntheticControlData(String algorithm) {
    rmr("testdata");
    sh.exec("hadoop fs -mkdir testdata",
            "hadoop fs -put ${download_dir}/synthetic_control.data testdata");
    assertEquals("Unable to put data in hdfs", 0, sh.getRet());
    sh.exec("mahout org.apache.mahout.clustering.syntheticcontrol.${algorithm}.Job");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
  }

  @Test(timeout=900000L)
  public void clusterControlDataWithCanopy() {
    _clusterSyntheticControlData("canopy");
  }

  @Test(timeout=9000000L)
  public void clusterControlDataWithKMeans() {
    _clusterSyntheticControlData("kmeans");
  }

  @Test(timeout=9000000L)
  public void clusterControlDataWithFuzzyKMeans() {
    _clusterSyntheticControlData("fuzzykmeans");
  }

  @Test(timeout=900000L)
  public void clusterControlDataWithDirichlet() {
    _clusterSyntheticControlData("dirichlet");
  }

  @Test(timeout=900000L)
  public void clusterControlDataWithMeanShift() {
    _clusterSyntheticControlData("meanshift");
  }

  @Test(timeout=7200000L)
  public void testReutersLDA() {
    // where does lda.algorithm come in?
    sh.exec("mahout org.apache.lucene.benchmark.utils.ExtractReuters ${TEMP_DIR}/reuters-sgm ${TEMP_DIR}/reuters-out");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
    //put ${TEMP_DIR}/reuters-out into hdfs as we have to run seqdirectory in mapreduce mode, so files need be in hdfs
    sh.exec("hadoop fs -put ${TEMP_DIR}/reuters-out ${WORK_DIR}/reuters-out");
    assertEquals("Unable to put reuters-out-seqdir in hdfs", 0, sh.getRet());   

    sh.exec("mahout seqdirectory -i ${TEMP_DIR}/reuters-out -o ${TEMP_DIR}/reuters-out-seqdir -c UTF-8 -chunk 5");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
    /*
    // reuters-out-seqdir exists on a local disk at this point,
    // copy it to hdfs
    rmr("${WORK_DIR}/reuters-out-seqdir");
    sh.exec("hadoop fs -put ${TEMP_DIR}/reuters-out-seqdir ${WORK_DIR}/reuters-out-seqdir");
    assertEquals("Unable to put reuters-out-seqdir in hdfs", 0, sh.getRet());
    */
    sh.exec("""mahout seq2sparse \
    -i ${WORK_DIR}/reuters-out-seqdir/ \
    -o ${WORK_DIR}/reuters-out-seqdir-sparse-lda \
    -wt tf -seq -nr 3 --namedVector""");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("hadoop fs -mkdir ${WORK_DIR}/reuters-lda");
    assertEquals("Unable to make dir reuters-lda in hdfs", 0, sh.getRet());

    sh.exec("""mahout lda \
    -i ${WORK_DIR}/reuters-out-seqdir-sparse-lda/tf-vectors \
    -o ${WORK_DIR}/reuters-lda -k 20 -x 20 \
  && \
  mahout ldatopics \
    -i ${WORK_DIR}/reuters-lda/state-20 \
    -d ${WORK_DIR}/reuters-out-seqdir-sparse-lda/dictionary.file-0 \
    -dt sequencefile""");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
  }

  @Test(timeout=9000000L)
  public void testBayesNewsgroupClassifier() {
    // put bayes-train-input and bayes-test-input in hdfs
    sh.exec("hadoop fs -mkdir ${WORK_DIR}/20news-vectors");
    sh.exec("hadoop fs -put ${TEMP_DIR}/20news-all ${WORK_DIR}/20news-all");
    assertEquals("Unable to put bayes-train-input in hdfs", 0, sh.getRet());

    sh.exec("mahout seqdirectory -i ${WORK_DIR}/20news-all -o ${WORK_DIR}/20news-seq");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("mahout seq2sparse -i ${WORK_DIR}/20news-seq -o ${WORK_DIR}/20news-vectors  -lnorm -nv  -wt tfidf");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("""mahout split \
-i ${WORK_DIR}/20news-vectors/tfidf-vectors \
--trainingOutput ${WORK_DIR}/20news-train-vectors \
--testOutput ${WORK_DIR}/20news-test-vectors \
--randomSelectionPct 40 --overwrite --sequenceFiles -xm sequential""");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("""mahout trainnb \
-i ${WORK_DIR}/20news-train-vectors -el \
-o ${WORK_DIR}/model \
-li ${WORK_DIR}/labelindex \
-ow"""); 
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("""mahout testnb \
-i ${WORK_DIR}/20news-train-vectors \
-m ${WORK_DIR}/model \
-l ${WORK_DIR}/labelindex \
-ow -o ${WORK_DIR}/20news-testing""");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    sh.exec("""mahout testnb \
-i ${WORK_DIR}/20news-test-vectors \
-m ${WORK_DIR}/model \
-l ${WORK_DIR}/labelindex \
-ow -o ${WORK_DIR}/20news-testing""");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

  }
}
