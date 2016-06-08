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

Apache Pig is a platform for creating MapReduce programs used with Hadoop.
It consists of a high-level language (Pig Latin) for expressing data analysis
programs, coupled with infrastructure for evaluating these programs. Learn more
at [pig.apache.org](http://pig.apache.org).

This charm deploys the Pig component of the Apache Bigtop platform and
supports running Pig in two execution modes:

 * Local Mode: Pig runs using your local host and file system. Specify local
   mode using the -x flag: `pig -x local`
 * Mapreduce Mode: Pig runs using a Hadoop cluster and HDFS. This is the default
   mode; you can, optionally, specify it using the -x flag:
   `pig` or `pig -x mapreduce`


# Deploying / Using

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the
[getting-started](https://jujucharms.com/docs/2.0/getting-started)
instructions prior to deploying this charm.

This charm is intended to be used with one of the
[apache bigtop bundles](https://jujucharms.com/u/bigdata-charmers/#bundles).
For example:

    juju deploy hadoop-processing

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart](https://launchpad.net/juju-quickstart) with the
following syntax: `juju quickstart hadoop-processing`.

This will deploy an Apache Bigtop Hadoop cluster. More information about this
deployment can be found in the [bundle readme](https://jujucharms.com/hadoop-processing/).

Now add Pig and relate it to the cluster via the hadoop-plugin:

    juju deploy pig
    juju add-relation pig plugin

Once deployment is complete, Apache Pig will be available to execute Pig Latin
jobs on your data. You can run Pig in a variety of modes:

## Local Mode
Run Pig in local mode on the Pig unit with the following:

    juju ssh pig/0
    pig -x local

## MapReduce Mode
MapReduce mode is the default for Pig. To run in this mode, ssh to the Pig unit
and run pig as follows:

    juju ssh pig/0
    pig


# Verifying

## Status
Apache Bigtop charms provide extended status reporting to indicate when they
are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message column will provide information about a given unit's state.
This charm is ready for use once the status message indicates that it is
ready.

## Smoke Test
This charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. Run the action as follows:

    juju run-action pig/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do pig/0 smoke-test`.

Watch the progress of the smoke test actions with:

    watch -n 0.5 juju show-action-status

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`.

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`.


# Network-Restricted Environments

Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate
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
