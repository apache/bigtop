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

This charm facilitates communication between Hadoop components of an
[Apache Bigtop][] cluster and workload applications.

[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the [getting-started][] instructions prior to deploying this
charm.

This charm is intended to be deployed via one of the [apache bigtop bundles][].
For example:

    juju deploy hadoop-processing

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart][] with the following syntax: `juju quickstart
hadoop-processing`.

This will deploy an Apache Bigtop cluster with a client unit preconfigured to
work with the cluster. More information about this deployment can be found in the
[bundle readme](https://jujucharms.com/hadoop-processing/).

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[apache bigtop bundles]: https://jujucharms.com/u/bigdata-charmers/#bundles
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
ready with hdfs and/or yarn.

## Smoke Test
This charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. Run the action as follows:

    juju run-action plugin/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do plugin/0 smoke-test`.

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

## Utilities
This charm includes Hadoop command line utilities that can be used
to verify information about the cluster.

Show the dfsadmin report on the command line with the following:

    juju run --application plugin "su hdfs -c 'hdfs dfsadmin -report'"


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
