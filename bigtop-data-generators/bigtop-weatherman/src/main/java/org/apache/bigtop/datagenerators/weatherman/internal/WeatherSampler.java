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

import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.weatherman.WeatherRecord;
import org.joda.time.LocalDate;

public class WeatherSampler implements Sampler<WeatherRecord> {
  private final ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> tempSampler;
  private final ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> windSpeedSampler;
  private final ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> precipitationSampler;
  private LocalDate date;

  public WeatherSampler(LocalDate startDate,
          ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> tempSampler,
          ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> windSpeedSampler,
          ConditionalSampler<WeatherRecordBuilder, WeatherRecordBuilder> precipitationSampler) {
    this.tempSampler = tempSampler;
    this.windSpeedSampler = windSpeedSampler;
    this.precipitationSampler = precipitationSampler;
    date = startDate;
  }

  public WeatherRecord sample() throws Exception {
    WeatherRecordBuilder weatherRecord = new WeatherRecordBuilder(date);

    weatherRecord = tempSampler.sample(weatherRecord);
    weatherRecord = windSpeedSampler.sample(weatherRecord);
    weatherRecord = precipitationSampler.sample(weatherRecord);

    date = date.plusDays(WeatherConstants.WEATHER_TIMESTEP);

    return weatherRecord.build();
  }
}
