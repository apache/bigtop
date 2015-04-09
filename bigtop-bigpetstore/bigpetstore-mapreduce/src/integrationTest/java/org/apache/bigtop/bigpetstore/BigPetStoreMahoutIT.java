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
package org.apache.bigtop.bigpetstore;

import static org.apache.bigtop.bigpetstore.ITUtils.createTestOutputPath;
import static org.apache.bigtop.bigpetstore.ITUtils.setup;

import java.util.regex.Pattern;

import org.apache.bigtop.bigpetstore.recommend.ItemRecommender;
import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants.OUTPUTS.MahoutPaths;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicate;

public class BigPetStoreMahoutIT {

  //the cleaned path has subdirs, one for tsv, one for mahout numerical format, and so on.
  public static final Path INPUT_DIR_PATH =
          new Path(ITUtils.BPS_TEST_PIG_CLEANED_ROOT, MahoutPaths.Mahout.name());
  public static final String INPUT_DIR_PATH_STR = INPUT_DIR_PATH.toString();
  private static final Path MAHOUT_OUTPUT_DIR = createTestOutputPath(MahoutPaths.Mahout.name());
  private static final Path ALS_FACTORIZATION_OUTPUT_DIR =
          createTestOutputPath(MahoutPaths.Mahout.name(), MahoutPaths.AlsFactorization.name());
  private static final Path ALS_RECOMMENDATIONS_DIR =
          createTestOutputPath(MahoutPaths.Mahout.name(), MahoutPaths.AlsRecommendations.name());

  private ItemRecommender itemRecommender;

  @Before
  public void setupTest() throws Throwable {
    setup();
    try {
      FileSystem fs = FileSystem.get(new Configuration());
      fs.delete(MAHOUT_OUTPUT_DIR, true);
      itemRecommender = new ItemRecommender(INPUT_DIR_PATH_STR, ALS_FACTORIZATION_OUTPUT_DIR.toString(),
              ALS_RECOMMENDATIONS_DIR.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final Predicate<String> TEST_OUTPUT_FORMAT = new Predicate<String>() {
    private final Pattern p = Pattern.compile("^\\d+\\s\\[\\d+:\\d+\\.\\d+\\]$");
    @Override
    public boolean apply(String input) {
      return p.matcher(input).matches();
    }
  };

  @Test
  public void testPetStorePipeline() throws Exception {
    itemRecommender.recommend();
    ITUtils.assertOutput(ALS_RECOMMENDATIONS_DIR, TEST_OUTPUT_FORMAT);
  }

}
