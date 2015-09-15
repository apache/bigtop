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
BigTop Weatherman
=================

Library for generating weather on a zipcode-by-zipcode basis.

Building and Testing
--------------------
This project is part of a multiproject Gradle build.  Please see directions in the parent directory for build instructions.

Running
-------
BigTop Weatherman is primarily designed for consumption as a library but it also provides a CLI interface in the jar.  The jar can be executed as follows:

    $ java -jar build/libs/bigtop-weatherman-1.1.0-SNAPSHOT.jar outputDir zipcode simulationLength startDate seed

For example, simulating the weather in South Bend, IN 46617:

    $ java -jar build/libs/bigtop-weatherman-1.1.0-SNAPSHOT.jar output/ 46617 365 2014-04-05 1234

will produce a file `output/46617.txt` containing simulated daily temperature, wind chill, wind speed, total precipitation, rainfall, and snowfall readings.
