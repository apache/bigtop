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
package org.apache.bigtop.datagenerators.weatherman.internal;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.ExponentialSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class PrecipitationSampler implements
        ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> {
  private final Sampler<Double> precipitationSampler;

  public PrecipitationSampler(double averagePrecipitation,
          SeedFactory seedFactory) {
    precipitationSampler = new ExponentialSampler(1.0 / averagePrecipitation,
            seedFactory);
  }

  public WeatherRecordBuilder sample(WeatherRecordBuilder record)
          throws Exception {
    double temp = record.getTemperature();
    double precipitation = precipitationSampler.sample();
    record.setPrecipitation(precipitation);

    double fractionRain = 1.0
            / (1.0 + Math.exp(-WeatherConstants.PRECIPITATION_A
                    * (temp - WeatherConstants.PRECIPITATION_B)));

    double rainfall = fractionRain * precipitation;
    double snowfall = WeatherConstants.PRECIPITATION_TO_SNOWFALL
            * (1.0 - fractionRain) * precipitation;
    record.setRainFall(rainfall);
    record.setSnowFall(snowfall);

    return record;
  }
}
