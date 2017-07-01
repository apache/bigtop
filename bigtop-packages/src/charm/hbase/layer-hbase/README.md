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
millions of columns -- atop clusters of commodity hardware. Learn more at
[hbase.apache.org][].

This charm deploys version 1.1.9 of the HBase master, regionserver, and thrift
components from [Apache Bigtop][].

[hbase.apache.org]: http://hbase.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

An HBase deployment consists of HBase Masters and HBase RegionServers.
In a distributed HBase environment, one master and one regionserver are
deployed on each unit. HBase ensures that only one master is active with
the rest in standby mode in case the active master fails.

Because HBase requires HDFS, this charm is recommended to be deployed as part
of the `hadoop-hbase` bundle:

    juju deploy hadoop-hbase

This will deploy an Apache Bigtop Hadoop cluster with 3 HBase units colocated
on 3 Hadoop DataNodes. More information about this deployment can be found in
the [bundle readme](https://jujucharms.com/hadoop-hbase/).

This charm also supports the Thrift client API for HBase. Thrift is both
cross-platform and more lightweight than REST for many operations.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
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

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

## HBase web interfaces
The HBase Master service provides a web console that can be used to verify
information about the cluster. To access it, find the `PUBLIC-ADDRESS` of any
hbase unit and expose the application:

    juju status hbase
    juju expose hbase

The HBase Master web interface will be available at the following URL:

    http://HBASE_PUBLIC_IP:16010

The HBase RegionServer and Thrift services also provide web interfaces that
are accessible at the following URLs:

    http://HBASE_PUBLIC_IP:16030
    http://HBASE_PUBLIC_IP:9095


# Using

## Actions
Once HBase is ready, there are a number of actions available in this charm.

Run a performance test (as described in the **Benchmarking** section):

    juju run-action hbase/0 perf-test
    juju show-action-output <id>  # <-- id from above command

Run a smoke test (as described in the **Verifying** section):

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

## HBase Shell

HBase includes a shell that can be used to interact with the cluster. Access it
from any hbase unit (e.g. `hbase/0`) as follows:

    $ juju ssh hbase/0
    $ hbase shell
    HBase Shell; enter 'help<RETURN>' for list of supported commands.
    Type "exit<RETURN>" to leave the HBase Shell
    Version 1.1.3, rUnknown, Wed Mar 29 07:39:44 UTC 2017

    hbase(main):002:0> exit

# Configuring

Charm configuration can be changed at runtime with `juju config`. This charm
supports the following config parameters.

## Heap
The default heap size for the the HBase master JVM is 1024MB. Set a different
value (in MB) with the following:

    juju config hbase heap=4096


# Benchmarking

This charm provides a `perf-test` action to gauge the performance of the HBase
cluster:

    $ juju run-action hbase/0 perf-test
    Action queued with id: 339cec1f-e903-4ee7-85ca-876fb0c3d28e

    $ juju show-action-output 339cec1f-e903-4ee7-85ca-876fb0c3d28e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: "90"
        raw: /opt/hbase-perf-results/1495562300.log
        start: 2017-05-23T17:58:20Z
        stop: 2017-05-23T17:59:50Z
      outcome: success
    status: completed
    timing:
      completed: 2017-05-23 17:59:51 +0000 UTC
      enqueued: 2017-05-23 17:58:16 +0000 UTC
      started: 2017-05-23 17:58:20 +0000 UTC


# Limitations

Restarting an HBase cluster is potentially disruptive. Be aware that the
following events will cause a restart of all HBase services:

- Adding or removing HBase units
- Adding or removing Zookeeper units
- Changing charm configuration with `juju config`
- Upgrading this charm


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

- [Apache HBase home page](http://hbase.apache.org/)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jujucharms.com/big-data)
- [Juju Bigtop charms](https://jujucharms.com/q/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
