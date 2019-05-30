<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Introduction to the BigPetStore transaction queue#

This is a rotating queue for Apache BigTop which uses the BigPetStore libraries to generate realistic streaming transactional data.

There are many ways that data streams are consumed,

- one is Files (files written every n seconds)
- another is via the web (peridoic HTTP PUTs)

Both of these are supported by this library.  It runs infinitely as a single java process, repeatedly re running itself every time a simulation is done.

It is a part of the BigPetStore ecosystem of projects which Apache BigTop leverages for real world validation of the stack.

As a proof of concept, it has been used to test google's kubernetes framework by generating large load against Redis (a in memory caching data layer).

# I'm in a huge rush, how do I use it? #

Just run the dockerfile (eventually these dockerfiles will be stored in bigtop/ on dockerhub).
```
docker run -t -i jayunit100/bigpetstore-load-generator
```

After you run it, you can read below to customize it.

# BigPetStore Data Generator #

See related project on github, jayunit100/PetStoreBook, for example usage.

An infiniterator for bigpetstore !

If you're not familiar with bigpetstore, check out the apache bigtop project, which uses it to process data with spark, and the original whitepaper on the data generator (http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=7034765). This repository writes bigpetstore transactions to

- a rotating *file queue* OR
- a *REST API* calling queue.
- You better have a consumer somewhere, otherwise it will fill up your disk very quickly !
- Its a little raw: I can clean it up later if people start getting interested.

# Get Started by building the jar #
Clone this repository.  Then just run

```
gradle fatJar
```

## Arguments ##

The command line arguments are:

- path (URL, for REST loads or directory on local disk, for file loads)
- stores (total number)
- customers (total number)
- simulation length (in days)
- seed (random seed for generator)

## Generating FileSystem load ##

From docker

```
docker run -t -i jayunit100/bigpetstore-load-generator /tmp/ 1 1 3.0
```
or Java
```
 gradle clean fatJar
 java -jar ./build/libs/bigpetstore-transaction-queue-all-1.0.jar /tmp 1 5 10000 123
```

## Generating REST load ##
OR Replace the file path with a REST API root (it will jsonify the transactions, and send them as the final url portion).

From docker

```
docker run -t -i jayunit100/bigpetstore-load-generator http://localhost:3000/restapi/rpush/ 1 5 10000 123
```
Or from java
```
gradle clean fatJar
java -jar ./build/libs/bigpetstore-transaction-queue-all-1.0.jar http://localhost:3000/rpush/guestbook/ 4 4 1000 123

```

For details, just see the unit tests.

This will write transactions to /tmp/transaction[0..n] every 10 seconds or so, depending on performance of your machine.

For different parameter settings you can try the following.

*Increase timescale, lower customers, for higher throughput, but unrealistic *

```
/tmp 1 5 1500000 13241234
```

*Increase customers, for more diversity*

```
/tmp 1 500000 15000 13241234
```

And so on.

Have fun ! For me, I was able to generate 25k transactions per minute.

# Whats this for, how do I get help ? #

To understand the BigPetStore application, you can go to the apache bigtop user list.  This is based on a data generator
which was published recently http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=7034765.

We also have a parallel Spark based generator,
which is here https://github.com/apache/bigtop/tree/master/bigtop-bigpetstore/bigpetstore-spark.

# OUTPUT #

```
jayunit100smacbookpro:PetStoreLoadGenerator jayunit100java -cp ./build/libs/PetStoreLoadGenerator-1.0.jar:libs/* org.apache.bigtop.qstream.LoadGen
Running default simulation, which should result in 10 to 25K transactions per second on i7 chip w/ SSD
Reading zipcode data
Read 30891 zipcode entries
Reading name data
Read 86987 first names and 47819 last names
Reading product data
Read 4 product categories
Generating stores...
Generating customers...
...Generated 100000
Clearing 31179 elements
WRITE FILE to 6964688bytes -> /tmp/transactions0.txt
TRANSACTIONS SO FAR 31182.0 RATE 577.4629629629629
Clearing 293666 elements
WRITE FILE to 66377212bytes -> /tmp/transactions1.txt
TRANSACTIONS SO FAR 324852.0 RATE 4922.015151515152
Clearing 343637 elements
WRITE FILE to 77805125bytes -> /tmp/transactions2.txt
TRANSACTIONS SO FAR 668492.0 RATE 8681.727272727272
```

And the output files:

```
jayunit100smacbookpro:PetStoreLoadGenerator jayunit100$ cat /tmp/transactions5.txt  | head
851,07936,East Hanover,NJ,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,31,534.6314963516645,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;
454,60447,Minooka,IL,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,30,495.0237724899403,category=dry cat food;brand=Pretty Cat;flavor=Tuna;size=15.0;per_unit_cost=2.86;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;,category=dry dog food;brand=Happy Pup;flavor=Pork;size=30.0;per_unit_cost=2.67;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;
703,37849,Powell,TN,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,29,477.41342172147426,category=dry cat food;brand=Pretty Cat;flavor=Tuna;size=15.0;per_unit_cost=2.86;,category=poop bags;brand=Happy Pup;color=Blue;size=120.0;per_unit_cost=0.17;
576,79938,El Paso,TX,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,28,473.62278493049723,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;,category=dry cat food;brand=Wellfed;flavor=Chicken & Rice;size=14.0;per_unit_cost=2.14;
570,71730,El Dorado,AR,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,27,452.4051824447436,category=poop bags;brand=Happy Pup;color=multicolor;size=60.0;per_unit_cost=0.17;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;,category=dry dog food;brand=Dog Days;flavor=Pork;size=30.0;per_unit_cost=3.0;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;
721,37801,Maryville,TN,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,26,443.25719128990784,category=dry cat food;brand=Wellfed;flavor=Chicken & Rice;size=14.0;per_unit_cost=2.14;
73,77083,Houston,TX,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,25,429.55735733784735,category=poop bags;brand=Happy Pup;color=multicolor;size=60.0;per_unit_cost=0.17;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;
228,08053,Marlton,NJ,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,24,416.1376243980079,category=dry cat food;brand=Feisty Feline;flavor=Tuna;size=7.0;per_unit_cost=2.14;,category=kitty litter;brand=Fiesty Feline;size=7.0;per_unit_cost=1.5;
346,91711,Claremont,CA,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,23,413.9219953662229,category=dry dog food;brand=Dog Dogs;flavor=Chicken;size=30.0;per_unit_cost=3.0;330,37138,Old Hickory,TN,17105,Lofton,Mattheeuw,07722,Colts Neck,NJ,22,392.11323640456675,category=dry cat food;brand=Feisty Feline;flavor=Tuna;size=7.0;per_unit_cost=2.14;

```

# Development and Distribution #

Building the distribution is relatively simple, and you can easily develop it using eclipse / intelliJ, for example, you can run
```
gradle idea
```
and then import it.

To build a distribution , run ```gradle distZip``` followed by ```docker build -t yourname/bps-data-generator ./```.  This will bundle a distribution zip into a Dockerfile which can be pushed.

Also, the distribution zip in and of itself is an artifact which we will release.  More notes to come on that.  For now, since this is quite new, contact the ASF BigTop mailing lists if interested in maintaining or contributing.
