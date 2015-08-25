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
package org.apache.bigtop.datagenerators.bigpetstore.generators.products.collections;

import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.ProductCategoryBuilder;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.ProductFieldValue;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.AndRule;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.FieldPredicate;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.NotRule;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.OrRule;

import com.google.common.collect.Lists;

public class MediumProductCollection
{
	private ProductCategory createDogFood()
	{
		ProductCategoryBuilder builder = new ProductCategoryBuilder();

		builder.addApplicableSpecies(PetSpecies.DOG);
		builder.setCategory("dry dog food");
		builder.setTriggerTransaction(true);
		builder.setDailyUsageRate(2.0);
		builder.setAmountUsedPetPetAverage(0.25);
		builder.setAmountUsedPetPetVariance(0.1);
		builder.setTriggerTransactionRate(2.0);
		builder.setTriggerPurchaseRate(7.0);
		builder.setBasePrice(2.0);

		builder.addPropertyValues("brand",
				new ProductFieldValue("Wellfed", 0.67, 1.0),
				new ProductFieldValue("Happy Pup", 0.67, 1.0),
				new ProductFieldValue("Dog Days", 1.0, 1.0),
				new ProductFieldValue("Chef Corgi", 0.0, 1.0));

		builder.addPropertyValues("meat",
				new ProductFieldValue("Chicken", 0.0, 1.0),
				new ProductFieldValue("Pork", 0.0, 1.0),
				new ProductFieldValue("Lamb", 0.1, 1.0),
				new ProductFieldValue("Salmon", 0.25, 1.0),
				new ProductFieldValue("Venison", 0.5, 1.0),
				new ProductFieldValue("Rabbit", 0.5, 1.0),
				new ProductFieldValue("Vegetarian", 0.0, 1.0));

		builder.addPropertyValues("grain",
				new ProductFieldValue("Corn", 0.0, 1.0),
				new ProductFieldValue("Potatoes", 0.1, 1.0),
				new ProductFieldValue("Barley", 0.1, 1.0),
				new ProductFieldValue("Rice", 0.0, 1.0),
				new ProductFieldValue("Soy", 0.1, 1.0));

		builder.addPropertyValues("lifestage",
				new ProductFieldValue("Senior", 0.0, 1.0),
				new ProductFieldValue("Puppy", 0.0, 1.0),
				new ProductFieldValue("Adult", 0.0, 1.0));

		builder.addPropertyValues("organic",
				new ProductFieldValue("false", 0.0, 1.0),
				new ProductFieldValue("true", 0.0, 1.1));

		builder.addPropertyValues("quantity",
				new ProductFieldValue(4.5, 0.0, 4.5),
				new ProductFieldValue(15.0, 0.0, 15.0),
				new ProductFieldValue(30.0, 0.0, 30.0));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Chef Corgi"),
				new FieldPredicate("organic", "true")));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Chef Corgi"),
				new FieldPredicate("meat", "Vegetarian")));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Dog Days"),
				new FieldPredicate("organic", "false")));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("grain", "Corn"),
				new OrRule(
						new FieldPredicate("organic", "true"),
						new FieldPredicate("meat", "Venison"),
						new FieldPredicate("meat", "Rabbit"),
						new FieldPredicate("meat", "Lamb"),
						new FieldPredicate("meat", "Salmon"))));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("organic", "true"),
				new FieldPredicate("meat", "Pork")));

		builder.addExclusionRule(new AndRule(
				new NotRule(new FieldPredicate("grain", "Corn")),
				new FieldPredicate("meat", "Pork")));

		builder.addExclusionRule(new AndRule(
				new OrRule(
						new FieldPredicate("brand", "Chef Corgi"),
						new FieldPredicate("brand", "Happy Pup")),
				new OrRule(
						new FieldPredicate("meat", "Rabbit"),
						new FieldPredicate("meat", "Venison"))
						));

		return builder.build();
	}

	private ProductCategory createCatFood()
	{
		ProductCategoryBuilder builder = new ProductCategoryBuilder();

		builder.addApplicableSpecies(PetSpecies.CAT);
		builder.setCategory("dry cat food");
		builder.setTriggerTransaction(true);
		builder.setDailyUsageRate(2.0);
		builder.setAmountUsedPetPetAverage(0.1);
		builder.setAmountUsedPetPetVariance(0.05);
		builder.setTriggerTransactionRate(2.0);
		builder.setTriggerPurchaseRate(7.0);
		builder.setBasePrice(2.14);

		builder.addPropertyValues("brand",
				new ProductFieldValue("Wellfed", 0.67, 1.0),
				new ProductFieldValue("Feisty Feline", 0.72, 1.0),
				new ProductFieldValue("Pretty Cat", 0.0, 1.0));

		builder.addPropertyValues("meat",
				new ProductFieldValue("Tuna", 0.0, 1.0),
				new ProductFieldValue("Chicken", 0.0, 1.0),
				new ProductFieldValue("Turkey", 0.0, 1.0),
				new ProductFieldValue("Salmon", 0.1, 1.0));

		builder.addPropertyValues("lifestyle",
				new ProductFieldValue("Indoor", 0.0, 1.0),
				new ProductFieldValue("Outdoor", 0.0, 1.0),
				new ProductFieldValue("Weight Management", 0.1, 1.0));

		builder.addPropertyValues("lifestage",
				new ProductFieldValue("Senior", 0.0, 1.0),
				new ProductFieldValue("Kitten", 0.0, 1.0),
				new ProductFieldValue("Adult", 0.0, 1.0));

		builder.addPropertyValues("organic",
				new ProductFieldValue("true", 0.0, 1.1),
				new ProductFieldValue("false", 0.0, 1.0));

		builder.addPropertyValues("quantity",
				new ProductFieldValue(7.0, 0.0, 7.0),
				new ProductFieldValue(15.0, 0.0, 15.0));

		builder.addPropertyValues("hairball management",
				new ProductFieldValue("true", 0.1, 1.0),
				new ProductFieldValue("false", 0.0, 1.0));

		builder.addExclusionRule(new AndRule(
					new FieldPredicate("brand", "Pretty Cat"),
					new FieldPredicate("organic", "true")));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Feisty Feline"),
				new FieldPredicate("organic", "false")));

		return builder.build();
	}

	private ProductCategory createKittyLitter()
	{
		ProductCategoryBuilder builder = new ProductCategoryBuilder();

		builder.addApplicableSpecies(PetSpecies.CAT);
		builder.setCategory("kitty litter");
		builder.setTriggerTransaction(true);
		builder.setDailyUsageRate(1.0);
		builder.setAmountUsedPetPetAverage(0.1);
		builder.setAmountUsedPetPetVariance(0.05);
		builder.setTriggerTransactionRate(2.0);
		builder.setTriggerPurchaseRate(7.0);
		builder.setBasePrice(1.43);

		builder.addPropertyValues("brand",
				new ProductFieldValue("Pretty Cat", 0.0, 1.0),
				new ProductFieldValue("Feisty Feline", 0.1, 1.0));

		builder.addPropertyValues("material",
				new ProductFieldValue("clay", 0.0, 1.0),
				new ProductFieldValue("pellets", 0.1, 1.0));

		builder.addPropertyValues("clumping",
				new ProductFieldValue("true", 0.0, 1.0),
				new ProductFieldValue("false", 0.0, 1.0));

		builder.addPropertyValues("odor control",
				new ProductFieldValue("true", 0.1, 1.0),
				new ProductFieldValue("false", 0.0, 1.0));

		builder.addPropertyValues("quantity",
				new ProductFieldValue(7.0, 0.0, 7.0),
				new ProductFieldValue(14.0, 0.0, 14.0),
				new ProductFieldValue(28.0, 0.0, 28.0));

		return builder.build();
	}

	private ProductCategory createPoopBags()
	{
		ProductCategoryBuilder builder = new ProductCategoryBuilder();

		builder.addApplicableSpecies(PetSpecies.DOG);
		builder.setCategory("poop bags");
		builder.setTriggerTransaction(true);
		builder.setDailyUsageRate(2.0);
		builder.setAmountUsedPetPetAverage(1.0);
		builder.setAmountUsedPetPetVariance(0.5);
		builder.setTriggerTransactionRate(2.0);
		builder.setTriggerPurchaseRate(7.0);
		builder.setBasePrice(0.17);

		builder.addPropertyValues("brand",
				new ProductFieldValue("Chef Corgi", 0.0, 1.0),
				new ProductFieldValue("Happy Pup", 0.67, 1.0),
				new ProductFieldValue("Dog Days", 1.0, 1.0));

		builder.addPropertyValues("color",
				new ProductFieldValue("blue", 0.0, 1.0),
				new ProductFieldValue("multicolor (pastels)", 0.0, 1.0),
				new ProductFieldValue("multicolor (solids)", 0.0, 1.0),
				new ProductFieldValue("designs", 0.0, 1.0));

		builder.addPropertyValues("recycled material",
				new ProductFieldValue("false", 0.0, 60.0),
				new ProductFieldValue("true", 0.1, 120.0));

		builder.addPropertyValues("quantity",
				new ProductFieldValue(60.0, 0.0, 60.0),
				new ProductFieldValue(120.0, 0.0, 120.0));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Chef Corgi"),
				new FieldPredicate("recycled material", "true")));

		builder.addExclusionRule(new AndRule(
				new FieldPredicate("brand", "Dog Days"),
				new FieldPredicate("recycled material", "false")));

		return builder.build();
	}

	public List<ProductCategory> generateProductCategory()
	{
		List<ProductCategory> productCategories = Lists.newArrayList();

		productCategories.add(this.createDogFood());
		productCategories.add(this.createCatFood());
		productCategories.add(this.createKittyLitter());
		productCategories.add(this.createPoopBags());

		return productCategories;
	}
}
