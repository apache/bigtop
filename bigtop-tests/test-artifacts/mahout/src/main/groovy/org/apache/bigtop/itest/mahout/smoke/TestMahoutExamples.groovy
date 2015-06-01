/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.bigtop.itest.mahout.smoke;

import static org.junit.Assert.assertTrue;
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

  /**
   * If MAHOUT_HOME is supplied, use that as the executable.  Else, use
   * mahout.  This eases the testing of tarball installations and other scenarios
   * where possible more than one version of an ecosystem component is available.
   */
  public static String MAHOUT_HOME = System.getenv("MAHOUT_HOME");
  public static String MAHOUT = MAHOUT_HOME ? MAHOUT_HOME + "/bin/mahout" : "mahout"

  private static Shell sh = new Shell("/bin/bash -s");
  public static String download_dir = System.getProperty("mahout.examples.resources.download.path") ?: "/tmp";

  /**
   *  Mahout smokes rely on a lot of external files.  So we
   *  modularize the downloads into a single function, so that
   *  the setup is easier to debug.  If any download results in a
   *  small file (i.e. due to 404 or 500 error), assertion will fail
   *  before the smokes actually start.
   */
  public static void download() {

    //key value pairs : data file > url that file resides on.
    def urlmap = [
      "20news-bydate.tar.gz":
        "http://people.csail.mit.edu/jrennie/20Newsgroups/20news-bydate.tar.gz",

      "reuters21578.tar.gz":
        "http://kdd.ics.uci.edu/databases/reuters21578/reuters21578.tar.gz",

      "synthetic_control.data":
        "http://archive.ics.uci.edu/ml/databases/synthetic_control/synthetic_control.data",

      "ml-1m.zip":
        "http://files.grouplens.org/papers/ml-1m.zip"
    ];
    //For each url above, download it.
    urlmap.each() {
      f_name, loc ->
        sh.exec("if [ ! -f ${download_dir}/${f_name} ]; then " +
          "curl ${loc} -o ${download_dir}/${f_name}; " +
          "fi");
        File file = new File("${download_dir}/${f_name}");

        assertTrue("file " + f_name + " at  " + loc + " len=" + file.length() + " is > 5k bytes", file.length() > 5000);
    }

  }

  /**
   * Individual tests (i.e. movie lens factorizer) will selectively copy this directory into the
   * distributed file system & then run tests against it (i.e. movie lens factorizer uses "fs -put" after
   * formatting a csv file in the tmp dir).
   */
  @BeforeClass
  public static void setUp() {
    download();

    // uncompress archives
    sh.exec("mkdir ${TEMP_DIR}",
      "cd ${TEMP_DIR}",
      //Create news-date data dir :: input for classifier test
      "mkdir 20news-bydate",
      "cd 20news-bydate",
      "tar xzf ${download_dir}/20news-bydate.tar.gz",
      "cd ..",
      //Create news-all data directory :: input for LDA test
      "mkdir 20news-all",
      "cp -R 20news-bydate/*/* 20news-all",
      "mkdir reuters-sgm",
      "cd reuters-sgm",
      "tar xzf ${download_dir}/reuters21578.tar.gz",
      "cd ..",
      //Create movie lens data directory :: input data for movie recommender test
      "mkdir movielens",
      "cd movielens",
      "unzip ${download_dir}/ml-1m.zip");
    assertEquals("Failed to uncompress archives", 0, sh.getRet());
    sh.exec("hadoop fs -mkdir ${WORK_DIR}");
    assertEquals("Unable to create work dir in HCFS", 0, sh.getRet());
    rmr("temp");
  }

  /**
   * Run method that tests for 0 return code and logs the entire command.
   */
  public void assertRun(String mahoutJob) {
    final String cmd = MAHOUT + " " + mahoutJob;

    //Cat the commands to a central file thats easy to tail.
    //TODO a simpler
    sh.exec("echo \"" + cmd + "\" >> /var/log/mahout.smoke");
    sh.exec(cmd);
    assertEquals("non-zero return! :::: " + cmd + " :::: out= " + sh.out + " :::: err= " + sh.err, 0, sh.getRet());
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
      assertEquals("Deletion of $path from the underlying FileSystem failed", 0, sh.getRet());
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

  //iterations for factorizer, original value was "10",
  //on a small 4 node cluster, 2 iterations
  //should complete in about 5 minutes or so.
  static final int ITERATIONS = 2;

  /**
   * This is the full workflow for creating recommendations based on movie
   * ratings including creating training/test data, ALS for training, evaluating
   * the ALS, and then outputting final movie recommendations for users.
   */
  @Test(timeout = 12000000L)
  public void factorizeMovieLensRatings() {
    // convert ratings
    sh.exec("cat ${TEMP_DIR}/movielens/ml-1m/ratings.dat |sed -e s/::/,/g| cut -d, -f1,2,3 > ${TEMP_DIR}/movielens/ratings.csv");
    assertEquals("Unexpected error from converting ratings", 0, sh.getRet());

    // put ratings in hdfs
    sh.exec("hadoop fs -mkdir ${WORK_DIR}/movielens",
      "hadoop fs -put ${TEMP_DIR}/movielens/ratings.csv ${WORK_DIR}/movielens/ratings.csv");
    assertEquals("Unable to put movielens/ratings.csv in hdfs", 0, sh.getRet());

    //create a 90% percent training set and a 10% probe set
    assertRun("splitDataset --input ${WORK_DIR}/movielens/ratings.csv --output ${WORK_DIR}/dataset " +
      "--trainingPercentage 0.9 --probePercentage 0.1 --tempDir ${WORK_DIR}/dataset/tmp");

    //Default iterations was 10, but for simple smokes that most might run,
    //2 iterations will confirm enough to move on.

    //run distributed ALS-WR to factorize the rating matrix based on the training set

    assertRun("parallelALS --input ${WORK_DIR}/dataset/trainingSet/ --output ${WORK_DIR}/als/out " +
      "--tempDir ${WORK_DIR}/als/tmp --numFeatures 20 --numIterations ${ITERATIONS} --lambda 0.065");

    //remove this
    sh.exec("hadoop fs -ls ${WORK_DIR}/als/out >> /tmp/mahoutdebug");
    //compute predictions against the probe set, measure the error
    assertRun("evaluateFactorization --output ${WORK_DIR}/als/rmse --input ${WORK_DIR}/dataset/probeSet/ " +
      "--userFeatures ${WORK_DIR}/als/out/U/ --itemFeatures ${WORK_DIR}/als/out/M/ --tempDir ${WORK_DIR}/als/tmp");

    //compute recommendations
    assertRun("recommendfactorized --input ${WORK_DIR}/als/out/userRatings/ --output ${WORK_DIR}/recommendations " +
      "--userFeatures ${WORK_DIR}/als/out/U/ --itemFeatures ${WORK_DIR}/als/out/M/ " +
      "--numRecommendations 6 --maxRating 5");

    // check that error has been calculated
    assertEquals("${WORK_DIR}/als/rmse/rmse.txt does not exist", 0, sh.getRet());
    // print the error
    sh.exec("hadoop fs -cat ${WORK_DIR}/als/rmse/rmse.txt");
    assertEquals("Unexpected error from running hadoop", 0, sh.getRet());

    // check that recommendations has been calculated
    sh.exec("hadoop fs -test -e ${WORK_DIR}/recommendations/part-m-00000");
    assertEquals("${WORK_DIR}/recommendations/part-m-00000 does not exist", 0, sh.getRet());
  }

  /**
   * Alternative to parameterized test: this is a test that is implemented by each
   * individual clustering test.
   *
   * Explanation of clustering tests:
   *
   * Each of the below tests runs a different clustering algorithm against the same
   * input data set, against synthesize "control" data.  "Control data" is data that shows
   * the time series performance of a process.  For example, a cellphone company
   * might want to run this to find which regions have decreasing performance over time (i.e. due to increased population),
   * versus places which have cyclic performance (i.e. due to weather).
   */
  private void _clusterSyntheticControlData(String algorithm) {
    rmr("testdata");
    sh.exec("hadoop fs -mkdir testdata",
      "hadoop fs -put ${download_dir}/synthetic_control.data testdata");
    assertEquals("Unable to put data in hdfs", 0, sh.getRet());
    assertRun("org.apache.mahout.clustering.syntheticcontrol.${algorithm}.Job");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
  }

  @Test(timeout = 900000L)
  public void clusterControlDataWithCanopy() {
    _clusterSyntheticControlData("canopy");
  }

  @Test(timeout = 9000000L)
  public void clusterControlDataWithKMeans() {
    _clusterSyntheticControlData("kmeans");
  }

  @Test(timeout = 9000000L)
  public void clusterControlDataWithFuzzyKMeans() {
    _clusterSyntheticControlData("fuzzykmeans");
  }

  /**
   * Test the creation of topical clusters from raw lists words using LDA.
   */
  @Test(timeout = 7200000L)
  public void testReutersLDA() {
    // where does lda.algorithm come in?
    assertRun("org.apache.lucene.benchmark.utils.ExtractReuters ${TEMP_DIR}/reuters-sgm ${TEMP_DIR}/reuters-out");
    //put ${TEMP_DIR}/reuters-out into hdfs as we have to run seqdirectory in mapreduce mode, so files need be in hdfs
    sh.exec("hadoop fs -put ${TEMP_DIR}/reuters-out ${WORK_DIR}/reuters-out");
    assertEquals("Unable to put reuters-out-seqdir in hdfs", 0, sh.getRet());

    assertRun("seqdirectory -i ${TEMP_DIR}/reuters-out -o ${TEMP_DIR}/reuters-out-seqdir -c UTF-8 -chunk 5");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());
    /*
    // reuters-out-seqdir exists on a local disk at this point,
    // copy it to hdfs
    rmr("${WORK_DIR}/reuters-out-seqdir");
    sh.exec("hadoop fs -put ${TEMP_DIR}/reuters-out-seqdir ${WORK_DIR}/reuters-out-seqdir");
    assertEquals("Unable to put reuters-out-seqdir in hdfs", 0, sh.getRet());
    */
    assertRun("""seq2sparse \
-i ${WORK_DIR}/reuters-out-seqdir/ \
-o ${WORK_DIR}/reuters-out-seqdir-sparse-lda \
-wt tf -seq -nr 3 --namedVector""");

    sh.exec("hadoop fs -mkdir ${WORK_DIR}/reuters-lda");
    assertEquals("Unable to make dir reuters-lda in hdfs", 0, sh.getRet());

    assertRun("""lda \
-i ${WORK_DIR}/reuters-out-seqdir-sparse-lda/tf-vectors \
-o ${WORK_DIR}/reuters-lda -k 20 -x 20 \
&& \
mahout ldatopics \
-i ${WORK_DIR}/reuters-lda/state-20 \
-d ${WORK_DIR}/reuters-out-seqdir-sparse-lda/dictionary.file-0 \
-dt sequencefile""");
  }

  /**
   * Note that this test doesnt work on some older mahout versions.
   */
  @Test(timeout = 9000000L)
  public void testBayesNewsgroupClassifier() {
    // put bayes-train-input and bayes-test-input in hdfs
    sh.exec("hadoop fs -mkdir ${WORK_DIR}/20news-vectors");
    sh.exec("hadoop fs -put ${TEMP_DIR}/20news-all ${WORK_DIR}/20news-all");
    assertEquals("Unable to put bayes-train-input in hdfs", 0, sh.getRet());

    assertRun("seqdirectory -i ${WORK_DIR}/20news-all -o ${WORK_DIR}/20news-seq");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    assertRun("seq2sparse -i ${WORK_DIR}/20news-seq -o ${WORK_DIR}/20news-vectors  -lnorm -nv  -wt tfidf");
    assertEquals("Unexpected error from running mahout", 0, sh.getRet());

    assertRun("""split \
-i ${WORK_DIR}/20news-vectors/tfidf-vectors \
--trainingOutput ${WORK_DIR}/20news-train-vectors \
--testOutput ${WORK_DIR}/20news-test-vectors \
--randomSelectionPct 40 --overwrite --sequenceFiles -xm sequential""");

    assertRun("""trainnb \
-i ${WORK_DIR}/20news-train-vectors \
-o ${WORK_DIR}/model \
-li ${WORK_DIR}/labelindex \
-ow""");

    assertRun("""testnb \
-i ${WORK_DIR}/20news-train-vectors \
-m ${WORK_DIR}/model \
-l ${WORK_DIR}/labelindex \
-ow -o ${WORK_DIR}/20news-testing""");

    assertRun("""testnb \
-i ${WORK_DIR}/20news-test-vectors \
-m ${WORK_DIR}/model \
-l ${WORK_DIR}/labelindex \
-ow -o ${WORK_DIR}/20news-testing""");
  }
}
