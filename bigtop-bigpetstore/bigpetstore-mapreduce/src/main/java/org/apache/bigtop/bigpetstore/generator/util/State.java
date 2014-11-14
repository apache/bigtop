/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.bigpetstore.generator.util;

import java.util.Random;


/**
 * Each "state" has a pet store , with a certain "proportion" of the
 * transactions.
 */
public enum State {
  // Each state is associated with a relative probability.
  AZ(.1f),
  AK(.1f),
  CT(.1f),
  OK(.1f),
  CO(.1f),
  CA(.3f),
  NY(.2f);

  public static Random rand = new Random();
  public float probability;

  private State(float probability) {
    this.probability = probability;
  }
}
