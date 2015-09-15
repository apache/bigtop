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
package org.apache.bigtop.datagenerators.weatherman;

import java.util.List;

import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.weatherman.internal.WeatherParametersReader;
import org.apache.bigtop.datagenerators.weatherman.internal.WeatherSamplerBuilder;
import org.apache.bigtop.datagenerators.weatherman.internal.WeatherStationParameters;
import org.joda.time.LocalDate;

/**
 * Generates daily weather records for a given location.
 *
 * @author rnowling
 *
 */
public class WeatherGenerator implements Sampler<WeatherRecord> {
  private final Sampler<WeatherRecord> weatherSampler;

  /**
   * Initializes the generator.
   *
   * @param startDate
   *          - first day of simulation
   * @param location
   *          - location to simulate weather for
   * @param seedFactory
   *          - for initializing seeds
   * @throws Exception
   *           - if weather parameter data cannot be read
   */
  public WeatherGenerator(LocalDate startDate, Location location,
          SeedFactory seedFactory) throws Exception {
    List<WeatherStationParameters> parameters = new WeatherParametersReader()
            .readParameters();
    WeatherSamplerBuilder builder = new WeatherSamplerBuilder(parameters,
            location, startDate, seedFactory);
    weatherSampler = builder.build();
  }

  /**
   * Generates a daily WeatherRecord for consecutive days, one day per call.
   *
   * @return Weather data for a single day
   * @throws Exception
   */
  public WeatherRecord sample() throws Exception {
    return weatherSampler.sample();
  }
}
