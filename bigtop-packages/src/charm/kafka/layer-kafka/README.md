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

Apache Kafka is an open-source message broker project developed by the Apache
Software Foundation written in Scala. The project aims to provide a unified,
high-throughput, low-latency platform for handling real-time data feeds. Learn
more at [kafka.apache.org](http://kafka.apache.org/).

This charm deploys the Kafka component of the Apache Bigtop platform.

## Usage

Kafka requires the Zookeeper distributed coordination service. Deploy and
relate them as follows:

    juju deploy zookeeper
    juju deploy kafka
    juju add-relation kafka zookeeper

Once deployed, we can list the zookeeper servers that our kafka brokers
are connected to. The following will list `<ip>:<port>` information for each
zookeeper unit in the environment (e.g.: `10.0.3.221:2181`).

    juju run-action kafka/0 list-zks
    juju show-action-output <id>  # <-- id from above command

We can create a Kafka topic with:

    juju run-action kafka/0 create-topic topic=<topic_name> \
     partitions=<#> replication=<#>
    juju show-action-output <id>  # <-- id from above command

We can list topics with:

    juju run-action kafka/0 list-topics
    juju show-action-output <id>  # <-- id from above command

We can write to a topic with:

    juju run-action kafka/0 write-topic topic=<topic_name> data=<data>
    juju show-action-output <id>  # <-- id from above command

We can read from a topic with:

    juju run-action kafka/0 read-topic topic=<topic_name> partition=<#>
    juju show-action-output <id>  # <-- id from above command

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the action syntax is:_

    juju action do kafka/0 <action_name> <action_args>
    juju action fetch <id>  # <-- id from above command


## Status and Smoke Test

Kafka provides extended status reporting to indicate when it is ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that Kafka is working as expected using the built-in `smoke-test`
action:

    juju run-action kafka/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do kafka/0 smoke-test`._

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


## Scaling

Creating a cluster with many brokers is as easy as adding more Kafka units:

    juju add-unit kafka

After adding additional brokers, you will be able to create topics with
replication up to the number of kafka units.

To verify replication is working you can do the following:

    juju add-unit kafka -n 2
    juju run-action kafka/0 create-topic topic=my-replicated-topic \
        partitions=1 replication=2

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do kafka/0 create-topic <args>`._

Query for the description of the just created topic:

    juju run --unit kafka/0 'kafka-topics.sh --describe \
        --topic my-replicated-topic --zookeeper <zookeeperip>:2181'

You should get a response similar to:

    Topic: my-replicated-topic PartitionCount:1 ReplicationFactor:2 Configs:
    Topic: my-replicated-topic Partition: 0 Leader: 2 Replicas: 2,0 Isr: 2,0


## Connecting External Clients

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


## Network Interfaces

In some network environments, you may want to restrict kafka to
listening for incoming connections on a specific network interface
(for example, for security reasons). To do so, you may pass either a
network interface name or a CIDR range specifying a subnet to the
``network_interface`` configuration variable. For example:

  juju set-config kafka network_interface=eth0

or

  juju set-config kafka network_interface=10.0.2.0/24

Each kafka machine in your cluster will lookup the IP address of that
network interface, or find the first network interface with an IP
address in the specified subnet, and bind kafka to that address.

If you make a mistake, and pass in an invalid name for a network
interface, you may recover by passing the correct name to set-config,
and then running "juju resolved" on each unit:

  juju set-config kafka network_interface=eth0
  juju resolved kafka/0

If you want to go back to listening on any network interface on the
machine, simply pass ``0.0.0.0`` to ``network_interface``.

  juju set-config kafka network_interface=0.0.0.0


## Contact Information
- <bigdata@lists.ubuntu.com>


## Help
- [Apache Kafka home page](http://kafka.apache.org/)
- [Apache Kafka issue tracker](https://issues.apache.org/jira/browse/KAFKA)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
