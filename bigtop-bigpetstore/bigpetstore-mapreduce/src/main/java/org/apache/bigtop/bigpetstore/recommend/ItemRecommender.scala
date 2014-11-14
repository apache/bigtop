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

package org.apache.bigtop.bigpetstore.recommend

import org.apache.mahout.cf.taste.hadoop.als.RecommenderJob
import org.apache.mahout.cf.taste.hadoop.als.ParallelALSFactorizationJob
import java.io.File
import parquet.org.codehaus.jackson.map.DeserializerFactory.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.conf.Configurable
import org.apache.hadoop.util.ToolRunner
import org.apache.mahout.cf.taste.hadoop.als.SharingMapper
import org.apache.hadoop.util.Tool
import org.apache.bigtop.bigpetstore.util.DeveloperTools

// We don't need to wrap these two jobs in ToolRunner.run calls since the only
// thing that we are doing right now is calling the run() methods of RecommenderJob
// and ParallelALSFactorizationJob. Both of these classes have a main() method that
// internally calls ToolRunner.run with all the command line args passed. So, if
// we want to run this code from the command line, we can easily do so by running
// the main methods of the ParallelALSFactorizationJob, followed by running the
// main method of RecommenderJob. That would also take care of the multiple-jvm
// instance issue metioned in the comments below, so the call to
class ItemRecommender(private val inputDir: String,
        private val factorizationOutputDir: String,
        private val recommendationsOutputDir: String) {
  private val recommenderJob = new RecommenderJob
  private val factorizationJob = new ParallelALSFactorizationJob

  private def tempDir = "/tmp/mahout_" + System.currentTimeMillis

  private def performAlsFactorization() = {
    ToolRunner.run(factorizationJob, Array(
        "--input", inputDir,
        "--output", factorizationOutputDir,
        "--lambda", "0.1",
        "--tempDir", tempDir,
        "--implicitFeedback", "false",
        "--alpha", "0.8",
        "--numFeatures", "2",
        "--numIterations", "5",
        "--numThreadsPerSolver", "1"))
  }

  private def generateRecommendations() = {
    ToolRunner.run(recommenderJob, (Array(
        "--input", factorizationOutputDir + "/userRatings/",
        "--userFeatures", factorizationOutputDir + "/U/",
        "--itemFeatures", factorizationOutputDir + "/M/",
        "--numRecommendations", "1",
        "--output", recommendationsOutputDir,
        "--maxRating", "1")))
  }

  // At this point, the performAlsFactorization generateRecommendations
  // and this method can not be run from the same VM instance. These two jobs
  // share a common static variable which is not being handled correctly.
  // This, unfortunately, results in a class-cast exception being thrown. That's
  // why the resetFlagInSharedAlsMapper is required. See the comments on
  // resetFlagInSharedAlsMapper() method.
  def recommend = {
    performAlsFactorization
    resetFlagInSharedAlsMapper
    generateRecommendations
  }

  // necessary for local execution in the same JVM only. If the performAlsFactorization()
  // and generateRecommendations() calls are performed in separate JVM instances, this
  // would be taken care of automatically. However, if we want to run this two methods
  // as one task, we need to clean up the static state set by these methods, and we don't
  // have any legitimate way of doing this directly. This clean-up should have been
  // performed by ParallelALSFactorizationJob class after the job is finished.
  // TODO: remove this when a better way comes along, or ParallelALSFactorizationJob
  // takes responsibility.
  private def resetFlagInSharedAlsMapper {
    val m = classOf[SharingMapper[_, _, _, _, _]].getDeclaredMethod("reset");
    m setAccessible true
    m.invoke(null)
  }
}

object ItemRecommender {
  def main(args: Array[String]) {
      val res = ToolRunner.run(new Configuration(), new Tool() {
      var conf: Configuration = _;

      override def setConf(conf: Configuration) {
        this.conf=conf;
      }


      override def getConf() = {
        this.conf;
      }


      override def run(toolArgs: Array[String]) = {
        val ir = new ItemRecommender(toolArgs(0), toolArgs(1), toolArgs(2))
        ir.recommend
        0;
      }
    }, args);
    System.exit(res);
  }
}