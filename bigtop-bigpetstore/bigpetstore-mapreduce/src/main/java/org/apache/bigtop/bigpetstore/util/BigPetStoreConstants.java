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
 *
 * Static final constants
 *
 * is useful to have the basic sql here as the HIVE SQL can vary between hive
 * versions if updated here will update everywhere
 */

package org.apache.bigtop.bigpetstore.util;

public class BigPetStoreConstants {

   //Files should be stored in graphviz arch.dot
   public static enum OUTPUTS {
        generated,//generator
        cleaned,//pig
        tsv,
        pig_ad_hoc_script,
        CUSTOMER_PAGE; //crunchhh

        public static enum MahoutPaths {
          Mahout,
          AlsFactorization,
          AlsRecommendations
        }
    };

}
