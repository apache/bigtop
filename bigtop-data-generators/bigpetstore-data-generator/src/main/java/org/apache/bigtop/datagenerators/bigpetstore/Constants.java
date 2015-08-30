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
package org.apache.bigtop.datagenerators.bigpetstore;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class Constants
{
	public static enum PurchasingModelType
	{
		MULTINOMIAL,
		MARKOV;
	}

	public static enum ProductsCollectionSize
	{
		SMALL,
		MEDIUM;
	}

	public static final ProductsCollectionSize PRODUCTS_COLLECTION = ProductsCollectionSize.MEDIUM;

	public static final double INCOME_SCALING_FACTOR = 100.0;

	public static final int MIN_PETS = 1;
	public static final int MAX_PETS = 10;

	public static final List<Pair<Double, Double>> TRANSACTION_TRIGGER_TIME_GAUSSIANS = ImmutableList.of(Pair.of(5.0, 2.0));
	public static final List<Pair<Double, Double>> PURCHASE_TRIGGER_TIME_GAUSSIANS = ImmutableList.of(Pair.of(10.0, 4.0));

	public static final double TRANSACTION_TRIGGER_TIME_MAX = 10.0;
	public static final double TRANSACTION_TRIGGER_TIME_MIN = 1.0;

	public static final double PURCHASE_TRIGGER_TIME_MAX = 20.0;
	public static final double PURCHASE_TRIGGER_TIME_MIN = 1.0;

	public static final double AVERAGE_CUSTOMER_STORE_DISTANCE = 5.0; // miles

	public static final PurchasingModelType PURCHASING_MODEL_TYPE = PurchasingModelType.MULTINOMIAL;

	public static final List<Pair<Double, Double>> PRODUCT_MSM_FIELD_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.of(0.15, 0.1), Pair.of(0.85, 0.1));
	public static final double PRODUCT_MSM_FIELD_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_FIELD_WEIGHT_UPPERBOUND = 0.95;

	public static final List<Pair<Double, Double>> PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.of(0.15, 0.1), Pair.of(0.85, 0.1));
	public static final double PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_UPPERBOUND = 0.95;

	public static final List<Pair<Double, Double>> PRODUCT_MSM_LOOPBACK_WEIGHT_GAUSSIANS = ImmutableList.of(Pair.of(0.25, 0.1), Pair.of(0.75, 0.1));
	public static final double PRODUCT_MSM_LOOPBACK_WEIGHT_LOWERBOUND = 0.05;
	public static final double PRODUCT_MSM_LOOPBACK_WEIGHT_UPPERBOUND = 0.95;

	public static final double PRODUCT_MULTINOMIAL_POSITIVE_WEIGHT = 10.0;
	public static final double PRODUCT_MULTINOMIAL_NEUTRAL_WEIGHT = 1.0;
	public static final double PRODUCT_MULTINOMIAL_NEGATIVE_WEIGHT = 0.1;
	public static final int PRODUCT_MULTINOMIAL_POSITIVE_COUNT_MIN = 1;
	public static final double PRODUCT_MULTINOMIAL_POSITIVE_FREQUENCY = 0.1; // 10%

	public static final String PRODUCT_QUANTITY = "quantity";
	public static final String PRODUCT_CATEGORY = "category";
	public static final String PRODUCT_UNIT_PRICE = "unitPrice";
	public static final String PRODUCT_PRICE = "price";

	/*
	 *  Until we have a more intelligent way (e.g., based on range) of dealing with prices,
	 *  let's exclude them.
	 */
	public static final Set<String> PRODUCT_MODEL_EXCLUDED_FIELDS = ImmutableSet.of(PRODUCT_CATEGORY,
			PRODUCT_UNIT_PRICE,
			PRODUCT_PRICE);

	public static final double STOP_CATEGORY_WEIGHT = 0.01;
}
