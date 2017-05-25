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
# Overview

Apache Hive is a data warehouse infrastructure built on top of Hadoop that
supports data summarization, query, and analysis. Hive provides an SQL-like
language called HiveQL that transparently converts queries to MapReduce for
execution on large datasets stored in Hadoop's HDFS. Learn more at
[hive.apache.org][].

This charm deploys version 1.2.1 of the Hive component from [Apache Bigtop][].

[hive.apache.org]: http://hive.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

This charm is intended to be deployed via one of the [apache bigtop bundles][].
For example:

    juju deploy hadoop-processing

This will deploy an Apache Bigtop Hadoop cluster. More information about this
deployment can be found in the [bundle readme](https://jujucharms.com/hadoop-processing/).

Now add Hive and relate it to the cluster via the hadoop-plugin:

    juju deploy hive
    juju add-relation hive plugin

## Metastore
This charm will start the Hive Metastore service using a local Apache Derby
metastore database by default. This is suitable for unit or smoke testing Hive,
but this configuration should not be used in production. Deploying an external
database as the Hive metastore is recommended:

    juju deploy mariadb
    juju add-relation hive mariadb

## HBase Integration
This charm supports interacting with HBase tables using Hive. Enable this by
relating Hive to a deployed HBase charm:

    juju add-relation hive hbase

See the [hadoop-hbase][] bundle for an example HBase deployment.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[apache bigtop bundles]: https://jujucharms.com/u/bigdata-charmers/#bundles
[Configuring Models]: https://jujucharms.com/docs/stable/models-config
[hadoop-hbase]: https://jujucharms.com/hadoop-hbase/


# Verifying

## Status
Apache Bigtop charms provide extended status reporting to indicate when they
are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 2 juju status

The message column will provide information about a given unit's state.
This charm is ready for use once the status message indicates that it is
ready.

## Smoke Test
This charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. Run the action as follows:

    juju run-action hive/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>


# Using

Once the deployment has been verified, Apache Hive is ready to execute HiveQL
queries via the command line or thrift interfaces:

## Command Line

    $ juju ssh hive/0
    $ hive
    ...
    hive> create table foo(col1 int, col2 string);
    OK
    Time taken: 0.381 seconds
    hive> show tables;
    OK
    foo
    hivesmoke
    Time taken: 0.202 seconds, Fetched: 2 row(s)
    hive> exit;

## HBase
As mentioned in the **Deploying** section, this charm supports integration
with HBase. When HBase is deployed and related to Hive, use the Hive cli to
interact with HBase tables:

    $ juju ssh hive/0
    $ hive
    ...
    hive> CREATE TABLE myhivetable(key STRING, mycol STRING)
    STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
    WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,cf:mycol')
    TBLPROPERTIES ('hbase.table.name' = 'myhbasetable');
    OK
    Time taken: 2.497 seconds
    hive> DESCRIBE myhivetable;
    OK
    key                 	string              	from deserializer
    mycol               	string              	from deserializer
    Time taken: 0.174 seconds, Fetched: 2 row(s)

## Thrift
The HiveServer2 service provides a thrift server that can be used by Hive
clients. To access it, find the `PUBLIC-ADDRESS` of the hive unit and expose
the application:

    juju status hive
    juju expose hive

External clients will be able to access Hive using:

    thrift://HIVE_PUBLIC_IP:10000


# Configuring

Charm configuration can be changed at runtime with `juju config`. This charm
supports the following config parameters.

## Heap
The default heap size for the the Hive shell JVM is 1024MB. Set a different
value (in MB) with the following:

    juju config hbase heap=4096


# Issues

Apache Bigtop tracks issues using JIRA (Apache account required). File an
issue for this charm at:

https://issues.apache.org/jira/secure/CreateIssue!default.jspa

Ensure `Bigtop` is selected as the project. Typically, charm issues are filed
in the `deployment` component with the latest stable release selected as the
affected version. Any uncertain fields may be left blank.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Hive home page](http://hive.apache.org/)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jujucharms.com/big-data)
- [Juju Bigtop charms](https://jujucharms.com/q/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
