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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

public class MarkovModelBuilder<S>
{
	ImmutableTable.Builder<S, S, Double> transitionWeights;
	ImmutableMap.Builder<S, Double> startWeights;

	public MarkovModelBuilder()
	{
		transitionWeights = ImmutableTable.builder();
		startWeights = ImmutableMap.builder();
	}

	public static <T> MarkovModelBuilder<T> create()
	{
		return new MarkovModelBuilder<T>();
	}

	public void addStartState(S state, double weight)
	{
		startWeights.put(state, weight);
	}

	public void addTransition(S state1, S state2, double weight)
	{
		transitionWeights.put(state1, state2, weight);
	}

	public MarkovModel<S> build()
	{
		return new MarkovModel<S>(transitionWeights.build().rowMap(), startWeights.build());
	}


}
