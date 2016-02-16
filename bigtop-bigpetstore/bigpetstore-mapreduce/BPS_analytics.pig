----------------------------------------------------------------------------
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-----------------------------------------------------------------------------

-- This is the analytics script that BigPetStore uses as an example for
-- demos of how to do ad-hoc analytics on the cleaned transaction data.
-- It is used in conjunction with the big pet store web app, soon to be
-- added to apache bigtop (As of 4/12/2014, the
-- corresponding web app to consume this scripts output is
-- in jayunit100.github.io/bigpetstore).

-- invoke with two arguments, the input file, and the output file. -param input=bps/cleaned -param output=bps/analytics

-- FYI...
-- If you run into errors, you can see them in
-- ./target/failsafe-reports/TEST-org.bigtop.bigpetstore.integration.BigPetStorePigIT.xml

-- First , we load data in from a file, as tuples.
-- in pig, relations like tables in a relational database
-- so each relation is just a bunch of tuples.
-- in this case csvdata will be a relation,
-- where each tuple is a single petstore transaction.
csvdata =
    LOAD '$input' using PigStorage()
        AS (
          dump:chararray,
          state:chararray,
          transaction:int,
          custId:long,
          fname:chararray,
          lname:chararray,
          productId:int,
          product:chararray,
          price:float,
          date:chararray);

-- RESULT:
-- (BigPetStore,storeCode_AK,1,11,jay,guy,3,dog-food,10.5,Thu Dec 18 12:17:10 EST 1969)
-- ...

-- Okay! Now lets group our data so we can do some stats.
-- lets create a new relation,
-- where each tuple will contain all transactions for a product in a state.

state_product = group csvdata by ( state, product ) ;

-- RESULT
-- ((storeCode_AK,dog-food) , {(BigPetStore,storeCode_AK,1,11,jay,guy,3,dog-food,10.5,Thu Dec 18 12:17:10 EST 1969)}) --
-- ...


-- Okay now lets make some summary stats so that the boss man can
-- decide which products are hottest in which states.

-- Note that for the "groups", we tease out each individual field here for formatting with
-- the BigPetStore visualization app.
summary1 = FOREACH state_product generate STRSPLIT(group.state,'_').$1 as sp, group.product, COUNT($1);


-- Okay, the stats look like this.  Lets clean them up.
-- (storeCode_AK,cat-food)      2530
-- (storeCode_AK,dog-food)      2540
-- (storeCode_AK,fuzzy-collar)     2495

dump summary1;

store summary1 into '$output';
