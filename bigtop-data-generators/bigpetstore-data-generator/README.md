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
BigPetStore Data Generator
==========================

Library for simulating customer purchasing behavior at a fictional chain of petstores for the purpose of generating synthetic transaction data.

Building and Testing
--------------------
The data generator is part of a Gradle multiproject build.  Please see the README in the parent directory for build and test instructions.


Running the Data Generator
--------------------------
The data generator can be used as a library (for incorporating in
Hadoop or Spark applications) or using a command-line interface.
The data generator CLI requires several parameters.  To get 
descriptions:

    $ java -jar build/libs/bigpetstore-data-generator-1.1.0-SNAPSHOT.jar

Here is an example for generating 10 stores, 1000 customers, 100 purchasing models,
and a year of transactions:

    $ java -jar build/libs/bigpetstore-data-generator-1.1.0-SNAPSHOT.jar generatedData/ 10 1000 100 365.0


Groovy Drivers for Scripting
----------------------------
Several Groovy example script drivers are included in the `groovy_example_drivers` directory.
Groovy scripts can be used to easily call and interact with classes in the data generator
jar without having to create separate Java projects or worry about compilation.  I've found
them to be very useful for interactive exploration and validating my implementations
when unit tests alone aren't sufficient.

To use Groovy scripts, you will need to have Groovy installed on your system.  Build the 
data generator as instructed above.  Then run the scripts in the `groovy_example_drivers`
directory as so:

    $ groovy -classpath ../build/libs/bigpetstore-data-generator-1.1.0-SNAPSHOT.jar MonteCarloExponentialSamplingExample.groovy

