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

import java.math.BigDecimal;
import static org.apache.bigtop.bigpetstore.generator.util.ProductType.*;

public enum Product {
  DOG_FOOD(DOG, 10.50),
  ORGANIC_DOG_FOOD(DOG, 16.99),
  STEEL_LEASH(DOG, 19.99),
  FUZZY_COLLAR(DOG, 24.90),
  LEATHER_COLLAR(DOG, 18.90),
  CHOKE_COLLAR(DOG, 15.50),
  DOG_HOUSE(DOG, 109.99),
  CHEWY_BONE(DOG, 20.10),
  DOG_VEST(DOG, 19.99),
  DOG_SOAP(DOG, 5.45),

  CAT_FOOD(CAT, 7.50),
  FEEDER_BOWL(CAT, 10.99),
  LITTER_BOX(CAT, 24.95),
  CAT_COLLAR(CAT, 7.95),
  CAT_BLANKET(CAT, 14.49),

  TURTLE_PELLETS(TURTLE, 4.95),
  TURTLE_FOOD(TURTLE, 10.90),
  TURTLE_TUB(TURTLE, 40.45),

  FISH_FOOD(FISH, 12.50),
  SALMON_BAIT(FISH, 29.95),
  FISH_BOWL(FISH, 20.99),
  AIR_PUMP(FISH, 13.95),
  FILTER(FISH, 34.95),

  DUCK_COLLAR(DUCK, 13.25),
  DUCK_FOOD(DUCK, 20.25),
  WADING_POOL(DUCK, 45.90);

  /*
  ANTELOPE_COLLAR(OTHER, 19.90),
  ANTELOPE_SNACKS(OTHER, 29.25),
  RODENT_CAGE(OTHER, 39.95),
  HAY_BALE(OTHER, 4.95),
  COW_DUNG(OTHER, 1.95),
  SEAL_SPRAY(OTHER, 24.50),
  SNAKE_BITE_OINTMENT(OTHER, 29.90);
  */
  private final BigDecimal price;
  public final ProductType productType;
  private Product(ProductType productType, double price) {
    this.price = BigDecimal.valueOf(price);
    this.productType = productType;
  }

  public int id() {
    return this.ordinal();
  }

  public BigDecimal price() {
    return this.price;
  }


}
