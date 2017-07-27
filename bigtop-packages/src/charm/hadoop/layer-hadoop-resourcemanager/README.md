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

This charm deploys version 2.7.3 of the ResourceManager component from
[Apache Bigtop][].

[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

This charm is intended to be deployed via one of the [apache bigtop bundles][].
For example:

    juju deploy hadoop-processing

This will deploy an Apache Bigtop cluster with this charm acting as the
ResourceManager. More information about this deployment can be found in the
[bundle readme](https://jujucharms.com/hadoop-processing/).

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
ready with nodemanagers.

## Smoke Test
This charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. This action executes the 'yarn'
smoke tests provided by Apache Bigtop and may take up to
10 minutes to complete. Run the action as follows:

    juju run-action resourcemanager/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

## Utilities
This charm includes Hadoop command line and web utilities that can be used
to verify information about the cluster.

Show the running nodes on the command line with the following:

    juju run --application resourcemanager "su yarn -c 'yarn node -list'"

To access the Resource Manager web consoles, find the `PUBLIC-ADDRESS` of the
resourcemanager application and expose it:

    juju status resourcemanager
    juju expose resourcemanager

The YARN and Job History web interfaces will be available at the following URLs:

    http://RESOURCEMANAGER_PUBLIC_IP:8088
    http://RESOURCEMANAGER_PUBLIC_IP:19888


# Benchmarking

This charm provides several benchmarks to gauge the performance of the
cluster. Each benchmark is an action that can be run with `juju run-action`:

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

- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jujucharms.com/big-data)
- [Juju Bigtop charms](https://jujucharms.com/q/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
