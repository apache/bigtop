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
package org.apache.bigtop.datagenerators.samplers.markovmodels;

import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModel;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModelBuilder;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.matchers.JUnitMatchers.*;

public class TestMarkovModelBuilder
{

	@Test
	public void testAddStateState()
	{
		MarkovModelBuilder<String> builder = MarkovModelBuilder.create();

		builder.addStartState("a", 1.0);

		MarkovModel<String> msm = builder.build();

		assertThat(msm.getStartWeights().keySet(), hasItem("a"));
		assertEquals((double) msm.getStartWeights().get("a"), (double) 1.0, 0.000001);

	}

	@Test
	public void testAddEdgeTransition()
	{
		MarkovModelBuilder<String> builder = MarkovModelBuilder.create();

		builder.addTransition("a", "b", 1.0);

		MarkovModel<String> msm = builder.build();

		assertThat(msm.getTransitionWeights().keySet(), hasItem("a"));
		assertThat(msm.getTransitionWeights().get("a").keySet(), hasItem("b"));
		assertEquals((double) msm.getTransitionWeights().get("a").get("b"), (double) 1.0, 0.000001);
	}

	@Test
	public void testBuildMSM()
	{
		MarkovModelBuilder<String> builder = MarkovModelBuilder.create();

		builder.addStartState("a", 1.0);
		builder.addTransition("a", "b", 1.0);
		builder.addTransition("a", "c", 1.0);

		MarkovModel<String> msm = builder.build();

		assertThat(msm.getStartWeights().keySet(), hasItem("a"));
		assertThat(msm.getTransitionWeights().keySet(), hasItem("a"));
		assertThat(msm.getTransitionWeights().get("a").keySet(), hasItem("b"));
		assertThat(msm.getTransitionWeights().get("a").keySet(), hasItem("c"));
		assertEquals((double) msm.getTransitionWeights().get("a").get("b"), (double) 1.0, 0.000001);
		assertEquals((double) msm.getTransitionWeights().get("a").get("c"), (double) 1.0, 0.000001);
	}

}
