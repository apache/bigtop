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
package org.apache.bigtop.bigpetstore.datagenerator;

import java.io.File;
import java.util.List;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;

import com.google.common.collect.ImmutableList;

public class Constants
{
	public static enum PurchasingModelType
	{
		STATIC,
		DYNAMIC;
	}
	
	public static enum DistributionType
	{
		BOUNDED_MULTIMODAL_GAUSSIAN,
		EXPONENTIAL;
	}
	
	public static final File COORDINATES_FILE = new File("zips.csv");
	public static final File INCOMES_FILE = new File("ACS_12_5YR_S1903/ACS_12_5YR_S1903_with_ann.csv");
	public static final File POPULATION_FILE = new File("population_data.csv");
	
	public static final File NAMEDB_FILE = new File("namedb/data/data.dat");
	
	public static final String PRODUCTS_COLLECTION = "small";
	
	public static final double INCOME_SCALING_FACTOR = 100.0;
	
	public static final int MIN_PETS = 1;
	public static final int MAX_PETS = 10;
	
	public static final List<Pair<Double, Double>> TRANSACTION_TRIGGER_TIME_GAUSSIANS = ImmutableList.of(Pair.create(5.0, 2.0));
	public static final List<Pair<Double, Double>> PURCHASE_TRIGGER_TIME_GAUSSIANS = ImmutableList.of(Pair.create(10.0, 4.0));
	
	public static final double TRANSACTION_TRIGGER_TIME_MAX = 10.0;
	public static final double TRANSACTION_TRIGGER_TIME_MIN = 1.0;
	
	public static final double PURCHASE_TRIGGER_TIME_MAX = 20.0;
	public static final double PURCHASE_TRIGGER_TIME_MIN = 1.0;
	
	public static final double AVERAGE_CUSTOMER_STORE_DISTANCE = 5.0; // miles
	
	public static final PurchasingModelType PURCHASING_MODEL_TYPE = PurchasingModelType.DYNAMIC;
	
	public static final List<Pair<Double, Double>> PRODUCT_MSM_FIELD_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.create(0.15, 0.1), Pair.create(0.85, 0.1));
	public static final double PRODUCT_MSM_FIELD_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_FIELD_WEIGHT_UPPERBOUND = 0.95;
	
	public static final List<Pair<Double, Double>> PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.create(0.15, 0.1), Pair.create(0.85, 0.1));
	public static final double PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_UPPERBOUND = 0.95;
	
	public static final List<Pair<Double, Double>> PRODUCT_MSM_LOOPBACK_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.create(0.25, 0.1), Pair.create(0.75, 0.1));
	public static final double PRODUCT_MSM_LOOPBACK_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_LOOPBACK_WEIGHT_UPPERBOUND = 0.95;
	
	public static final DistributionType STATIC_PURCHASING_MODEL_FIELD_WEIGHT_DISTRIBUTION_TYPE = DistributionType.BOUNDED_MULTIMODAL_GAUSSIAN;
	public static final DistributionType STATIC_PURCHASING_MODEL_FIELD_VALUE_WEIGHT_DISTRIBUTION_TYPE = DistributionType.EXPONENTIAL;
	
	public static final List<Pair<Double, Double>> STATIC_FIELD_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.create(0.15, 0.1), Pair.create(0.85, 0.1));
	public static final double STATIC_FIELD_WEIGHT_LOWERBOUND = 0.05;
	public static final double STATIC_FIELD_WEIGHT_UPPERBOUND = 0.95;
	
	public static final List<Pair<Double, Double>> STATIC_FIELD_VALUE_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.create(0.15, 0.1), Pair.create(0.85, 0.1));
	public static final double STATIC_FIELD_VALUE_WEIGHT_LOWERBOUND = 0.05;
	public static final double STATIC_FIELD_VALUE_WEIGHT_UPPERBOUND = 0.95;
	
	public static final double STATIC_FIELD_WEIGHT_EXPONENTIAL = 0.25;
	public static final double STATIC_FIELD_VALUE_WEIGHT_EXPONENTIAL = 2.0;
	
	
	public static final String PRODUCT_QUANTITY = "quantity";
	public static final String PRODUCT_CATEGORY = "category";
	public static final String PRODUCT_UNIT_PRICE = "unitPrice";
	public static final String PRODUCT_PRICE = "price";
	
	public static final double STOP_CATEGORY_WEIGHT = 0.01;
}
