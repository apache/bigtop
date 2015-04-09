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

import static org.apache.bigtop.bigpetstore.ITUtils.BPS_TEST_GENERATED;
import static org.apache.bigtop.bigpetstore.ITUtils.BPS_TEST_PIG_CLEANED_ROOT;
import static org.apache.bigtop.bigpetstore.ITUtils.fs;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.bigtop.bigpetstore.etl.PigCSVCleaner;
import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.pig.ExecType;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

/**
 * This is the main integration test for pig. Like all BPS integration tests, it
 * is designed to simulate exactly what will happen on the actual cluster,
 * except with a small amount of records.
 *
 * In addition to cleaning the dataset, it also runs the BPS_analytics.pig
 * script which BigPetStore ships with.
 */
public class BigPetStorePigIT {

	final static Logger log = LoggerFactory.getLogger(BigPetStorePigIT.class);

	/**
	 * An extra unsupported code path that we have so people can do ad hoc
	 * analytics on pig data after it is cleaned.
	 */
	public static final Path BPS_TEST_PIG_COUNT_PRODUCTS = fs
			.makeQualified(new Path("bps_integration_",
					BigPetStoreConstants.OUTPUTS.pig_ad_hoc_script.name() + "0"));

	static final File PIG_SCRIPT = new File("BPS_analytics.pig");

	static {
		if (!PIG_SCRIPT.exists()) {
			throw new RuntimeException("Couldnt find pig script at " + PIG_SCRIPT.getAbsolutePath());
		}
	}

	@Before
	public void setupTest() throws Throwable {
		ITUtils.setup();
		try {
			FileSystem.get(new Configuration()).delete(BPS_TEST_PIG_CLEANED_ROOT, true);
			FileSystem.get(new Configuration()).delete(BPS_TEST_PIG_COUNT_PRODUCTS, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static Map<Path, Predicate<String>> TESTS = ImmutableMap.of(
		/** Test of the main output */
		new Path(BPS_TEST_PIG_CLEANED_ROOT,"tsv"), ITUtils.VERIFICATION_PERDICATE,
		// Example of how to count products after doing basic pig data cleanup
		BPS_TEST_PIG_COUNT_PRODUCTS, ITUtils.VERIFICATION_PERDICATE,
		// Test the output that is to be used as an input for Mahout.
		BigPetStoreMahoutIT.INPUT_DIR_PATH, ITUtils.VERIFICATION_PERDICATE
	);

	@Test
	public void testPetStoreCorePipeline() throws Exception {
		//outputs multiple data sets, Mahout, tsv, ...	under BPS_TEST_PIG_CLEANED_ROOT
		runPig(BPS_TEST_GENERATED, BPS_TEST_PIG_CLEANED_ROOT, PIG_SCRIPT);
		for (Entry<Path, Predicate<String>> e : TESTS.entrySet()) {
			ITUtils.assertOutput(e.getKey(), e.getValue());
		}
	}

	private void runPig(Path input, Path output, File pigscript)
			throws Exception {
		new PigCSVCleaner(input, output, ExecType.LOCAL, pigscript);
	}
}
