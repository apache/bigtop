/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.bigpetstore.clustering;

import org.apache.bigtop.bigpetstore.util.DeveloperTools;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.cf.taste.hadoop.item.RecommenderJob;
import org.apache.mahout.cf.taste.hadoop.preparation.PreparePreferenceMatrixJob;
import org.apache.pig.builtin.LOG;

/**
 * Implement user based collab filter.
 *
 * The input set is the
 *
 * userid,productid,weight
 *
 * rows.
 */
public class BPSRecommnder implements Tool {


    Configuration c;
    @Override
    public void setConf(Configuration conf) {
        c=conf;
    }

    @Override
    public Configuration getConf() {
        return c;
    }

    @Override
    public int run(String[] args) throws Exception {
        DeveloperTools.validate(args,"input path","output path");

        Configuration conf = new Configuration();

        System.out.println("Runnning recommender against : " + args[0] +" -> " + args[1]);

        RecommenderJob recommenderJob = new RecommenderJob();
        /**
        int x = ToolRunner.run(getConf(), new BPSPreparePreferenceMatrixJob(), new String[]{
            "--input", args[0],
            "--output", args[1],
            "--tempDir", "/tmp",
          });
        System.out.println("RETURN = " + x);
         **/

        int ret = recommenderJob.run(new String[] {
             "--input",args[0],
             "--output",args[1],
             "--usersFile","/tmp/users.txt",
             "--tempDir", "/tmp/mahout_"+System.currentTimeMillis(),
             "--similarityClassname", "SIMILARITY_PEARSON_CORRELATION",
             "--threshold",".00000000001",
             "--numRecommendations", "4",
             //"--encodeLongsAsInts",
             //Boolean.FALSE.toString(),
             //"--itemBased", Boolean.FALSE.toString()
        });

        System.out.println("Exit of recommender: " + ret);
        return ret;
    }
}