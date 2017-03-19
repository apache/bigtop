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

HBase is the Hadoop database. Think of it as a distributed, scalable Big Data
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

This charm deploys the hbase master and regionserver components of the
[Apache Bigtop][] platform.

[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the [getting-started][] instructions prior to deploying this
charm.

An HBase deployment consists of HBase masters and HBase RegionServers.
In a distributed HBase environment, one master and one regionserver are
deployed on each unit. HBase makes sure that only one master is active and
the rest are in standby mode in case the active one fails.

Because HBase requires HDFS, this charm is recommended to be deployed as part
of the `hadoop-hbase` bundle:

    juju deploy hadoop-hbase

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart][] with the following syntax: `juju quickstart
hadoop-processing`.

This will deploy an Apache Bigtop Hadoop cluster with 3 HBase units. More
information about this deployment can be found in the
[bundle readme](https://jujucharms.com/hadoop-hbase/).

This charm also supports the Thrift client API for HBase. Thrift is both
cross-platform and more lightweight than REST for many operations.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[juju-quickstart]: https://launchpad.net/juju-quickstart
[Configuring Models]: https://jujucharms.com/docs/stable/models-config


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

    juju run-action hbase/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do hbase/0 smoke-test`.

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`.

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`.

## HBase web UI
HBase provides a web console that can be used to verify information about
the cluster. To access it, find the `PUBLIC-ADDRESS` of any hbase unit and
expose the application:

    juju status hbase
    juju expose hbase

The web interface will be available at the following URL:

    http://HBASE_PUBLIC_IP:60010


# Using

Once the deployment has been verified, there are a number of actions available
in this charm.
> **Note**: Actions described below assume Juju 2.0 or greater. If using an
earlier version of Juju, the action syntax is:
`juju action do hbase/0 <action_name> <action_args>; juju action fetch <id>`.

Run a performance test:

    juju run-action hbase/0 perf-test
    juju show-action-output <id>  # <-- id from above command

Run a smoke test (as described in the above **Verifying** section):

    juju run-action hbase/0 smoke-test
    juju show-action-output <id>  # <-- id from above command

Start/Stop/Restart all HBase services on a unit:

    juju run-action hbase/0 [start|stop|restart]
    juju show-action-output <id>  # <-- id from above command


Start/Stop the HBase Master service on a unit:

    juju run-action hbase/0 [start|stop]-hbase-master
    juju show-action-output <id>  # <-- id from above command

Start/Stop the HBase RegionServer and Thrift services on a unit:

    juju run-action hbase/0 [start|stop]-hbase-regionserver
    juju show-action-output <id>  # <-- id from above command


# Limitations

Restarting an HBase deployment is potentially disruptive. Be aware that the
following events will cause a restart:

- Zookeeper units joining or departing the quorum.
- Upgrading the hbase charm.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache HBase home page](https://hbase.apache.org/)
- [Apache HBase issue tracker](https://issues.apache.org/jira/browse/HBASE)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
