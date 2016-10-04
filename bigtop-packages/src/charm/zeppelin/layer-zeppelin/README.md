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
## Overview

Apache Zeppelin is a web-based notebook that enables interactive data analytics.
You can make beautiful data-driven, interactive, and collaborative documents
with SQL, Scala and more.

As a Multi-purpose Notebook, Apache Zeppelin is the place for interactive:

 * Data Ingestion
 * Data Discovery
 * Data Analytics
 * Data Visualization & Collaboration


## Usage

This charm is intended to be deployed alongside one of the
[big data bundles](https://jujucharms.com/u/bigdata-charmers/#bundles).
For example:

    juju deploy hadoop-processing

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju quickstart cs:bundle/hadoop-processing`._

Now add Zeppelin and expose the web interface:

    juju deploy zeppelin
    juju expose zeppelin

Now relate Zeppelin to Java and the Hadoop cluster:

    juju add-relation zeppelin openjdk
    juju add-relation zeppelin plugin

Once deployment is complete, you will have an Apache Bigtop Hadoop platform
with Zeppelin available to create notebooks and start analyzing your data!
You can access the web interface at http://{zeppelin_unit_ip_address}:9080.
The ip address can be found by running`juju status zeppelin | grep public-address`.


## Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that Zeppelin is working as expected using the built-in `smoke-test`
action:

    juju run-action zeppelin/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do zeppelin/0 smoke-test`._

After a minute or so, you can check the results of the smoke test:

    juju show-action-status

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`._

You will see `status: completed` if the smoke test was successful, or
`status: failed` if it was not.  You can get more information on why it failed
via:

    juju show-action-output <action-id>

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`._


## Limitations

### Spark Interpreter Settings

Zeppelin Spark interpreter configuration is set according to environment
variable values at deploy time. If you alter these variables post
deployment (e.g., `juju set-config spark spark_execution_mode=NEW_VALUE`), you will
need to edit Zeppelin's Spark interpreter to match the new value. Do this on
the `Interpreter` tab of the Zeppelin web interface.

 * Affected Spark Interpreter configuration includes:

   * spark.executor.memory


## Contact Information

- <bigdata@lists.ubuntu.com>


## Help

- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
