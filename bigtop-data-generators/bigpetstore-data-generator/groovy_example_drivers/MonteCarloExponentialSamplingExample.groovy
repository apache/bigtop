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
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.MonteCarloSampler
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.ExponentialPDF
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.UniformSampler
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory

averageValue = 2.0

seedFactory = new SeedFactory()
uniformSampler = new UniformSampler(0.0, 100.0, seedFactory)
pdf = new ExponentialPDF(1.0 / averageValue)


mcSampler = new MonteCarloSampler(uniformSampler, pdf, seedFactory)

sample = mcSampler.sample()

println("Sampled the value: " + sample)

sampleSum = 0.0
for(int i = 0; i < 10000; i++)
{
	sampleSum += mcSampler.sample()
}

sampleAverage = sampleSum / 10000.0

println("Expected Average: " + averageValue)
println("Observed Average: " + sampleAverage)

