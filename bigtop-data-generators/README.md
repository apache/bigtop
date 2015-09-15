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
BigTop Data Generators
======================

A collection of synthetic data generators and supporting libraries
for building blueprints, smoke testing, and timing operations.

The following data generators are included so far:

* BigPetStore -- generates customers, stores, products, and transactions for a fictional chain of pet stores
* BigTop Name Generator -- generates names by sampling from U.S. Census data
* BigTop Weatherman -- weather simulator

We have the following libraries:

* BigTop Samplers -- collection of samplers, PDFs, and weight function interfaces and implementations

Building
--------
To simplify dependency management, Gradle's multi-project support is used.  Each project
defines its dependencies in terms of other projects and a holistic `settings.gradle` file
is provided to make building easy.  Just run the following from the `bigtop-data-generators`
directory:

    $ gradle build

The resulting jar files will be located under `build/libs` directory of each project.

Jar files can be installed to a local Maven cache to simplify integration by external projects:

    $ gradle install

You can then define the dependencies via Maven.

Running
-------
Please see READMEs in individual project directories.
