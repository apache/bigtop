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
package org.apache.bigtop.bigpetstore.datagenerator.datareaders;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.PetSpecies;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategoryBuilder;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

public class ProductsReader
{
	InputStream path;
	
	public ProductsReader(InputStream path)
	{
		this.path = path;
	}
	
	protected Product parseProduct(Object productJson)
	{
		Map<String, Object> fields = (Map<String, Object>) productJson;
		Product product = new Product(fields);
		return product;
	}
	
	protected ProductCategory parseProductCategory(Object productCategoryObject) throws Exception
	{
		Map<String, Object> jsonProductCategory = (Map<String, Object>) productCategoryObject;

		ProductCategoryBuilder builder = new ProductCategoryBuilder();
		
		for(Map.Entry<String, Object> entry : jsonProductCategory.entrySet())
		{
			Object key = entry.getKey();
			Object value = entry.getValue();

			if(key.equals("category"))
			{
				builder.setCategory( (String) entry.getValue());
			}
			else if(key.equals("species"))
			{
				for(String species : (List<String>) value)
				{
					if(species.equals("dog"))
					{
						builder.addApplicableSpecies(PetSpecies.DOG);
					}
					else if(species.equals("cat"))
					{
						builder.addApplicableSpecies(PetSpecies.CAT);
					}
					else
					{
						throw new Exception("Invalid species " + species + " encountered when parsing product categories JSON.");
					}
				}
			}
			else if(key.equals("trigger_transaction"))
			{
				builder.setTriggerTransaction((Boolean) entry.getValue()); 
			}
			else if(key.equals("fields"))
			{
				for(String fieldName : (List<String>) value)
				{
					builder.addFieldName(fieldName);
				}
			}
			else if(key.equals("daily_usage_rate"))
			{
				builder.setDailyUsageRate((Double) value);
			}
			else if(key.equals("base_amount_used_average"))
			{
				builder.setAmountUsedPetPetAverage((Double) value);
			}
			else if(key.equals("base_amount_used_variance"))
			{
				builder.setAmountUsedPetPetVariance((Double) value);
			}
			else if(key.equals("transaction_trigger_rate"))
			{
				builder.setTriggerTransactionRate((Double) value);
			}
			else if(key.equals("transaction_purchase_rate"))
			{
				builder.setTriggerPurchaseRate((Double) value);
			}
			else if(key.equals("items"))
			{
				for(Object productJson : (List<Object>) value)
				{
					Product product = parseProduct(productJson);
					builder.addProduct(product);
				}
			}
			else
			{
				throw new Exception("Invalid field " + key + " encountered when parsing product categories JSON.");
			}
			
		}
		
		return builder.build();
	}
	
	public List<ProductCategory> readData() throws Exception
	{
		Gson gson = new Gson();
		
		Reader reader = new InputStreamReader(path);
		Object json = gson.fromJson(reader, Object.class);
		
		List<Object> productCategoryObjects = (List<Object>) json;

		List<ProductCategory> productCategories = Lists.newArrayList();
		
		for(Object obj : productCategoryObjects)
		{
			ProductCategory productCategory = parseProductCategory(obj);
			productCategories.add(productCategory);
		}
		
		reader.close();
		
		return productCategories;
		
	}
}
