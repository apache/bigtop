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

Apache ZooKeeper is a high-performance coordination service for distributed
applications. It exposes common services such as naming, configuration
management, synchronization, and group services in a simple interface. Use it
off-the-shelf to implement consensus, group management, leader election, and
presence protocols. Learn more at [zookeeper.apache.org][].

This charm provides version 3.4.6 of the ZooKeeper component from
[Apache Bigtop][].

[zookeeper.apache.org]: http://zookeeper.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

Deploy a Zookeeper unit. With only one unit, the application will be running in
`standalone` mode:

    juju deploy zookeeper

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[Configuring Models]: https://jujucharms.com/docs/stable/models-config

## Configuring Network Interfaces
In some network environments, zookeeper may need to be restricted to
listen for incoming connections on a specific network interface
(e.g.: for security reasons). To do so, configure zookeeper with either a
network interface name or a CIDR range specifying a subnet. For example:

    juju config zookeeper network_interface=eth0
    juju config zookeeper network_interface=10.0.2.0/24

Each zookeeper unit in the cluster will lookup the IP address of that
network interface, or find the first network interface with an IP
address in the specified subnet, and bind zookeeper to that address.

If a mistake is made and an invalid name for the network interface is
configured, recover by re-configuring with the correct name and then
run "juju resolved" on any failed units:

    juju config zookeeper network_interface=eth0
    juju resolved zookeeper/0

To go back to listening on all interfaces, configure zookeeper with
`network_interface=0.0.0.0`:

    juju config zookeeper network_interface=0.0.0.0


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

    juju run-action zookeeper/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

## Utilities
This charm includes ZooKeeper command line utilities that can also be used to
verify that the application is running as expected. Check the status of the
ZooKeeper daemon with `zkServer.sh`:

    juju run --application=zookeeper '/usr/lib/zookeeper/bin/zkServer.sh status'

A successful deployment will report the service mode as either `standalone`
(if only one zookeeper unit has been deployed) or `leader` / `follower` (if
a zookeeper quorum has been formed).


# Scaling

Running ZooKeeper in `standalone` mode is convenient for evaluation, some
development, and testing. In production, however, ZooKeeper should be run in
`replicated` mode. A replicated group of servers in the same application is
called a quorum, and in `replicated` mode, all servers in the quorum have
copies of the same configuration file.

In order to add new zookeeper servers to the quorum, simply add more units.
For example, add two more zookeeper units with:

    juju add-unit -n 2 zookeeper

The zookeeper nodes will automatically perform a rolling restart to update the
zookeeper quorum without losing any jobs in progress. Once the rolling restart
has completed, all of the zookeeper nodes should report the following status:

    ready (n units)

(Where 'n' is the total number of zookeeper units in the quorum.)


# Integrating

To integrate ZooKeeper into solutions with other charms, update the charms
that require ZooKeeper as follows:

1) Add following lines to `metadata.yaml`:

    requires:
      zookeeper:
         interface: zookeeper

2) Add a `zookeeper-relation-changed` hook. Example contents:

    from charmhelpers.core.hookenv import relation_get
    ZK_hostname = relation_get('private-address')
    ZK_port = relation_get('port')


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

- [Apache ZooKeeper home page](http://zookeeper.apache.org/)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jaas.ai/big-data)
- [Juju Bigtop charms](https://jaas.ai/search?q=bigtop)
