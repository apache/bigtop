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
package org.apache.bigtop.datagenerators.samplers.pdfs;

import java.util.Map;
import java.util.Set;

import org.apache.bigtop.datagenerators.samplers.pdfs.MultinomialPDF;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class TestMultinomialPDF
{

	@Test
	public void testToString()
	{
		Map<String, Double> objects = ImmutableMap.of("A", 0.1, "B", 0.3, "C", 0.5);
		MultinomialPDF<String> pdf = new MultinomialPDF<String>(objects);
		String string = pdf.toString();
		Set<String> observed = Sets.newHashSet(string.split("\n"));
		Set<String> expected = Sets.newHashSet("0.1,A", "0.3,B", "0.5,C");

		Assert.assertEquals(expected, observed);
	}
}
