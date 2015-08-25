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
BigTop Name Generator
=====================

Library for generating first and last names by sampling from the [Drupal
Name Database](https://www.drupal.org/project/namedb) which itself is based
on U.S. Census data.  Frequency of generates names are in proportion to
their frequencies according to the U.S. Census.

Building and Testing
--------------------
We use the Gradle build system for the BPS data generator so you'll need
to install Gradle on your system.
Once that's done, you can use gradle to run the included unit tests
and build the data generator jar.

To build:

    $ gradle build

This will create several directories and a jar located at:

    build/libs/bigtop-name-generator-1.1.0-SNAPSHOT.jar

Building automatically runs the included unit tests.  If you would prefer
to just run the unit tests, you can do so by:

    $ gradle test

To clean up the build files, run:

    $ gradle clean

To install a jar into your local maven repository:

    $ gradle install
