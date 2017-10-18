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

Apache Zeppelin is a web-based notebook that enables interactive data analytics.
It allows for beautiful data-driven, interactive, and collaborative documents
with SQL, Scala and more. Learn more at [zeppelin.apache.org][].

This charm deploys version 0.7.2 of the Zeppelin component from
[Apache Bigtop][].

[zeppelin.apache.org]: http://zeppelin.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

Zeppelin can be deployed by itself as a stand-alone web notebook. Deployment
is simple:

    juju deploy zeppelin

To access the web interface, find the `Public address` of the `zeppelin`
application and expose it:

    juju status zeppelin
    juju expose zeppelin

The web interface will be available at the following URL:

    http://ZEPPELIN_PUBLIC_IP:9080

This charm also supports more complex integration scenarios as described below.

## Hadoop Integration
This charm may be deployed alongside any of the [Apache Bigtop bundles][].
For example:

    juju deploy hadoop-processing

This will deploy a basic Bigtop Hadoop cluster. More information about this
deployment can be found in the [bundle readme](https://jujucharms.com/hadoop-processing/).

Now relate the previously deployed `zeppelin` charm to the Hadoop plugin. This
enables communication between Zeppelin and Hadoop:

    juju add-relation zeppelin plugin

Once deployment is complete, Zeppelin notebooks will have access to the
Hadoop Distributed File System (HDFS). Additionally, the local Spark driver
will be reconfigured in YARN mode. Any notebooks that submit Spark jobs will
leverage the Hadoop compute resources deployed by the `hadoop-processing`
bundle.

## Spark Integration
Zeppelin includes a local Spark driver by default. This allows notebooks to
use a SparkContext without needing external Spark resources. This driver can
process jobs using local machine resources or compute resources from a Hadoop
cluster as mentioned above.

Zeppelin's Spark driver can also use external Spark cluster resources. For
example, the following will deploy a 3-unit Spark cluster that Zeppelin will
use when submitting jobs:

    juju deploy spark -n 3
    juju relate zeppelin spark

Once deployment is complete, the local Spark driver will be reconfigured to
use the external cluster as the Spark Master. Any notebooks that submit Spark
jobs will leverage the newly deployed `spark` units.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[apache bigtop bundles]: https://jujucharms.com/u/bigdata-charmers/#bundles
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

    juju run-action zeppelin/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>


# Limitations

When related to Spark, Zeppelin requires a `spark://xxx.xxx.xxx.xxx:7077`
URL for the Spark Master. This is only available when the `spark` charm is
in `standalone` mode -- `local` and `yarn` modes are not supported.


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

- [Apache Zeppelin home page](http://zeppelin.apache.org/)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jujucharms.com/big-data)
- [Juju Bigtop charms](https://jujucharms.com/q/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
