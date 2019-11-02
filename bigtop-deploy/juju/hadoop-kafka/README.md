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

Apache Kafka is an open-source message broker project developed by the Apache
Software Foundation written in Scala. The project aims to provide a unified,
high-throughput, low-latency platform for handling real-time data feeds. Learn
more at [kafka.apache.org][].

This bundle provides a complete deployment of Hadoop and Kafka components from
[Apache Bigtop][] that perform distributed data processing at scale. Ganglia
and rsyslog applications are also provided to monitor cluster health and syslog
activity.

[kafka.apache.org]: http://kafka.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/

## Bundle Composition

The applications that comprise this bundle are spread across 9 units as
follows:

  * NameNode v2.8.5
  * ResourceManager v2.8.5
    * Colocated on the NameNode unit
  * Slave (DataNode and NodeManager) v2.8.5
    * 3 separate units
  * Kafka v0.10.2.2
  * Flume-Kafka
    * Colocated on the Kafka unit
  * Zookeeper v3.4.6
    * 3 separate units
  * Client (Hadoop endpoint)
  * Plugin (Facilitates communication with the Hadoop cluster)
    * Colocated on the Client unit
  * Flume-HDFS
    * Colocated on the Client unit
  * Ganglia (Web interface for monitoring cluster metrics)
    * Colocated on the Client unit
  * Rsyslog (Aggregate cluster syslog events in a single location)
    * Colocated on the Client unit

The Flume-HDFS unit provides an Apache Flume agent featuring an Avro source,
memory channel, and HDFS sink. This agent supports a relation with the
Flume-Kafka charm (apache-flume-kafka) to ingest messages published to a given
Kafka topic into HDFS.

Deploying this bundle results in a fully configured Apache Bigtop
cluster on any supported cloud, which can be scaled to meet workload
demands.


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this bundle.

> **Note**: This bundle requires hardware resources that may exceed limits
of Free-tier or Trial accounts on some clouds. To deploy to these
environments, modify a local copy of [bundle.yaml][] to set
`services: 'X': num_units: 1` and `machines: 'X': constraints: mem=3G` as
needed to satisfy account limits.

Deploy this bundle from the Juju charm store with the `juju deploy` command:

    juju deploy hadoop-kafka

Alternatively, deploy a locally modified `bundle.yaml` with:

    juju deploy /path/to/bundle.yaml

The charms in this bundle can also be built from their source layers in the
[Bigtop charm repository][].  See the [Bigtop charm README][] for instructions
on building and deploying these charms locally.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[bundle.yaml]: https://github.com/apache/bigtop/blob/master/bigtop-deploy/juju/hadoop-kafka/bundle.yaml
[Bigtop charm repository]: https://github.com/apache/bigtop/tree/master/bigtop-packages/src/charm
[Bigtop charm README]: https://github.com/apache/bigtop/blob/master/bigtop-packages/src/charm/README.md
[Configuring Models]: https://jujucharms.com/docs/stable/models-config


# Configuring

The default Kafka topic where messages are published is unset. Set this to
an existing Kafka topic as follows:

    juju config flume-kafka kafka_topic='<topic_name>'

If no existing topic is available, create and verify a new topic with:

    juju run-action kafka/0 create-topic topic=<topic_name> \
     partitions=1 replication=1
    juju show-action-output <id>  # <-- id from above command

Once the Flume agents start, messages will start flowing into
HDFS in year-month-day directories here: `/user/flume/flume-kafka/%y-%m-%d`.


# Verifying

## Status
The applications that make up this bundle provide status messages to indicate
when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 2 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, perform application smoke tests
to verify that the bundle is working as expected.

