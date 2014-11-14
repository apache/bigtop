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

package org.apache.bigtop.bigpetstore.util;

import org.apache.bigtop.bigpetstore.generator.util.State;

/**
 * User and Product IDs need numerical
 * identifiers for recommender algorithms
 * which attempt to interpolate new
 * products.
 *
 * TODO: Delete this class. Its not necessarily required: We might just use HIVE HASH() as our
 * standard for this.
 */
public class NumericalIdUtils {

    /**
     * People: Leading with ordinal code for state.
     */
    public static long toId(State state, String name){
        String fromRawData =
                state==null?
                        name:
                         (state.name()+"_"+name);
        return fromRawData.hashCode();
    }
    /**
     * People: Leading with ordinal code for state.
     */
    public static long toId(String name){
        return toId(null,name);
    }
}