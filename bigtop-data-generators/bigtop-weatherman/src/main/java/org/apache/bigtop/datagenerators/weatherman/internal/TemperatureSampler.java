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
import org.apache.bigtop.datagenerators.samplers.samplers.GaussianSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.joda.time.LocalDate;

public class TemperatureSampler implements
        ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> {
  final private Sampler<Double> R;
  final private double average;
  final private double coeffReal;
  final private double coeffImag;

  private LocalDate date;
  private double noise;

  public TemperatureSampler(LocalDate startDate, double tempAverage,
          double tempRealCoeff, double tempImagCoeff, double tempSigma,
          SeedFactory seedFactory) {
    R = new GaussianSampler(0.0, tempSigma, seedFactory);

    this.average = tempAverage;
    this.coeffReal = tempRealCoeff;
    this.coeffImag = tempImagCoeff;

    date = startDate;
    noise = 0.0;
  }

  public WeatherRecordBuilder sample(WeatherRecordBuilder weatherRecord)
          throws Exception {
    double temp = 0.0;
    while (date.isEqual(weatherRecord.getDate())
            || date.isBefore(weatherRecord.getDate())) {
      double dayOfYear = date.getDayOfYear();
      temp = average
              + coeffReal * Math.cos(-2.0 * Math.PI * dayOfYear
                      / WeatherConstants.TEMPERATURE_PERIOD)
              + coeffImag * Math.sin(-2.0 * Math.PI * dayOfYear
                      / WeatherConstants.TEMPERATURE_PERIOD)
              + noise;

      noise += -1.0 * noise * WeatherConstants.TEMPERATURE_GAMMA
              * WeatherConstants.WEATHER_TIMESTEP
              + Math.sqrt(WeatherConstants.WEATHER_TIMESTEP) * R.sample();

      date = date.plusDays(WeatherConstants.WEATHER_TIMESTEP);
    }

    weatherRecord.setTemperature(temp);

    return weatherRecord;
  }
}
