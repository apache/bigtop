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

HBase is the Hadoop database. Think of it as a distributed scalable Big Data
store.

Use HBase when you need random, realtime read/write access to your Big Data.
This project's goal is the hosting of very large tables -- billions of rows X
millions of columns -- atop clusters of commodity hardware.

HBase is an open-source, distributed, versioned, column-oriented store modeled
after Google's Bigtable: A Distributed Storage System for Structured Data by
Chang et al. Just as Bigtable leverages the distributed data storage provided
by the Google File System, HBase provides Bigtable-like capabilities on top of
Hadoop and HDFS.

HBase provides:

- Linear and modular scalability.
- Strictly consistent reads and writes.
- Automatic and configurable sharding of tables
- Automatic failover support between RegionServers.
- Convenient base classes for backing Hadoop MapReduce jobs with HBase tables.
- Easy to use Java API for client access.
- Block cache and Bloom Filters for real-time queries.
- Query predicate push down via server side Filters
- Thrift gateway and a REST-ful Web service that supports XML, Protobuf,
  and binary data encoding options
- Extensible jruby-based (JIRB) shell
- Support for exporting metrics via the Hadoop metrics subsystem to files
  or Ganglia; or via JMX.

See [the homepage](http://hbase.apache.org) for more information.

This charm provides the hbase master and regionserver roles as delivered by the
Apache Bigtop project.


## Usage

A HBase deployment consists of HBase masters and HBase RegionServers.
In the distributed HBase deployment this charm provides each unit deploys
one master and one regionserver on each unit. HBase makes sure that
only one master is active and the rest are in standby mode in case
the active one fails.

To HBase operates over HDFS so we first need to deploy::

    juju deploy hadoop-namenode namenode
    juju deploy hadoop-slave slave
    juju deploy hadoop-plugin plugin
    juju deploy openjdk

    juju add-relation namenode openjdk
    juju add-relation namenode slave
    juju add-relation plugin namenode
    juju add-relation plugin openjdk
    juju add-relation slave openjdk

In order to function correctly the hbase master and regionserver services
have a mandatory relationship with zookeeper - please use the zookeeper charm
to create a functional zookeeper quorum and then relate it to this charm::
Remember that quorums come in odd numbers start from 3 (but it will work
with one BUT with no resilience).

    juju deploy zookeeper -n 3
    juju add-relation zookeeper openjdk

Now we are ready to deploy HBase scaled to 3 units and add the required relations.

    juju deploy hbase -n 3

    juju add-relation hbase openjdk
    juju add-relation plugin hbase
    juju add-relation zookeeper hbase

The charm also supports use of the thrift gateway.


## Service Restarts

Restarting a HBase deployment is potentially disruptive so you should be aware
what events cause restarts::

- Zookeeper service units joining or departing relations.
- Upgrading the charm or changing the configuration.


## Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that HBase is working as expected using the built-in `smoke-test`
action:

    juju run-action hbase/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do hbase/0 smoke-test`._

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


## Contact Information
- <bigdata@lists.ubuntu.com>


## Help
- [Apache HBase home page](https://hbase.apache.org/)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
