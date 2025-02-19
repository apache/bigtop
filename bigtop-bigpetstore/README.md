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
BigPetStore
============

BigPetStore is a family of example applications for the Hadoop/Spark
ecosystems. BigPetStore generates and analyzes synthetic transaction data for
a fictional chain of petstores.

BigPetStore has the following aims:

* Serve as a demo application to showcase capabilities of the BigTop distribution
* Perform integration testing for BigTop's components
* Server as a template for building / packaging Hadoop/Spark applications
* Provide scalable generation of complex synthetic data
* Examples for using and integrating components
* Examples of how to perform popular analytics tasks

BigPetStore has the following components to date:

* Gradle build systems supporting Java, Scala, and Groovy
* Data generators
* Analytics
  * ETL
  * Item Recommenders

The BigPetStore application was originally developed for MapReduce and associated
components such as Pig, Hive, Mahout, Crunch, etc. With the increasing popularity
and importance of Spark, BigPetStore has been expanded to support Spark. To support
the use case of deploying to pure MapReduce or Spark environments, we've elected to
separate the MapReduce and Spark support into separate applications.
After that, we have dropped Pig, Mahout, and Crunch from our software stack
due to the project inactivity at that time. Therefore the MapReduce version
of BigPetStore has also been dropped and we now have only the Spark version.
For futher documentation, see the `bigpetstore-spark` directory.


