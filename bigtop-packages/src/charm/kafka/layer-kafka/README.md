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

Apache Kafka is an open-source message broker project developed by the Apache
Software Foundation written in Scala. The project aims to provide a unified,
high-throughput, low-latency platform for handling real-time data feeds. Learn
more at [kafka.apache.org][].

This charm deploys the Kafka component of the [Apache Bigtop][] platform.

[kafka.apache.org]: http://kafka.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the [getting-started][] instructions prior to deploying this
charm.

Kafka requires the Zookeeper distributed coordination service. Deploy and
relate them as follows:

    juju deploy kafka
    juju deploy zookeeper
    juju add-relation kafka zookeeper

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[Configuring Models]: https://jujucharms.com/docs/stable/models-config


# Using

Once deployed, there are a number of actions available in this charm.
> **Note**: Actions described below assume Juju 2.0 or greater. If using an
earlier version of Juju, the action syntax is:
`juju action do kafka/0 <action_name> <action_args>; juju action fetch <id>`.

List the zookeeper servers that our kafka brokers
are connected to. The following will list `<ip>:<port>` information for each
zookeeper unit in the environment (e.g.: `10.0.3.221:2181`).

    juju run-action kafka/0 list-zks
    juju show-action-output <id>  # <-- id from above command

Create a Kafka topic with:

    juju run-action kafka/0 create-topic topic=<topic_name> \
     partitions=<#> replication=<#>
    juju show-action-output <id>  # <-- id from above command

List topics with:

    juju run-action kafka/0 list-topics
    juju show-action-output <id>  # <-- id from above command

Write to a topic with:

    juju run-action kafka/0 write-topic topic=<topic_name> data=<data>
    juju show-action-output <id>  # <-- id from above command

Read from a topic with:

    juju run-action kafka/0 read-topic topic=<topic_name> partition=<#>
    juju show-action-output <id>  # <-- id from above command


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
application is functioning as expected. The test will verify connectivity
between Kafka and Zookeeper, and will test creation and listing of Kafka
topics. Run the action as follows:

    juju run-action slave/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do kafka/0 smoke-test`.

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


# Scaling

Expanding a cluster with many brokers is as easy as adding more Kafka units:

    juju add-unit kafka

After adding additional brokers, topics may be created with
replication up to the number of ready units. For example, if there are two
ready units, create a replicated topic as follows:

    juju run-action kafka/0 create-topic topic=my-replicated-topic \
        partitions=1 replication=2

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do kafka/0 create-topic <args>`.

Query the description of the recently created topic:

    juju run --unit kafka/0 'kafka-topics.sh --describe \
        --topic my-replicated-topic --zookeeper <zookeeperip>:2181'

An expected response should be similar to:

    Topic: my-replicated-topic PartitionCount:1 ReplicationFactor:2 Configs:
    Topic: my-replicated-topic Partition: 0 Leader: 2 Replicas: 2,0 Isr: 2,0


# Connecting External Clients

By default, this charm does not expose Kafka outside of the provider's network.
To allow external clients to connect to Kafka, first expose the service:

    juju expose kafka

Next, ensure the external client can resolve the short hostname of the kafka
unit. A simple way to do this is to add an `/etc/hosts` entry on the external
kafka client machine. Gather the needed info from juju:

    user@juju-client$ juju run --unit kafka/0 'hostname -s'
    kafka-0
    user@juju-client$ juju status --format=yaml kafka/0 | grep public-address
    public-address: 40.784.149.135

Update `/etc/hosts` on the external kafka client:

    user@kafka-client$ echo "40.784.149.135 kafka-0" | sudo tee -a /etc/hosts

The external kafka client should now be able to access Kafka by using
`kafka-0:9092` as the broker.


# Network Interfaces

In some network environments, kafka may need to be restricted to
listen for incoming connections on a specific network interface
(e.g.: for security reasons). To do so, configure kafka with either a
network interface name or a CIDR range specifying a subnet. For example:

    juju config kafka network_interface=eth0
    juju config kafka network_interface=10.0.2.0/24

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju set-config kafka network_interface=eth0`.

Each kafka machine in the cluster will lookup the IP address of that
network interface, or find the first network interface with an IP
address in the specified subnet, and bind kafka to that address.

If a mistake is made and an invalid name for the network interface is
configured, recover by re-configuring with the correct name and then
run "juju resolved" on each unit:

    juju config kafka network_interface=eth0
    juju resolved kafka/0

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju set-config kafka network_interface=eth0;
juju resolved -r kafka/0`.

To go back to listening on any network interface on the
machine, simply pass ``0.0.0.0`` to ``network_interface``.

    juju config kafka network_interface=0.0.0.0


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Apache Kafka home page](http://kafka.apache.org/)
- [Apache Kafka issue tracker](https://issues.apache.org/jira/browse/KAFKA)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
