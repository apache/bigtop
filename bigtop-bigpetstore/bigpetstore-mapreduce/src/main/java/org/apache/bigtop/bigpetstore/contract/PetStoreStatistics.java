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


package org.apache.bigtop.bigpetstore.contract;

import java.util.Map;

/**
 * This is the contract for the web site. This object is created by each ETL
 * tool : Summary stats.
 */
public abstract class PetStoreStatistics {

    public abstract Map<String, ? extends Number> numberOfTransactionsByState()
            throws Exception;

    public abstract Map<String, ? extends Number> numberOfProductsByProduct()
            throws Exception;

}