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
Apache ZooKeeper is a high-performance coordination service for distributed
applications. It exposes common services such as naming, configuration
management, synchronization, and group services in a simple interface so you
don't have to write them from scratch. You can use it off-the-shelf to
implement consensus, group management, leader election, and presence protocols.

## Usage
Deploy a Zookeeper unit. With only one unit, the service will be running in
`standalone` mode:

    juju deploy zookeeper zookeeper


## Scaling
Running ZooKeeper in `standalone` mode is convenient for evaluation, some
development, and testing. But in production, you should run ZooKeeper in
`replicated` mode. A replicated group of servers in the same application is
called a quorum, and in `replicated` mode, all servers in the quorum have
copies of the same configuration file.

In order to add new Zookeeper servers to the quorum, simply add units
as you usually would in juju:

    juju add-unit -n 2 zookeeper

The Zookeeper nodes will then automatically perform a rolling restart,
in order to update the Zookeeper quorum without losing any jobs in
progress. Once the rolling restart has completed, all of your
Zookeeper nodes should be in the following state:

    ready (n zk nodes)

(Where 'n' is the total number of Zookeeper nodes in your quorum.)


## Network Interfaces

In some network environments, you may want to restrict your Zookeepers
to listen for client connections on a specific network interface (for
example, for security reasons). To do so, you may pass either a
network interface name or a CIDR range specifying a subnet to the
``network_interface`` configuration variable. For example:

  juju set-config zookeeper network_interface=eth0

or

  juju set-config zookeeper network_interface=10.0.2.0/24

Each Zookeeper machine in your cluster will lookup the IP address of that
network interface, or find the first network interface with an IP
address in the specified subnet, and bind Zookeeper to that address.

If you make a mistake, and pass an invalid name for a network
interface, you may recover by passing the correct name to set-config,
and then running "juju resolved" on each unit:

  juju set-config zookeeper network_interface=eth0
  juju resolved -r zookeeper/0

If you want to go back to listening on any network interface on the
machine, simply pass ``0.0.0.0`` to ``network_interface``.

  juju set-config zookeeper network_interface=0.0.0.0


## Test the deployment
Test if the Zookeeper service is running by using the `zkServer.sh` script:

    juju run --service=zookeeper '/usr/lib/zookeeper/bin/zkServer.sh status'

A successful deployment will report the service mode as either `standalone`
(if only one Zookeeper unit has been deployed) or `leader` / `follower` (if
a Zookeeper quorum has been formed).


## Integrate Zookeeper into another charm
1) Add following lines to your charm's metadata.yaml:

    requires:
      zookeeper:
         interface: zookeeper

2) Add a `zookeeper-relation-changed` hook to your charm. Example contents:

    from charmhelpers.core.hookenv import relation_get
    ZK_hostname = relation_get('private-address')
    ZK_port = relation_get('port')



## Contact Information
[bigdata@lists.ubuntu.com](mailto:bigdata@lists.ubuntu.com)


## Help
- [Apache Zookeeper home page](https://zookeeper.apache.org/)
- [Apache Zookeeper issue tracker](https://issues.apache.org/jira/browse/ZOOKEEPER)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
