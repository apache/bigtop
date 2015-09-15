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

import java.util.Collection;

import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.weatherman.WeatherRecord;
import org.joda.time.LocalDate;

public class WeatherSamplerBuilder {

  private final WeatherStationParameters parameters;
  private final SeedFactory seedFactory;
  private final LocalDate startDate;

  public WeatherSamplerBuilder(
          Collection<WeatherStationParameters> weatherParameters,
          Location location, LocalDate startDate, SeedFactory seedFactory) {
    parameters = findClosest(weatherParameters, location);
    this.seedFactory = seedFactory;
    this.startDate = startDate;
  }

  private WeatherStationParameters findClosest(
          Collection<WeatherStationParameters> weatherParameters,
          Location location) {
    WeatherStationParameters closestStation = null;
    double minDist = Double.MAX_VALUE;

    for (WeatherStationParameters parameters : weatherParameters) {
      double dist = location.distance(parameters.getCoordinates());
      if (dist < minDist) {
        minDist = dist;
        closestStation = parameters;
      }
    }

    return closestStation;
  }

  private ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> buildTempSampler() {
    return new TemperatureSampler(startDate, parameters.getTemperatureAverage(),
            parameters.getTemperatureRealCoeff(),
            parameters.getTemperatureImagCoeff(),
            parameters.getTemperatureDerivStd(), seedFactory);
  }

  private ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> buildWindSpeedSampler() {
    return new WindSpeedSampler(parameters.getWindSpeedRealCoeff(),
            parameters.getWindSpeedImagCoeff(), parameters.getWindSpeedK(),
            parameters.getWindSpeedTheta(), seedFactory);
  }

  private ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> buildPrecipitationSampler() {
    return new PrecipitationSampler(parameters.getPrecipitationAverage(),
            seedFactory);
  }

  public Sampler<WeatherRecord> build() {
    return new WeatherSampler(startDate, buildTempSampler(),
            buildWindSpeedSampler(), buildPrecipitationSampler());
  }
}
