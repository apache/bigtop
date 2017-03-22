Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Test suite to validate Hadoop basic specifications
==================================================

The test suite is intended to be used as a validation tool to make sure that a
Hadoop stack derived from Apache Bigtop is still compliant with it. The
minimalistic way of doing so would be to guarantee compatibility of the
environment, binaries layouts, certain configuration parameters, and so on.

Validation test suite for the specs is vaguely based on Apache Bigtop iTest and
consists of two essential parts: a configuration file, communicating the 
functional commands and expected outcome(s) of it; and the test driver to run
the commands and compare the results.
 
Running the tests
=================

Tests could be executed by running the following command 
```
  gradle :bigtop-tests:spec-tests:runtime:test -Pspec.tests --info
```
=======
consists of two essential parts: a configuration file, communicating the
functional commands and expected outcome(s) of it; and the test driver to run
the commands and compare the results.

Running the tests
=================

Tests could be executed by running the following command
```
  gradle :bigtop-tests:spec-tests:runtime:test -Pspec.tests --info
```

