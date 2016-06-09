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

This charm provides the Hive command line interface and the HiveServer2 service.


## Usage

This charm is intended to be deployed via one of the
[big data bundles](https://jujucharms.com/u/bigdata-charmers/#bundles).
For example:

    juju quickstart apache-analytics-sql

This will deploy the Apache Hadoop platform with Apache Hive available to
perform SQL-like queries against your data.


## Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status --format=tabular

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status --format=tabular

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that Hive is working as expected using the built-in `smoke-test`
action:

    juju action do hive/0 smoke-test

After a few seconds or so, you can check the results of the smoke test:

    juju action status

You will see `status: completed` if the smoke test was successful, or
`status: failed` if it was not.  You can get more information on why it failed
via:

    juju action fetch <action-id>


## Considerations

This charm will deploy Hive with an embedded metastore_db by default. This
is suitable for unit or smoke testing Hive, but this configuration should not
be used in production. The embedded database only allows for one user to
connect and interact with Hive. For this reason, it is recommended you deploy
a MySQL database as the backing store for Hive (this is done automatically
when deploying Hive via one of the big data bundles).

If you have deployed Hive without an external database, you can add one
post-deployment with the following:

    juju deploy mysql
    juju add-relation hive mysql


## Contact Information

- <bigdata@lists.ubuntu.com>


## Help

- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
