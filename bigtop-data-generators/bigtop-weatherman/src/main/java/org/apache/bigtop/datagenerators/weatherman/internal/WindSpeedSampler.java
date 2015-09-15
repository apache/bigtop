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
import org.apache.bigtop.datagenerators.samplers.samplers.GammaSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class WindSpeedSampler implements
        ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> {
  private final double coeffReal;
  private final double coeffImag;
  private final Sampler<Double> gamma;

  public WindSpeedSampler(double windSpeedRealCoeff, double windSpeedImagCoeff,
          double windSpeedK, double windSpeedTheta, SeedFactory seedFactory) {
    coeffReal = windSpeedRealCoeff;
    coeffImag = windSpeedImagCoeff;

    gamma = new GammaSampler(windSpeedK, windSpeedTheta, seedFactory);

  }

  /**
   *
   * @param temp
   *          - given in Fahrenheit
   * @param windSpeed
   *          - given in MPH
   * @return
   */
  private Double windChill(double temp, double windSpeed) {
    double v_16 = Math.pow(windSpeed, 0.16);
    double windChill = 35.74 + 0.6215 * temp - 35.74 * v_16
            + 0.4275 * temp * v_16;

    return windChill;
  }

  public WeatherRecordBuilder sample(WeatherRecordBuilder weatherRecord)
          throws Exception {

    double dayOfYear = weatherRecord.getDate().getDayOfYear();
    // meters/second
    double windSpeed = Math.max(0.0,
            coeffReal
                    * Math.cos(-2.0 * Math.PI * dayOfYear
                            / WeatherConstants.TEMPERATURE_PERIOD)
            + coeffImag * Math.sin(-2.0 * Math.PI * dayOfYear
                    / WeatherConstants.TEMPERATURE_PERIOD) + gamma.sample());

    double windChill = windChill(weatherRecord.getTemperature(),
            windSpeed * WeatherConstants.M_PER_S_TO_MPH);

    weatherRecord.setWindSpeed(windSpeed);
    weatherRecord.setWindChill(windChill);

    return weatherRecord;
  }
}