## Smoke Test
The charms for each core component (namenode, resourcemanager, slave, kafka,
and zookeeper) provide a `smoke-test` action that can be used to verify the
application is functioning as expected. Note that the 'slave' component runs
extensive tests provided by Apache Bigtop and may take up to 30 minutes to
complete. Run the smoke-test actions as follows:

    juju run-action namenode/0 smoke-test
    juju run-action resourcemanager/0 smoke-test
    juju run-action slave/0 smoke-test
    juju run-action kafka/0 smoke-test
    juju run-action zookeeper/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, all of the actions should settle to `status: completed`.  If
any report `status: failed`, that application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

## Utilities
Applications in this bundle include command line and web utilities that
can be used to verify information about the cluster.

From the command line, show the HDFS dfsadmin report and view the current list
of YARN NodeManager units with the following:

    juju run --application namenode "su hdfs -c 'hdfs dfsadmin -report'"
    juju run --application resourcemanager "su yarn -c 'yarn node -list'"

Show the list of Zookeeper nodes with the following:

    juju run --unit zookeeper/0 'echo "ls /" | /usr/lib/zookeeper/bin/zkCli.sh'

To access the HDFS web console, find the `Public address` of the namenode
application and expose it:

    juju status namenode
    juju expose namenode

The web interface will be available at the following URL:

    http://NAMENODE_PUBLIC_IP:50070

Similarly, to access the Resource Manager web consoles, find the
`Public address` of the resourcemanager application and expose it:

    juju status resourcemanager
    juju expose resourcemanager

The YARN and Job History web interfaces will be available at the following URLs:

    http://RESOURCEMANAGER_PUBLIC_IP:8088
    http://RESOURCEMANAGER_PUBLIC_IP:19888


# Monitoring

This bundle includes Ganglia for system-level monitoring of the namenode,
resourcemanager, slave, kafka, and zookeeper units. Metrics are sent to a
centralized ganglia unit for easy viewing in a browser. To view the ganglia web
interface, find the `Public address` of the Ganglia application and expose it:

    juju status ganglia
    juju expose ganglia

The web interface will be available at:

    http://GANGLIA_PUBLIC_IP/ganglia


# Logging

This bundle includes rsyslog to collect syslog data from the namenode,
resourcemanager, slave, kafka, and zookeeper units. These logs are sent to a
centralized rsyslog unit for easy syslog analysis. One method of viewing this
log data is to simply cat syslog from the rsyslog unit:

    juju run --unit rsyslog/0 'sudo cat /var/log/syslog'

Logs may also be forwarded to an external rsyslog processing service. See
the *Forwarding logs to a system outside of the Juju environment* section of
the [rsyslog README](https://jujucharms.com/rsyslog/) for more information.


# Benchmarking

The `resourcemanager` charm in this bundle provide several benchmarks to gauge
the performance of the Hadoop cluster. Each benchmark is an action that can be
run with `juju run-action`:

    $ juju actions resourcemanager
    ACTION      DESCRIPTION
    mrbench     Mapreduce benchmark for small jobs
    nnbench     Load test the NameNode hardware and configuration
    smoke-test  Run an Apache Bigtop smoke test.
    teragen     Generate data with teragen
    terasort    Runs teragen to generate sample data, and then runs terasort to sort that data
    testdfsio   DFS IO Testing

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

By default, three Hadoop slave, one Kafka, and three zookeeper units are
deployed with this bundle. Scaling these applications is as simple as adding
more units. To add one unit:

    juju add-unit kafka
    juju add-unit slave
    juju add-unit zookeeper

Multiple units may be added at once.  For example, add four more slave units:

    juju add-unit -n4 slave


# Issues

Apache Bigtop tracks issues using JIRA (Apache account required). File an
issue for this bundle at:

https://issues.apache.org/jira/secure/CreateIssue!default.jspa

Ensure `Bigtop` is selected as the project. Typically, bundle issues are filed
in the `deployment` component with the latest stable release selected as the
affected version. Any uncertain fields may be left blank.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jaas.ai/big-data)
- [Juju Bigtop charms](https://jaas.ai/search?q=bigtop)
