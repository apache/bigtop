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

The Apache Hadoop software library is a framework that allows for the
distributed processing of large data sets across clusters of computers
using a simple programming model.

Hadoop is designed to scale from a few servers to thousands of machines,
each offering local computation and storage. Rather than rely on hardware
to deliver high-availability, Hadoop can detect and handle failures at the
application layer. This provides a highly-available service on top of a cluster
of machines, each of which may be prone to failure.

Spark is a fast and general engine for large-scale data processing.

This bundle provides a complete deployment of Hadoop and Spark components from
[Apache Bigtop](http://bigtop.apache.org/)
that performs distributed data processing at scale. Ganglia and rsyslog
applications are also provided to monitor cluster health and syslog activity.

## Bundle Composition

The applications that comprise this bundle are spread across 9 units as
follows:

  * NameNode (HDFS)
  * ResourceManager (YARN)
    * Colocated on the NameNode unit
  * Slave (DataNode and NodeManager)
    * 3 separate units
  * Spark (Master and Worker)
  * Plugin (Facilitates communication with the Hadoop cluster)
    * Colocated on the Spark unit
  * Zeppelin (Web interface for Hadoop/Spark)
    * Colocated on the Spark unit
  * Zookeeper
    * 3 separate units
  * Ganglia (Web interface for monitoring cluster metrics)
  * Rsyslog (Aggregate cluster syslog events in a single location)
    * Colocated on the Ganglia unit

Deploying this bundle gives you a fully configured and connected Apache Bigtop
cluster on any supported cloud, which can be easily scaled to meet workload
demands.


# Deploying

A working Juju installation is assumed to be present. If you have not yet set
up Juju, please follow the [getting-started][] instructions
prior to deploying this bundle. Once ready, deploy this bundle with the
`juju deploy` command:

    juju deploy hadoop-spark

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart](https://launchpad.net/juju-quickstart) with the
following syntax: `juju quickstart hadoop-spark`._

You can also build all of the charms from their source layers in the
[Bigtop charm repository][].  See the [Bigtop charm README][] for instructions
on building and deploying these charms locally.

[getting-started]: https://jujucharms.com/docs/2.0/getting-started
[Bigtop charm repository]: https://github.com/apache/bigtop/tree/master/bigtop-packages/src/charm
[Bigtop charm README]: https://github.com/apache/bigtop/blob/master/bigtop-packages/src/charm/README.md


# Verifying

## Status
The applications that make up this bundle provide status messages to
indicate when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a smoke test
to verify that the bundle is working as expected.

## Smoke Test
The charms for each master component (namenode, resourcemanager, spark, and
zeppelin) provide a `smoke-test` action that can be used to verify the
application is functioning as expected. You can run them all with the following:

    juju run-action namenode/0 smoke-test
    juju run-action resourcemanager/0 smoke-test
    juju run-action spark/0 smoke-test
    juju run-action zeppelin/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do <application>/0 smoke-test`._

You can watch the progress of the smoke test actions with:

    watch -n 0.5 juju show-action-status

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`._

Eventually, all of the actions should settle to `status: completed`.  If
any report `status: failed`, that application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`._


# Cluster User Interface

This bundle includes Apache Zeppelin, a web-based notebook that enables
interactive data analytics. To interact with your newly deployed cluster,
expose zeppelin:

    juju expose zeppelin

Now find the zeppelin public IP address:

    juju status zeppelin --format yaml | grep public-address

You can access the web interface at:

    http://{zeppelin_public_ip}:9080


# Monitoring

This bundle includes Ganglia for system-level monitoring of the namenode,
resourcemanager, slave, spark, and zeppelin units. Metrics are sent to a
centralized ganglia unit for easy viewing in a browser. To view the ganglia web
interface, first expose the service:

    juju expose ganglia

Now find the ganglia public IP address:

    juju status ganglia --format yaml | grep public-address

The ganglia web interface will be available at:

    http://{ganglia_public_ip}/ganglia


# Logging

This bundle includes rsyslog to collect syslog data from the namenode,
resourcemanager, slave, spark, and zeppelin units. These logs are sent to a
centralized rsyslog unit for easy syslog analysis of the units that make up
the Hadoop cluster. One method of viewing this log data is to simply cat syslog
from the rsyslog unit:

    juju run --unit rsyslog/0 'sudo cat /var/log/syslog'

You can also forward logs to an external rsyslog processing service. See
the *Forwarding logs to a system outside of the Juju environment* section of
the [rsyslog README](https://jujucharms.com/rsyslog/) for more information.


# Benchmarking

The charms in this bundle provide several benchmarks to gauge the performance
of your environment.

The easiest way to run the benchmarks is to relate charms to the
[Benchmark GUI][].  You will likely also want to relate the same charms to
the [Benchmark Collector][] to have machine-level information collected during
the benchmark, for a more complete picture of how the machine performed.

[Benchmark GUI]: https://jujucharms.com/benchmark-gui/
[Benchmark Collector]: https://jujucharms.com/benchmark-collector/

Each benchmark is also an action that can be run with `juju run-action`,
for example:

    $ juju run-action resourcemanager/0 nnbench
    Action queued with id: 55887b40-116c-4020-8b35-1e28a54cc622

    $ juju show-action-output 55887b40-116c-4020-8b35-1e28a54cc622
        results:
          meta:
            composite:
              direction: asc
              units: secs
              value: "128"
            start: 2016-02-04T14:55:39Z
            stop: 2016-02-04T14:57:47Z
          results:
            raw: '{"BAD_ID": "0", "FILE: Number of read operations": "0", "Reduce input groups":
              "8", "Reduce input records": "95", "Map output bytes": "1823", "Map input records":
              "12", "Combine input records": "0", "HDFS: Number of bytes read": "18635", "FILE:
              Number of bytes written": "32999982", "HDFS: Number of write operations": "330",
              "Combine output records": "0", "Total committed heap usage (bytes)": "3144749056",
              "Bytes Written": "164", "WRONG_LENGTH": "0", "Failed Shuffles": "0", "FILE:
              Number of bytes read": "27879457", "WRONG_MAP": "0", "Spilled Records": "190",
              "Merged Map outputs": "72", "HDFS: Number of large read operations": "0", "Reduce
              shuffle bytes": "2445", "FILE: Number of large read operations": "0", "Map output
              materialized bytes": "2445", "IO_ERROR": "0", "CONNECTION": "0", "HDFS: Number
              of read operations": "567", "Map output records": "95", "Reduce output records":
              "8", "WRONG_REDUCE": "0", "HDFS: Number of bytes written": "27412", "GC time
              elapsed (ms)": "603", "Input split bytes": "1610", "Shuffled Maps ": "72", "FILE:
              Number of write operations": "0", "Bytes Read": "1490"}'
        status: completed
        timing:
          completed: 2016-02-04 14:57:48 +0000 UTC
          enqueued: 2016-02-04 14:55:14 +0000 UTC
          started: 2016-02-04 14:55:27 +0000 UTC


# Scaling

This bundle is designed to scale out. By default, three Hadoop slave units,
three zookeeper units, and one spark unit are deployed. To increase the amount
of Hadoop slaves or spark workers, simple add more units. To add one unit:

    juju add-unit slave
    juju add-unit spark

You can also add multiple units, for example, to add four more slaves:

    juju add-unit -n4 slave


# Network-Restricted Environments

Charms can be deployed in environments with limited network access. To deploy
in this environment, configure your Juju model with appropriate
proxy and/or mirror options. See
[Configuring Models](https://jujucharms.com/docs/2.0/models-config) for more
information.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
