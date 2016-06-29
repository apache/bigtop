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

Apache Hive is a data warehouse infrastructure built on top of Hadoop that
supports data summarization, query, and analysis. Hive provides an SQL-like
language called HiveQL that transparently converts queries to MapReduce for
execution on large datasets stored in Hadoop's HDFS. Learn more at
[hive.apache.org](http://hive.apache.org).

This charm deploys the Hive component of the Apache Bigtop platform and
provides the Hive command line interface and the HiveServer2 service.


## Usage

This charm is intended to be deployed along with the
[hadoop-processing](https://jujucharms.com/hadoop-processing/) bundle:

    juju deploy hadoop-processing

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju quickstart cs:bundle/hadoop-processing`._

Now add Hive:

    juju deploy hive

Now relate Hive to Java and the Hadoop cluster:

    juju add-relation hive openjdk
    juju add-relation hive plugin

Once deployment is complete, you will have an Apache Bigtop Hadoop platform
with Apache Hive available to analyze your data with SQL-like queries.


## Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that Hive is working as expected using the built-in `smoke-test`
action:

    juju run-action hive/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do hive/0 smoke-test`._

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


## Considerations

This charm will deploy Hive with an embedded metastore_db by default. This
is suitable for unit or smoke testing Hive, but this configuration should not
be used in production. The embedded database only allows for one user to
connect and interact with Hive. For this reason, it is recommended you deploy
a database as the backing store for Hive.

If you have deployed Hive without an external database, you can add one
post-deployment with the following:

    juju deploy mariadb
    juju add-relation hive mariadb


## Contact Information

- <bigdata@lists.ubuntu.com>


## Help

- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
